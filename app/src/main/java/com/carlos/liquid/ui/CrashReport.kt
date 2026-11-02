package com.carlos.liquid.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Se o Liquid fechou da última vez, mostra o erro para o poderes copiar e enviar. */
@Composable
fun BoxScope.CrashReport() {
    val ctx = LocalContext.current
    val f = remember { File(ctx.filesDir, "crash.txt") }
    var text by remember { mutableStateOf(runCatching { if (f.exists()) f.readText() else null }.getOrNull()) }
    val t = text ?: return
    val close = { f.delete(); text = null }
    IOSAlert(
        true,
        "O Liquid fechou inesperadamente",
        "Copia o relatório e envia-o ao Claude para corrigir.\n\n" + t.lines().drop(3).take(4).joinToString("\n"),
        listOf(
            AlertButton("Copiar relatório", bold = true) {
                ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Liquid", t))
                Toast.makeText(ctx, "Relatório copiado", Toast.LENGTH_SHORT).show()
                close()
            },
            AlertButton("Fechar") { close() },
        ),
    ) { close() }
}
