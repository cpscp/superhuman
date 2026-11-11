package pt.carlos.painel.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat

/** Só serve para o Android deixar o Painel ver o que está a tocar (Acesso a notificações). */
class PainelNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() { Media.start(this) }
}

data class MediaInfo(
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val positionAt: Long,
    val speed: Float,
    val packageName: String,
) {
    fun currentPosition(): Long =
        if (!playing) positionMs else (positionMs + ((SystemClock.elapsedRealtime() - positionAt) * speed).toLong()).coerceAtMost(durationMs.coerceAtLeast(0))
}

/** Música a tocar (BitChord, YouTube, Nuvio...) através das sessões de media do Android. */
object Media {
    var info by mutableStateOf<MediaInfo?>(null)
        private set
    private var controller: MediaController? = null
    private var started = false

    fun hasAccess(ctx: Context) = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() { controller = null; info = null }
    }

    fun start(ctx: Context) {
        if (!hasAccess(ctx)) return
        val app = ctx.applicationContext
        val msm = app.getSystemService(MediaSessionManager::class.java)
        val comp = ComponentName(app, PainelNotificationListener::class.java)
        runCatching {
            if (!started) {
                msm.addOnActiveSessionsChangedListener({ list -> pick(list ?: emptyList()) }, comp)
                started = true
            }
            pick(msm.getActiveSessions(comp))
        }
    }

    private fun pick(list: List<MediaController>) {
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback)
        }
        refresh()
    }

    private fun refresh() {
        val c = controller ?: run { info = null; return }
        val m = c.metadata
        val st = c.playbackState
        if (m == null) { info = null; return }
        val bmp: Bitmap? = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        info = MediaInfo(
            title = m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: "",
            artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: "",
            art = bmp?.asImageBitmap(),
            playing = st?.state == PlaybackState.STATE_PLAYING,
            durationMs = m.getLong(MediaMetadata.METADATA_KEY_DURATION),
            positionMs = st?.position ?: 0L,
            positionAt = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f,
            packageName = c.packageName,
        )
    }

    fun playPause() { controller?.let { if (it.playbackState?.state == PlaybackState.STATE_PLAYING) it.transportControls.pause() else it.transportControls.play() } }
    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
    fun seekTo(ms: Long) { controller?.transportControls?.seekTo(ms) }
    fun openApp(ctx: Context) {
        val pkg = info?.packageName ?: return
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { runCatching { ctx.startActivity(it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}
