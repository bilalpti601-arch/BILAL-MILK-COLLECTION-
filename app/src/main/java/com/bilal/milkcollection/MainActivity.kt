package com.bilal.milkcollection

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val farmer: String,
    val liters: Double,
    val rate: Double,
    val fat: String,
    val snf: String,
    val paid: Boolean,
    val date: String
) { val total: Double get() = liters * rate }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MilkCollectionApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkCollectionApp(context: Context) {
    val prefs = context.getSharedPreferences("milk", Context.MODE_PRIVATE)
    var entries by remember { mutableStateOf(loadEntries(prefs)) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Entry?>(null) }
    val liters = entries.sumOf { it.liters }
    val amount = entries.sumOf { it.total }
    val unpaid = entries.filterNot { it.paid }.sumOf { it.total }

    fun save(list: List<Entry>) { entries = list; saveEntries(prefs, list) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF2E7D32))) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("🥛 Milk Collection") }) },
            floatingActionButton = { FloatingActionButton(onClick = { editing = null; showForm = true }) { Text("+") } }
        ) { pad ->
            Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Entries", entries.size.toString(), Modifier.weight(1f))
                    SummaryCard("Milk", "%.1f L".format(liters), Modifier.weight(1f))
                    SummaryCard("Amount", "Rs %.0f".format(amount), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text("Unpaid: Rs %.0f".format(unpaid), color = Color(0xFFC62828))
                Spacer(Modifier.height(16.dp))
                Text("Collections", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (entries.isEmpty()) Text("No entries yet. Tap + to add a collection.")
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries, key = { it.id }) { entry ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.farmer, style = MaterialTheme.typography.titleMedium)
                                        Text("${entry.liters} L × Rs ${entry.rate}  •  ${entry.date}")
                                        if (entry.fat.isNotBlank() || entry.snf.isNotBlank()) Text("Fat: ${entry.fat.ifBlank { "-" }}  SNF: ${entry.snf.ifBlank { "-" }}")
                                    }
                                    Text("Rs %.0f".format(entry.total), style = MaterialTheme.typography.titleMedium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { save(entries.map { if (it.id == entry.id) it.copy(paid = !it.paid) else it }) }) { Text(if (entry.paid) "Paid ✓" else "Mark paid") }
                                    TextButton(onClick = { editing = entry; showForm = true }) { Text("Edit") }
                                    TextButton(onClick = { deleteTarget = entry }) { Text("Delete", color = Color.Red) }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showForm) EntryDialog(editing, onDismiss = { showForm = false }, onSave = { value ->
            save(if (editing == null) listOf(value) + entries else entries.map { if (it.id == value.id) value else it })
            showForm = false
        })
        deleteTarget?.let { target ->
            AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete entry?") }, text = { Text("Delete ${target.farmer}'s collection?") }, confirmButton = { Button(onClick = { save(entries.filterNot { it.id == target.id }); deleteTarget = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDialog(existing: Entry?, onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    var farmer by remember { mutableStateOf(existing?.farmer ?: "") }
    var liters by remember { mutableStateOf(existing?.liters?.toString() ?: "") }
    var rate by remember { mutableStateOf(existing?.rate?.toString() ?: "180") }
    var fat by remember { mutableStateOf(existing?.fat ?: "") }
    var snf by remember { mutableStateOf(existing?.snf ?: "") }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Add milk collection" else "Edit collection") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(farmer, { farmer = it }, label = { Text("Farmer name") }, singleLine = true)
            OutlinedTextField(liters, { liters = it }, label = { Text("Milk (liters)") }, singleLine = true)
            OutlinedTextField(rate, { rate = it }, label = { Text("Rate per liter") }, singleLine = true)
            OutlinedTextField(fat, { fat = it }, label = { Text("Fat (optional)") }, singleLine = true)
            OutlinedTextField(snf, { snf = it }, label = { Text("SNF (optional)") }, singleLine = true)
            if (error.isNotBlank()) Text(error, color = Color.Red)
        }
    }, confirmButton = { Button(onClick = {
        val l = liters.toDoubleOrNull(); val r = rate.toDoubleOrNull()
        if (farmer.isBlank() || l == null || l <= 0 || r == null || r < 0) error = "Name, liters and valid rate are required"
        else onSave(Entry(existing?.id ?: UUID.randomUUID().toString(), farmer.trim(), l, r, fat.trim(), snf.trim(), existing?.paid ?: false, existing?.date ?: today()))
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun SummaryCard(label: String, value: String, modifier: Modifier) { Card(modifier) { Column(Modifier.padding(10.dp)) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value, style = MaterialTheme.typography.titleMedium) } } }
private fun today() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
private fun loadEntries(p: android.content.SharedPreferences): List<Entry> = try { val a = JSONArray(p.getString("entries", "[]")); List(a.length()) { val o = a.getJSONObject(it); Entry(o.optString("id", UUID.randomUUID().toString()), o.getString("farmer"), o.getDouble("liters"), o.getDouble("rate"), o.optString("fat"), o.optString("snf"), o.optBoolean("paid"), o.getString("date")) } } catch (_: Exception) { emptyList() }
private fun saveEntries(p: android.content.SharedPreferences, list: List<Entry>) { val a = JSONArray(); list.forEach { e -> a.put(JSONObject().apply { put("id", e.id); put("farmer", e.farmer); put("liters", e.liters); put("rate", e.rate); put("fat", e.fat); put("snf", e.snf); put("paid", e.paid); put("date", e.date) }) }; p.edit().putString("entries", a.toString()).apply() }
