#!/usr/bin/env pwsh
# Build Coinflow release APK and push to phone via Ship.

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

Write-Host "Running gradlew assembleRelease" -ForegroundColor Cyan
.\gradlew.bat assembleRelease
if ($LASTEXITCODE -ne 0) { throw "gradle assembleRelease failed (exit $LASTEXITCODE)" }

$apkDir = Join-Path $PSScriptRoot 'app\build\outputs\apk\release'
$targetApk = Get-ChildItem -Path $apkDir -Filter 'Coinflow-*.apk' -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1 -ExpandProperty FullName

# --- Push to phone ---
if ($targetApk -and (Test-Path $targetApk)) {
    Write-Host "Pushing $(Split-Path $targetApk -Leaf) to phone..." -ForegroundColor Cyan
    & push-apk $targetApk
    if ($LASTEXITCODE -ne 0) { Write-Warning "push-apk exit $LASTEXITCODE (build ok)" }
} else {
    Write-Warning "No Coinflow-*.apk in $apkDir; skipping push."
}
