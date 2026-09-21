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
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private data class Entry(val farmer: String, val liters: String, val rate: String, val date: String) {
    val total: Double get() = (liters.toDoubleOrNull() ?: 0.0) * (rate.toDoubleOrNull() ?: 0.0)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MilkCollectionApp(this) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkCollectionApp(context: Context) {
    val prefs = context.getSharedPreferences("milk", Context.MODE_PRIVATE)
    var entries by remember { mutableStateOf(loadEntries(prefs)) }
    var farmer by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("180") }
    var showForm by remember { mutableStateOf(false) }
    val totalLiters = entries.sumOf { it.liters.toDoubleOrNull() ?: 0.0 }
    val totalAmount = entries.sumOf { it.total }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF2E7D32))) {
        Scaffold(topBar = { TopAppBar(title = { Text("🥛 Milk Collection") }) }, floatingActionButton = { FloatingActionButton(onClick = { showForm = true }) { Text("+") } }) { pad ->
            Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Today", "${entries.size} entries", Modifier.weight(1f)); SummaryCard("Milk", "%.1f L".format(totalLiters), Modifier.weight(1f)); SummaryCard("Amount", "Rs %.0f".format(totalAmount), Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp)); Text("Recent collections", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp))
                if (entries.isEmpty()) Text("No entries yet. Tap + to add a collection.", modifier = Modifier.padding(top = 24.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(entries) { e -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(e.farmer, style = MaterialTheme.typography.titleMedium); Text("${e.liters} L × Rs ${e.rate}  •  ${e.date}") }; Text("Rs %.0f".format(e.total), style = MaterialTheme.typography.titleMedium) } } } }
            }
        }
        if (showForm) AlertDialog(onDismissRequest = { showForm = false }, title = { Text("Add milk collection") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(farmer, { farmer = it }, label = { Text("Farmer name") }, singleLine = true); OutlinedTextField(liters, { liters = it }, label = { Text("Milk (liters)") }, singleLine = true); OutlinedTextField(rate, { rate = it }, label = { Text("Rate per liter") }, singleLine = true) } }, confirmButton = { Button(onClick = { if (farmer.isNotBlank() && liters.toDoubleOrNull() != null) { val e = Entry(farmer.trim(), liters, rate, java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())); entries = listOf(e) + entries; saveEntries(prefs, entries); farmer = ""; liters = ""; showForm = false } }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showForm = false }) { Text("Cancel") } })
    }
}

@Composable fun SummaryCard(label: String, value: String, modifier: Modifier) { Card(modifier) { Column(Modifier.padding(10.dp)) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value, style = MaterialTheme.typography.titleMedium) } } }
private fun loadEntries(p: android.content.SharedPreferences): List<Entry> = try { val a = JSONArray(p.getString("entries", "[]")); List(a.length()) { val o = a.getJSONObject(it); Entry(o.getString("farmer"), o.getString("liters"), o.getString("rate"), o.getString("date")) } } catch (_: Exception) { emptyList() }
private fun saveEntries(p: android.content.SharedPreferences, list: List<Entry>) { val a = JSONArray(); list.forEach { a.put(JSONObject().apply { put("farmer", it.farmer); put("liters", it.liters); put("rate", it.rate); put("date", it.date) }) }; p.edit().putString("entries", a.toString()).apply() }
