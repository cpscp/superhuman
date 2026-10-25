package com.carlos.superhuman.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsBar
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carlos.superhuman.data.Expense
import com.carlos.superhuman.data.TaskItem
import com.carlos.superhuman.data.beerFeedback
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.ui.Nav
import com.carlos.superhuman.ui.components.CircleIconButton
import com.carlos.superhuman.ui.components.SHCard
import com.carlos.superhuman.ui.components.SHTextField
import com.carlos.superhuman.ui.components.Sheet
import com.carlos.superhuman.ui.theme.SH
import com.carlos.superhuman.ui.theme.Type
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun QuickAddSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf("menu") }
    var shop by remember { mutableStateOf("") }

    when (mode) {
        "task" -> TaskSheet(TaskItem(title = ""), onDismiss)
        "expense" -> ExpenseSheet(Expense(amount = 0.0, merchant = ""), onDismiss)
        "slot" -> {
            val h = (LocalTime.now().hour + 1).coerceAtMost(22)
            AddEventSheet(LocalDate.now(), h * 60, h * 60 + 60, onDismiss = onDismiss, onCreated = onDismiss)
        }
        "shop" -> Sheet("Lista de compras", onDismiss) {
            SHTextField(shop, { shop = it }, "Itens separados por vírgulas", Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            com.carlos.superhuman.ui.components.PrimaryButton("Adicionar", Modifier.fillMaxWidth(), enabled = shop.isNotBlank()) {
                shop.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { SuperOps.addItem(it) }
                onDismiss()
            }
        }
        else -> Sheet("Adicionar", onDismiss) {
            val items: List<Triple<String, String, ImageVector>> = listOf(
                Triple("task", "Task", Icons.Rounded.CheckCircle),
                Triple("expense", "Despesa", Icons.Rounded.Receipt),
                Triple("slot", "Slot", Icons.Rounded.Event),
                Triple("note", "Nota", Icons.Rounded.StickyNote2),
                Triple("beer", "+1 cerveja", Icons.Rounded.SportsBar),
                Triple("shop", "Compras", Icons.Rounded.ShoppingCart),
                Triple("trip", "Ida super", Icons.Rounded.Store),
                Triple("ai", "Pedir à AI", Icons.Rounded.AutoAwesome),
            )
            items.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (key, label, icon) ->
                        SHCard(Modifier.weight(1f), padding = 10.dp, onClick = {
                            when (key) {
                                "note" -> { onDismiss(); Nav.go("note", null) }
                                "beer" -> {
                                    HealthOps.addBeer(1)
                                    Toast.makeText(ctx, Store.data.beerFeedback().title, Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                "trip" -> { onDismiss(); Nav.go("super") }
                                "ai" -> { onDismiss(); Nav.go("assistant") }
                                else -> mode = key
                            }
                        }) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(SH.c.inset), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = SH.c.ink, modifier = Modifier.size(21.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(label, style = Type.tiny, color = SH.c.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
