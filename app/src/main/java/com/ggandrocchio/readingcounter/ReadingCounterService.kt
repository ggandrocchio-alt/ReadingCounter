package com.ggandrocchio.readingcounter

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class ReadingCounterService : Service() {
    companion object {
        const val ACTION_START="START"; const val ACTION_STOP="STOP"; const val ACTION_INCREMENT="INC"; const val ACTION_DECREMENT="DEC"; const val ACTION_RESET="RESET"
        private const val CHANNEL="reading_counter"; private const val PREFS="reading_counter_poc"
        private const val CURRENT_DAY = "current_day"
        private const val DAY_COUNT_PREFIX = "day_count_"
        internal const val COUNT_KEY = "count"
        private val dayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.ITALIAN)

        data class DailyCounter(val date: LocalDate, val count: Int)

        internal fun preferences(context: Context) = context.getSharedPreferences(PREFS, MODE_PRIVATE)

        private fun ensureCurrentDay(context: Context, today: LocalDate) {
            val prefs = preferences(context)
            val currentDay = prefs.getString(CURRENT_DAY, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val existingCount = prefs.getInt(COUNT_KEY, 0)
            if (currentDay == null) {
                // Keep the count saved by versions that predate daily history.
                prefs.edit().putString(CURRENT_DAY, today.toString()).putInt(DAY_COUNT_PREFIX + today, existingCount).apply()
            } else if (currentDay != today) {
                prefs.edit().putInt(DAY_COUNT_PREFIX + currentDay, existingCount)
                    .putString(CURRENT_DAY, today.toString()).putInt(COUNT_KEY, 0).apply()
            }
        }

        fun count(context: Context): Int {
            ensureCurrentDay(context, LocalDate.now())
            return preferences(context).getInt(COUNT_KEY, 0)
        }

        fun history(context: Context): List<DailyCounter> {
            val today = LocalDate.now()
            ensureCurrentDay(context, today)
            return preferences(context).all.asSequence()
                .filter { it.key.startsWith(DAY_COUNT_PREFIX) }
                .mapNotNull { entry ->
                    val date = runCatching { LocalDate.parse(entry.key.removePrefix(DAY_COUNT_PREFIX)) }.getOrNull()
                    val value = entry.value as? Int
                    if (date != null && value != null && date != today) DailyCounter(date, value) else null
                }
                .sortedByDescending { it.date }
                .toList()
        }

        fun formatDate(date: LocalDate): String = date.format(dayFormatter)

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ReadingCounterService::class.java).setAction(ACTION_START)
            )
        }
        fun active(context: Context): Boolean = context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("active", false)
        fun lastEvent(context: Context): String = context.getSharedPreferences(PREFS, MODE_PRIVATE).getString("last_event", "Nessun evento volume ricevuto") ?: ""
        fun recordActivityKey(context: Context, keyCode: Int) {
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("last_event", "Activity onKeyDown: $keyCode").commit()
        }
    }
    private var session: MediaSession? = null
    override fun onCreate() { super.onCreate(); getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Reading Counter",NotificationManager.IMPORTANCE_LOW)) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCounter()
            ACTION_STOP -> {
                releaseCounter()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
            ACTION_INCREMENT -> changeCount(1)
            ACTION_DECREMENT -> changeCount(-1)
            ACTION_RESET -> resetCount()
        }
        return START_NOT_STICKY
    }
    private fun actionIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(this, requestCode, Intent(this, ReadingCounterService::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun notification(): Notification = Notification.Builder(this,CHANNEL).setContentTitle("Reading Counter").setContentText("Pagine: ${count(this)}").setSmallIcon(R.drawable.ic_reading_counter).setOngoing(true)
        .setStyle(Notification.MediaStyle().setMediaSession(session?.sessionToken).setShowActionsInCompactView(0, 1))
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setCategory(Notification.CATEGORY_PROGRESS)
        .setPriority(Notification.PRIORITY_MAX)
        .setShowWhen(false)
        .setColor(0xFF6750A4.toInt())
        .addAction(Notification.Action.Builder(null, "−1", actionIntent(ACTION_DECREMENT, 2)).build())
        .addAction(Notification.Action.Builder(null, "+1", actionIntent(ACTION_INCREMENT, 3)).build()).build()
    private fun changeCount(delta: Int) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val updated = (count(this).toLong() + delta).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        if (prefs.edit().putInt(COUNT_KEY, updated).commit()) {
            if (active(this)) getSystemService(NotificationManager::class.java).notify(1, notification())
            Log.i("ReadingCounter", "count=$updated delta=$delta")
        } else Log.e("ReadingCounter", "Unable to save count")
    }
    private fun resetCount() {
        ensureCurrentDay(this, LocalDate.now())
        preferences(this).edit().putInt(COUNT_KEY, 0).commit()
        if (active(this)) getSystemService(NotificationManager::class.java).notify(1, notification())
    }
    private fun recordEvent(value: String) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("last_event", value).commit()
        Log.i("ReadingCounter", value)
    }
    private fun startCounter() {
        if (session == null) {
            session = createMediaSession()
            startForeground(1, notification())
            recordEvent("Counter Mode avviato; in attesa dei tasti")
            preferences(this).edit().putBoolean("active", true).commit()
        }
        if (Settings.canDrawOverlays(this)) startService(Intent(this, ReadingCounterOverlayService::class.java))
    }
    private fun createMediaSession(): MediaSession =
        MediaSession(this, "ReadingCounter").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0L, 1f).setActions(PlaybackState.ACTION_PLAY_PAUSE).build())
            setPlaybackToRemote(object: VolumeProvider(VOLUME_CONTROL_RELATIVE,100,50) {
                override fun onAdjustVolume(direction:Int) {
                    ContextCompat.getMainExecutor(this@ReadingCounterService).execute {
                        if (session != this@apply) return@execute
                        recordEvent("MediaSession onAdjustVolume: $direction")
                        if (direction > 0) changeCount(-1) else if (direction < 0) changeCount(1)
                    }
                }
                override fun onSetVolumeTo(volume: Int) {
                    ContextCompat.getMainExecutor(this@ReadingCounterService).execute {
                        if (session == this@apply) recordEvent("MediaSession onSetVolumeTo: $volume (nessun conteggio)")
                    }
                }
            })
            isActive=true
        }
    private fun releaseCounter() {
        session?.run {
            isActive = false
            release()
        }
        session = null
        preferences(this).edit().putBoolean("active", false).commit()
        stopService(Intent(this, ReadingCounterOverlayService::class.java))
    }

    override fun onDestroy() {
        releaseCounter()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
