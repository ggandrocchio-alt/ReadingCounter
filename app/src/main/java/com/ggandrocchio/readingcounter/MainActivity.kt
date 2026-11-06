package com.ggandrocchio.readingcounter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ReadingCounterApp() } }
    @Composable private fun ReadingCounterApp() {
        var active by remember { mutableStateOf(false) }; var count by remember { mutableIntStateOf(0) }
        MaterialTheme { Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Reading Counter", style = MaterialTheme.typography.headlineMedium); Text("MediaSession Proof of Concept")
            Text("Contatore: $count", style = MaterialTheme.typography.displaySmall)
            Button(onClick = { count--; sendAction(ReadingCounterService.ACTION_DECREMENT) }) { Text("Pagina −1") }
            Button(onClick = { count++; sendAction(ReadingCounterService.ACTION_INCREMENT) }) { Text("Pagina +1") }
            Button(onClick = { active = !active; sendAction(if (active) ReadingCounterService.ACTION_START else ReadingCounterService.ACTION_STOP) }) { Text(if (active) "Disattiva Counter Mode" else "Attiva Counter Mode") }
            Text("In PoC: Volume DOWN incrementa e Volume UP decrementa quando il servizio è attivo. Verificare su dispositivo reale con Kindle e Google Play Libri.", style = MaterialTheme.typography.bodyMedium)
        } } }
    }
    private fun sendAction(action: String) { ContextCompat.startForegroundService(this, Intent(this, ReadingCounterService::class.java).setAction(action)) }
}
