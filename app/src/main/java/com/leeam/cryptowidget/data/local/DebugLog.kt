package com.leeam.cryptowidget.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One in-memory ring buffer + DataStore-persisted error/info log.
 *
 * Components log via [log]; entries survive process death because each append
 * persists the whole buffer to [WidgetPreferences.persistDebugLog]. The buffer
 * is bounded at [MAX_ENTRIES] entries to keep DataStore writes cheap.
 *
 * Designed for the in-app debug page — not user-facing.
 */
@Singleton
class DebugLog @Inject constructor(
    private val widgetPrefs: WidgetPreferences
) {
    companion object {
        const val MAX_ENTRIES = 200
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _entries = MutableStateFlow<List<DebugEntry>>(emptyList())
    val entries: StateFlow<List<DebugEntry>> = _entries.asStateFlow()

    // Sealed command stream so init-load and appends are processed in strict order.
    // Without this, init's load can race with a concurrent log() and silently wipe
    // an entry, or a clear() can land between two appends and lose data.
    private sealed interface Command {
        data class Append(val entry: DebugEntry) : Command
        data object Clear : Command
    }

    private val inbox = Channel<Command>(capacity = Channel.UNLIMITED)

    init {
        scope.launch {
            // Phase 1: load persisted entries before processing any new commands.
            _entries.value = widgetPrefs.debugLog.first()
            // Phase 2: serially drain new commands. Single consumer ⇒ no races.
            for (cmd in inbox) {
                when (cmd) {
                    is Command.Append -> {
                        _entries.update { (it + cmd.entry).takeLast(MAX_ENTRIES) }
                        runCatching { widgetPrefs.persistDebugLog(_entries.value) }
                    }
                    is Command.Clear -> {
                        _entries.value = emptyList()
                        runCatching { widgetPrefs.persistDebugLog(emptyList()) }
                    }
                }
            }
        }
    }

    fun log(source: String, message: String, throwable: Throwable? = null, level: DebugLevel = DebugLevel.ERROR) {
        val text = if (throwable != null) {
            "$message :: ${throwable.javaClass.simpleName}: ${throwable.message ?: "(no message)"}"
        } else message
        val entry = DebugEntry(
            timeMs  = System.currentTimeMillis(),
            source  = source,
            level   = level,
            message = text
        )
        // trySend is fine because the Channel is UNLIMITED — won't drop unless closed.
        inbox.trySend(Command.Append(entry))
    }

    fun info(source: String, message: String) = log(source, message, level = DebugLevel.INFO)
    fun warn(source: String, message: String, t: Throwable? = null) = log(source, message, t, DebugLevel.WARN)
    fun error(source: String, message: String, t: Throwable? = null) = log(source, message, t, DebugLevel.ERROR)

    fun clear() {
        inbox.trySend(Command.Clear)
    }
}

enum class DebugLevel { INFO, WARN, ERROR }

data class DebugEntry(
    val timeMs: Long,
    val source: String,
    val level: DebugLevel,
    val message: String
)
