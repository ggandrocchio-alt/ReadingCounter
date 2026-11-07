package com.ggandrocchio.readingcounter

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ReadingCounterApp() } }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (ReadingCounterService.active(this) && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event.repeatCount == 0) {
                ReadingCounterService.recordActivityKey(this, keyCode)
                sendAction(if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) ReadingCounterService.ACTION_INCREMENT else ReadingCounterService.ACTION_DECREMENT)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (ReadingCounterService.active(this) && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) return true
        return super.onKeyUp(keyCode, event)
    }
    @Composable private fun ReadingCounterApp() {
        var active by remember { mutableStateOf(ReadingCounterService.active(this)) }
        var count by remember { mutableIntStateOf(ReadingCounterService.count(this)) }
        var diagnostic by remember { mutableStateOf(ReadingCounterService.lastEvent(this)) }
        var notificationsAllowed by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
        LaunchedEffect(Unit) { while (true) {
            count = ReadingCounterService.count(this@MainActivity)
            active = ReadingCounterService.active(this@MainActivity)
            diagnostic = ReadingCounterService.lastEvent(this@MainActivity)
            notificationsAllowed = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            delay(250)
        } }
        MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6750A4), secondary = Color(0xFF625B71), background = Color(0xFFF8F5FF))) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Reading Counter", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("La tua lettura, un passo alla volta", style = MaterialTheme.typography.bodyLarge)
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))) { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PAGINE LETTE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("$count", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            } }
            if (!notificationsAllowed) Button(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Abilita notifiche") }
            Button(onClick = { sendAction(ReadingCounterService.ACTION_DECREMENT) }) { Text("Pagina −1") }
            Button(onClick = { sendAction(ReadingCounterService.ACTION_INCREMENT) }) { Text("Pagina +1") }
            OutlinedButton(onClick = { sendAction(ReadingCounterService.ACTION_RESET) }) { Text("Azzera contatore") }
            Button(onClick = {
                if (!active && !notificationsAllowed) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                sendAction(if (active) ReadingCounterService.ACTION_STOP else ReadingCounterService.ACTION_START)
            }) { Text(if (active) "Disattiva Counter Mode" else "Attiva Counter Mode") }
            if (active && !Settings.canDrawOverlays(this@MainActivity)) {
                Button(onClick = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))) }) {
                    Text("Concedi permesso pulsante flottante +1")
                }
                Text("Se Android blocca il permesso: Impostazioni → App → Reading Counter → ⋮ → Consenti impostazioni con restrizioni. Poi torna in Accesso speciale app → Mostra sopra altre app e abilita Reading Counter. Infine riattiva Counter Mode.")
            }
            Text("Con Counter Mode in primo piano: Volume giù +1, Volume su −1. In lettura usa il pulsante flottante o le azioni nella notifica.", style = MaterialTheme.typography.bodyMedium)
        } } }
    }
    private fun sendAction(action: String) {
        val intent = Intent(this, ReadingCounterService::class.java).setAction(action)
        if (action == ReadingCounterService.ACTION_START) ContextCompat.startForegroundService(this, intent)
        else startService(intent)
    }
}
