#!/usr/bin/env pwsh
# Coinflow release builder.
# Delegates version bump to `./gradlew bumpPatch` (writes app/version.properties),
# runs split-per-ABI release build (gradle renames outputs to
# Coinflow-<ver>-<abi>.apk), copies arm64-v8a to releases/Coinflow-<ver>.apk,
# other ABIs to releases/abis/, archives prior top-level Coinflow-*.apk,
# pushes via push-apk.
#
# Release signing: reads RELEASE_STORE_FILE + RELEASE_STORE_PASSWORD +
# RELEASE_KEY_ALIAS + RELEASE_KEY_PASSWORD from app/gradle.properties.
# If unset, gradle falls back to the debug keystore so builds still work.
#
# Usage:
#   .\build.ps1              # bump patch, build release, archive, push
#   .\build.ps1 -NoBump      # rebuild current version (overwrites)
#   .\build.ps1 -Bump minor  # bump minor (resets patch)
#   .\build.ps1 -Bump major  # bump major (resets minor + patch)
#   .\build.ps1 -NoPush      # build + copy, skip the phone push

[CmdletBinding()]
param(
    [ValidateSet('patch', 'minor', 'major')]
    [string]$Bump = 'patch',
    [switch]$NoBump,
    [switch]$NoPush
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$versionFile = Join-Path $PSScriptRoot 'app\version.properties'
if (-not (Test-Path $versionFile)) { throw "app/version.properties not found at $versionFile" }

function Get-VersionName {
    $line = (Get-Content $versionFile) | Where-Object { $_ -match '^VERSION_NAME=' } | Select-Object -First 1
    if (-not $line) { throw 'VERSION_NAME not found in app/version.properties' }
    ($line -replace '^VERSION_NAME=', '').Trim()
}

$current = Get-VersionName

if (-not $NoBump) {
    $task = switch ($Bump) {
        'patch' { 'bumpPatch' }
        'minor' { 'bumpMinor' }
        'major' { 'bumpMajor' }
    }
    Write-Host "Running gradlew $task" -ForegroundColor Cyan
    & .\gradlew.bat $task
    if ($LASTEXITCODE -ne 0) { throw "gradle $task failed (exit $LASTEXITCODE)" }
}

$next = Get-VersionName
Write-Host "Version: $current -> $next" -ForegroundColor Cyan

$releases = Join-Path $PSScriptRoot 'releases'
$archive  = Join-Path $releases 'archive'
$abis     = Join-Path $releases 'abis'
New-Item -ItemType Directory -Force -Path $releases, $archive, $abis | Out-Null

Get-ChildItem -Path $releases -Filter 'Coinflow-*.apk' -File -ErrorAction SilentlyContinue |
    ForEach-Object {
        $dest = Join-Path $archive $_.Name
        if (Test-Path $dest) { Remove-Item $dest -Force }
        Move-Item $_.FullName $dest
        Write-Host "  archived $($_.Name)" -ForegroundColor DarkGray
    }

Write-Host "Running gradlew :app:assembleRelease" -ForegroundColor Cyan
& .\gradlew.bat :app:assembleRelease
if ($LASTEXITCODE -ne 0) { throw "gradle assembleRelease failed (exit $LASTEXITCODE)" }

$outDir = Join-Path $PSScriptRoot 'app\build\outputs\apk\release'
$map = @{
    "Coinflow-$next-arm64-v8a.apk"   = Join-Path $releases "Coinflow-$next.apk"
    "Coinflow-$next-armeabi-v7a.apk" = Join-Path $abis     "Coinflow-$next-armeabi-v7a.apk"
    "Coinflow-$next-x86_64.apk"      = Join-Path $abis     "Coinflow-$next-x86_64.apk"
}

$copied = 0
foreach ($src in $map.Keys) {
    $srcPath = Join-Path $outDir $src
    if (-not (Test-Path $srcPath)) { continue }
    Copy-Item $srcPath $map[$src] -Force
    $size = [math]::Round((Get-Item $map[$src]).Length / 1MB, 2)
    Write-Host "  $(Split-Path $map[$src] -Leaf)  ($size MB)" -ForegroundColor Green
    $copied++
}
if ($copied -eq 0) { throw "No Coinflow-$next-*.apk found in $outDir" }

$targetApk = Join-Path $releases "Coinflow-$next.apk"
if (-not (Test-Path $targetApk)) {
    throw "arm64-v8a APK missing at $targetApk -- check gradle output."
}

Write-Host ""
Write-Host "Done. Coinflow-$next.apk in $releases" -ForegroundColor Green

if ($NoPush) {
    Write-Host "Skipping push (-NoPush)." -ForegroundColor DarkGray
    return
}
Write-Host "Pushing $(Split-Path $targetApk -Leaf) to phone..." -ForegroundColor Cyan
$pushApk = (Get-Command push-apk -ErrorAction SilentlyContinue)?.Source
if (-not $pushApk) {
    $fallback = 'C:\Users\leeam\Documents\Github\ship\cli\push-apk.exe'
    if (Test-Path $fallback) { $pushApk = $fallback }
}
if ($pushApk) {
    & $pushApk $targetApk
    if ($LASTEXITCODE -ne 0) { Write-Warning "push-apk exit $LASTEXITCODE (build ok)" }
} else {
    Write-Warning "push-apk not found on PATH or at ship\cli; skipping push."
}
