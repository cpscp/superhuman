package com.carlos.superhuman.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Armazenamento local único: todo o estado da app num ficheiro JSON (privado da app).
 * Simples, rápido e fácil de exportar/importar como backup.
 */
object Store {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()
    val data: AppData get() = _state.value

    private var file: File? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private val lock = Any()

    /** Chamado sempre que os dados mudam (usado para atualizar widgets). */
    var onChanged: ((Context) -> Unit)? = null

    fun init(context: Context) {
        if (file != null) return
        synchronized(lock) {
            if (file != null) return
            appContext = context.applicationContext
            val f = File(context.applicationContext.filesDir, "superhuman.json")
            file = f
            if (f.exists()) {
                try {
                    _state.value = json.decodeFromString(AppData.serializer(), f.readText())
                } catch (e: Exception) {
                    Log.e("Store", "Falha a ler dados, a criar backup", e)
                    f.copyTo(File(f.parentFile, "superhuman.corrupt.json"), overwrite = true)
                }
            }
        }
    }

    fun update(block: (AppData) -> AppData) {
        synchronized(lock) {
            _state.value = block(_state.value)
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(350)
            saveNow()
            appContext?.let { ctx -> onChanged?.invoke(ctx) }
        }
    }

    fun saveNow() {
        val f = file ?: return
        try {
            val text = json.encodeToString(AppData.serializer(), _state.value)
            val tmp = File(f.parentFile, "superhuman.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(f)) {
                f.writeText(text)
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e("Store", "Falha a gravar", e)
        }
    }

    fun exportJson(): String = json.encodeToString(AppData.serializer(), _state.value)

    fun importJson(text: String): Boolean = try {
        val d = json.decodeFromString(AppData.serializer(), text)
        update { d }
        true
    } catch (e: Exception) {
        false
    }
}
