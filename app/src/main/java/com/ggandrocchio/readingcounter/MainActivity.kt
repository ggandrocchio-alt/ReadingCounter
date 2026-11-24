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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var overlaysAllowed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ReadingCounterApp() } }
    override fun onResume() {
        super.onResume()
        val overlaysWereAllowed = overlaysAllowed
        overlaysAllowed = Settings.canDrawOverlays(this)
        if (overlaysAllowed && !overlaysWereAllowed && ReadingCounterService.active(this)) {
            ReadingCounterService.start(this)
        }
    }
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
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ReadingCounterApp() {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6750A4), secondary = Color(0xFF625B71), background = Color(0xFFF8F5FF))) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Text(
                        "Reading Counter",
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Oggi") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Storico") })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Impostazioni") })
                    }
                    when (selectedTab) {
                        0 -> CounterTab()
                        1 -> HistoryTab()
                        2 -> SettingsTab()
                    }
                }
            }
        }
    }

    @Composable
    private fun CounterTab() {
        var active by remember { mutableStateOf(ReadingCounterService.active(this)) }
        var count by remember { mutableIntStateOf(ReadingCounterService.count(this)) }
        var notificationsAllowed by remember {
            mutableStateOf(Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        }
        LaunchedEffect(Unit) {
            while (true) {
                count = ReadingCounterService.count(this@MainActivity)
                active = ReadingCounterService.active(this@MainActivity)
                notificationsAllowed = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                delay(250)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("La tua lettura, un passo alla volta", style = MaterialTheme.typography.bodyLarge)
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("PAGINE LETTE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("$count", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (!notificationsAllowed) Button(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Abilita notifiche") }
            Button(onClick = { sendAction(ReadingCounterService.ACTION_DECREMENT) }) { Text("Pagina −1") }
            Button(onClick = { sendAction(ReadingCounterService.ACTION_INCREMENT) }) { Text("Pagina +1") }
            OutlinedButton(onClick = { sendAction(ReadingCounterService.ACTION_RESET) }) { Text("Azzera contatore") }
            Button(onClick = {
                if (active) {
                    sendAction(ReadingCounterService.ACTION_STOP)
                } else {
                    ReadingCounterService.start(this@MainActivity)
                    if (!notificationsAllowed) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }) { Text(if (active) "Disattiva Counter Mode" else stringResource(R.string.start_counter_mode)) }
            if (active && !overlaysAllowed) {
                Button(onClick = { startActivity(overlayPermissionIntent(this@MainActivity)) }) {
                    Text("Concedi permesso pulsante flottante")
                }
                Text("Se Android blocca il permesso, apri Impostazioni → App → Reading Counter → ⋮ → Consenti impostazioni con restrizioni. Poi abilita Reading Counter in Accesso speciale app → Mostra sopra altre app.")
            }
            Text("Con Counter Mode in primo piano: Volume giù +1, Volume su −1. In lettura usa il pulsante flottante o le azioni nella notifica.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    @Composable
    private fun HistoryTab() {
        var history by remember { mutableStateOf(ReadingCounterService.history(this)) }
        LaunchedEffect(Unit) {
            while (true) {
                history = ReadingCounterService.history(this@MainActivity)
                delay(250)
            }
        }
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Contatori giornalieri", style = MaterialTheme.typography.titleLarge)
            if (history.isEmpty()) {
                Text("Non ci sono ancora giorni precedenti.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(history) { day ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ReadingCounterService.formatDate(day.date), style = MaterialTheme.typography.bodyLarge)
                                Text("${day.count} pagine", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsTab() {
        var selectedColor by remember { mutableStateOf(FloatingButtonColor.selected(this)) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text("Colore del pulsante flottante", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.selectableGroup()) {
                for (color in FloatingButtonColor.entries) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(
                            selected = selectedColor == color,
                            role = Role.RadioButton,
                            onClick = {
                                color.save(this@MainActivity)
                                selectedColor = color
                            }
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedColor == color, onClick = null)
                        Box(Modifier.size(24.dp).background(Color(color.argb), CircleShape))
                        Text(color.label)
                    }
                }
            }
        }
    }

    private fun sendAction(action: String) {
        startService(Intent(this, ReadingCounterService::class.java).setAction(action))
    }
}
