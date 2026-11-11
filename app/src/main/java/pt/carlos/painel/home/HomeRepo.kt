package pt.carlos.painel.home

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.home.DeviceType
import com.google.home.FactoryRegistry
import com.google.home.ForcePermissionFlow
import com.google.home.Home
import com.google.home.HomeClient
import com.google.home.HomeConfig
import com.google.home.HomeDevice
import com.google.home.PermissionsResultStatus
import com.google.home.PermissionsState
import com.google.home.matter.standard.ColorControl
import com.google.home.matter.standard.ColorTemperatureLightDevice
import com.google.home.matter.standard.DimmableLightDevice
import com.google.home.matter.standard.DimmablePlugInUnitDevice
import com.google.home.matter.standard.ExtendedColorLightDevice
import com.google.home.matter.standard.LevelControl
import com.google.home.matter.standard.LevelControlTrait
import com.google.home.matter.standard.OnOff
import com.google.home.matter.standard.OnOffLightDevice
import com.google.home.matter.standard.OnOffPluginUnitDevice
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Um dispositivo da casa, já pronto a mostrar. */
data class HomeDev(
    val id: String,
    val name: String,
    val room: String,
    val isLight: Boolean,
    val on: Boolean?,
    val levelPct: Int?,   // brilho 0–100, se a luz o suportar
)

/**
 * Casa pelas Google Home APIs: os dispositivos que tens no Google Home (Tapo, Govee...),
 * ligar/desligar e brilho, com o estado em tempo real.
 */
object HomeRepo {
    /** Nenhum erro do Google Home pode fechar o Painel: fica registado e aparece no mosaico. */
    private val handler = CoroutineExceptionHandler { _, e -> fail("Erro interno", e) }
    // Uma só thread própria (nem a do ecrã, nem concorrência entre pedidos).
    private val scope = CoroutineScope(SupervisorJob() + java.util.concurrent.Executors.newSingleThreadExecutor().asCoroutineDispatcher() + handler)
    private var started = false
    private var appCtx: Context? = null

    private fun fail(where: String, e: Throwable) {
        error = "$where: ${e.javaClass.simpleName}${e.message?.let { " – $it" } ?: ""}"
        if (permitted == null) permitted = false
        appCtx?.let { c ->
            runCatching {
                java.io.File(c.filesDir, "ultimo-erro.txt").writeText(
                    java.time.LocalDateTime.now().toString() + " (Google Home)\n" + android.util.Log.getStackTraceString(e).take(6000)
                )
            }
        }
    }
    private var client: HomeClient? = null
    private var devicesJob: Job? = null
    private val deviceJobs = HashMap<String, Job>()
    private val onOffs = HashMap<String, OnOff>()
    private val levels = HashMap<String, LevelControl>()

    /** null = a verificar; true/false = autorizado ou não. */
    var permitted by mutableStateOf<Boolean?>(null)
        private set
    var devices by mutableStateOf<List<HomeDev>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** Dispositivos da casa que não são luzes nem tomadas (para diagnóstico). */
    var others by mutableStateOf<List<String>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set

    private val registry = FactoryRegistry(
        traits = listOf(OnOff, LevelControl, ColorControl),
        types = listOf(
            OnOffLightDevice, DimmableLightDevice, ColorTemperatureLightDevice, ExtendedColorLightDevice,
            OnOffPluginUnitDevice, DimmablePlugInUnitDevice,
        ),
    )

    val ready: Boolean get() = client != null

    /** Arranca o Google Home em segundo plano (nunca na thread do ecrã, que bloquearia o Display). */
    fun init(ctx: Context) {
        if (started) return
        started = true
        appCtx = ctx.applicationContext
        scope.launch {
            runCatching {
                Home.getClient(ctx.applicationContext, HomeConfig(coroutineContext = Dispatchers.IO, factoryRegistry = registry))
            }.onSuccess { client = it; watchPermission(it) }
                .onFailure { fail("Não foi possível iniciar o Google Home", it) }
        }
        // Se nada responder em 10 s, mostra o botão em vez de ficar à espera.
        scope.launch {
            delay(10_000)
            if (permitted == null) {
                permitted = false
                if (error == null) error = "Sem resposta do Google Home. Toca em Ligar para tentar outra vez."
            }
        }
    }

    /** Liga o ecrã de autorização da Google a uma atividade (chamar no onCreate, só com o cliente pronto). */
    fun registerIfReady(caller: ActivityResultCaller): Boolean {
        val c = client ?: return false
        return runCatching { c.registerActivityResultCallerForPermissions(caller); true }
            .onFailure { fail("Registo da autorização", it) }
            .getOrDefault(false)
    }

    /** Abre o ecrã da Google para escolheres a casa e as luzes que o Painel pode controlar. */
    suspend fun requestPermission() {
        val c = client ?: return
        error = null
        runCatching {
            val r = c.requestPermissions(ForcePermissionFlow.FORCE_LAUNCH)
            when (r.status) {
                PermissionsResultStatus.SUCCESS -> { permitted = true; startDevices(c) }
                PermissionsResultStatus.CANCELLED -> error = "Autorização cancelada."
                else -> error = r.errorMessage ?: "Não foi possível autorizar."
            }
        }.onFailure { fail("Pedido de autorização", it) }
    }

    private fun watchPermission(c: HomeClient) {
        scope.launch {
            runCatching {
                c.hasPermissions().collect { st ->
                    when (st) {
                        PermissionsState.GRANTED -> { permitted = true; startDevices(c) }
                        PermissionsState.NOT_GRANTED -> permitted = false
                        PermissionsState.PERMISSIONS_STATE_UNAVAILABLE -> {
                            permitted = false
                            error = "Google Home indisponível neste tablet. Atualiza os Serviços do Google Play na Play Store."
                        }
                        else -> Unit
                    }
                }
            }.onFailure { fail("Estado da autorização", it) }
        }
    }

    private fun startDevices(c: HomeClient) {
        if (devicesJob != null) return
        devicesJob = scope.launch {
            runCatching {
                c.devices().collect { set -> sync(set) }
            }.onFailure { error = it.message; devicesJob = null }
        }
    }

    private fun typeFlow(d: HomeDevice): Flow<DeviceType>? = when {
        d.has(ExtendedColorLightDevice) -> d.type(ExtendedColorLightDevice)
        d.has(ColorTemperatureLightDevice) -> d.type(ColorTemperatureLightDevice)
        d.has(DimmableLightDevice) -> d.type(DimmableLightDevice)
        d.has(OnOffLightDevice) -> d.type(OnOffLightDevice)
        d.has(DimmablePlugInUnitDevice) -> d.type(DimmablePlugInUnitDevice)
        d.has(OnOffPluginUnitDevice) -> d.type(OnOffPluginUnitDevice)
        else -> null
    }

    private fun sync(set: Set<HomeDevice>) {
        loaded = true
        others = set.filter { typeFlow(it) == null }.map { it.name }
        val ids = set.map { it.id.id }.toSet()
        (deviceJobs.keys - ids).forEach { gone -> deviceJobs.remove(gone)?.cancel(); onOffs.remove(gone); levels.remove(gone) }
        devices = devices.filter { it.id in ids }
        for (d in set) {
            val id = d.id.id
            if (deviceJobs.containsKey(id)) continue
            val flow = typeFlow(d) ?: continue
            deviceJobs[id] = scope.launch {
                val room = runCatching { withContext(Dispatchers.IO) { d.room()?.name } }.getOrNull().orEmpty()
                runCatching {
                    flow.collect { t ->
                        val (onOff, level, light) = when (t) {
                            is ExtendedColorLightDevice -> Triple(t.standardTraits.onOff, t.standardTraits.levelControl, true)
                            is ColorTemperatureLightDevice -> Triple(t.standardTraits.onOff, t.standardTraits.levelControl, true)
                            is DimmableLightDevice -> Triple(t.standardTraits.onOff, t.standardTraits.levelControl, true)
                            is OnOffLightDevice -> Triple(t.standardTraits.onOff, null, true)
                            is DimmablePlugInUnitDevice -> Triple(t.standardTraits.onOff, t.standardTraits.levelControl, false)
                            is OnOffPluginUnitDevice -> Triple(t.standardTraits.onOff, null, false)
                            else -> Triple(null, null, false)
                        }
                        if (onOff != null) onOffs[id] = onOff
                        if (level != null) levels[id] = level
                        val lvl = level?.currentLevel?.toInt()
                        val dev = HomeDev(
                            id = id,
                            name = d.name,
                            room = room,
                            isLight = light,
                            on = onOff?.onOff,
                            levelPct = lvl?.let { (it * 100 / 254).coerceIn(0, 100) },
                        )
                        devices = (devices.filter { it.id != id } + dev).sortedWith(compareBy({ it.room }, { it.name }))
                    }
                }.onFailure { error = it.message }
            }
        }
    }

    fun toggle(dev: HomeDev) {
        val t = onOffs[dev.id] ?: return
        // Mostra logo a mudança; o estado real chega a seguir pelo fluxo.
        devices = devices.map { if (it.id == dev.id) it.copy(on = !(it.on ?: false)) else it }
        scope.launch { runCatching { withContext(Dispatchers.IO) { if (dev.on == true) t.off() else t.on() } }.onFailure { error = it.message } }
    }

    fun setLevel(dev: HomeDev, pct: Int) {
        val l = levels[dev.id] ?: return
        val raw = (pct.coerceIn(1, 100) * 254 / 100).coerceIn(1, 254)
        devices = devices.map { if (it.id == dev.id) it.copy(levelPct = pct, on = true) else it }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    l.moveToLevelWithOnOff(
                        raw.toUByte(),
                        (4).toUShort(),
                        LevelControlTrait.OptionsBitmap(false, false),
                        LevelControlTrait.OptionsBitmap(false, false),
                    )
                }
            }.onFailure { error = it.message }
        }
    }
}
