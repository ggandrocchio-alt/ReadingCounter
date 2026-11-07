package com.ggandrocchio.readingcounter

import android.app.*; import android.content.Context; import android.content.Intent; import android.media.session.MediaSession; import android.media.session.PlaybackState; import android.media.VolumeProvider; import android.os.IBinder; import android.util.Log

class ReadingCounterService : Service() {
    companion object {
        const val ACTION_START="START"; const val ACTION_STOP="STOP"; const val ACTION_INCREMENT="INC"; const val ACTION_DECREMENT="DEC"
        private const val CHANNEL="reading_counter"; private const val PREFS="reading_counter_poc"
        fun count(context: Context): Int = context.getSharedPreferences(PREFS, MODE_PRIVATE).getInt("count", 0)
        fun active(context: Context): Boolean = context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("active", false)
        fun lastEvent(context: Context): String = context.getSharedPreferences(PREFS, MODE_PRIVATE).getString("last_event", "Nessun evento volume ricevuto") ?: ""
        fun recordActivityKey(context: Context, keyCode: Int) {
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("last_event", "Activity onKeyDown: $keyCode").commit()
        }
    }
    private var session: MediaSession? = null
    override fun onCreate() { super.onCreate(); getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Reading Counter",NotificationManager.IMPORTANCE_LOW)) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { when(intent?.action) { ACTION_START -> startCounter(); ACTION_STOP -> stopCounter(); ACTION_INCREMENT -> changeCount(1); ACTION_DECREMENT -> changeCount(-1) }; return START_NOT_STICKY }
    private fun notification(): Notification = Notification.Builder(this,CHANNEL).setContentTitle("Reading Counter").setContentText("Pagine: ${count(this)}").setSmallIcon(android.R.drawable.ic_media_play).setOngoing(true).build()
    private fun changeCount(delta: Int) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val updated = (prefs.getInt("count", 0).toLong() + delta).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        if (prefs.edit().putInt("count", updated).commit()) {
            if (active(this)) getSystemService(NotificationManager::class.java).notify(1, notification())
            Log.i("ReadingCounter", "count=$updated delta=$delta")
        } else Log.e("ReadingCounter", "Unable to save count")
    }
    private fun recordEvent(value: String) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("last_event", value).commit()
        Log.i("ReadingCounter", value)
    }
    private fun startCounter() {
        if (session != null) return
        recordEvent("Counter Mode avviato; in attesa dei tasti")
        startForeground(1,notification())
        session=MediaSession(this,"ReadingCounter").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0L, 1f).setActions(PlaybackState.ACTION_PLAY_PAUSE).build())
            setPlaybackToRemote(object: VolumeProvider(VOLUME_CONTROL_RELATIVE,100,50) {
                override fun onAdjustVolume(direction:Int) {
                    recordEvent("MediaSession onAdjustVolume: $direction")
                    if (direction > 0) changeCount(-1) else if (direction < 0) changeCount(1)
                }
                override fun onSetVolumeTo(volume: Int) {
                    recordEvent("MediaSession onSetVolumeTo: $volume (nessun conteggio)")
                }
            })
            isActive=true
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("active", true).commit()
    }
    private fun stopCounter() { session?.run { isActive=false; release() }; session=null; getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("active", false).commit(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { session?.run { isActive=false; release() }; session=null; getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("active", false).commit(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
