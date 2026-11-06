package com.ggandrocchio.readingcounter

import android.app.*; import android.content.Intent; import android.media.session.MediaSession; import android.media.session.PlaybackState; import android.media.VolumeProvider; import android.os.IBinder; import android.util.Log

class ReadingCounterService : Service() {
    companion object { const val ACTION_START="START"; const val ACTION_STOP="STOP"; const val ACTION_INCREMENT="INC"; const val ACTION_DECREMENT="DEC"; private const val CHANNEL="reading_counter" }
    private var session: MediaSession? = null
    override fun onCreate() { super.onCreate(); getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Reading Counter",NotificationManager.IMPORTANCE_LOW)) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { when(intent?.action) { ACTION_START -> startCounter(); ACTION_STOP -> stopCounter(); ACTION_INCREMENT, ACTION_DECREMENT -> Unit }; return START_NOT_STICKY }
    private fun startCounter() {
        val notification=Notification.Builder(this,CHANNEL).setContentTitle("Reading Counter").setContentText("Proof of Concept attiva").setSmallIcon(android.R.drawable.ic_media_play).build()
        startForeground(1,notification)
        session=MediaSession(this,"ReadingCounter").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0L, 1f).setActions(PlaybackState.ACTION_PLAY_PAUSE).build())
            setPlaybackToRemote(object: VolumeProvider(VOLUME_CONTROL_RELATIVE,100,0) {
                override fun onAdjustVolume(direction:Int) { Log.i("ReadingCounter", "volume direction=$direction") }
            })
            isActive=true
        }
    }
    private fun stopCounter() { session?.run { isActive=false; release() }; session=null; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onBind(intent: Intent?): IBinder? = null
}
