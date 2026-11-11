package pt.carlos.painel.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.carlos.painel.PainelApp
import pt.carlos.painel.data.AppEntry

/** Mudar o nome e a imagem do ícone de uma app (ou repor o original). */
@Composable
fun IconEditDialog(entry: AppEntry, app: PainelApp, onDismiss: () -> Unit) {
    var name by remember(entry.key) { mutableStateOf(entry.label) }
    val current = app.apps.byKey[entry.key] ?: entry
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) app.apps.setCustomIcon(entry.key, uri)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ícone") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(bitmap = current.icon, contentDescription = current.label, modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Escolher imagem") }
                    TextButton(onClick = {
                        app.apps.resetIcon(entry.key)
                        name = entry.originalLabel
                    }) { Text("Repor original") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { app.apps.setLabel(entry.key, name); onDismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
