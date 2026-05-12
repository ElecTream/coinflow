package com.leeam.cryptowidget.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    init {
        scope.launch {
            _entries.value = widgetPrefs.debugLog.first()
        }
    }

    fun log(source: String, message: String, throwable: Throwable? = null, level: DebugLevel = DebugLevel.ERROR) {
        scope.launch {
            val text = if (throwable != null) {
                "$message :: ${throwable.javaClass.simpleName}: ${throwable.message ?: "(no message)"}"
            } else message
            val entry = DebugEntry(
                timeMs  = System.currentTimeMillis(),
                source  = source,
                level   = level,
                message = text
            )
            _entries.update { (it + entry).takeLast(MAX_ENTRIES) }
            widgetPrefs.persistDebugLog(_entries.value)
        }
    }

    fun info(source: String, message: String) = log(source, message, level = DebugLevel.INFO)
    fun warn(source: String, message: String, t: Throwable? = null) = log(source, message, t, DebugLevel.WARN)
    fun error(source: String, message: String, t: Throwable? = null) = log(source, message, t, DebugLevel.ERROR)

    fun clear() {
        scope.launch {
            _entries.value = emptyList()
            widgetPrefs.persistDebugLog(emptyList())
        }
    }
}

enum class DebugLevel { INFO, WARN, ERROR }

data class DebugEntry(
    val timeMs: Long,
    val source: String,
    val level: DebugLevel,
    val message: String
)
