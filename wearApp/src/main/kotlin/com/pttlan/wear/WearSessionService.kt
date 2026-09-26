package com.pttlan.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Keeps a session alive with the screen off or the app closed, shown on the watch face as an Ongoing Activity.
 * It lasts as long as the connection: no "always listening" on a watch battery (ADR 0011).
 */
class WearSessionService : Service() {
    private val connectionRepository: ConnectionRepository by inject()
    private val lanNetwork: LanNetwork by inject()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        lanNetwork.acquire()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.session_channel_name), NotificationManager.IMPORTANCE_LOW),
        )
        scope.launch {
            connectionRepository.connectionStatus.first { it == ConnectionStatus.Disconnected }
            stopSelf()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_LEAVE) {
            connectionRepository.disconnect()
            return START_NOT_STICKY
        }
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val leave =
            PendingIntent.getService(
                this,
                1,
                Intent(this, WearSessionService::class.java).setAction(ACTION_LEAVE),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.session_connected))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.session_leave), leave)
        OngoingActivity
            .Builder(this, NOTIFICATION_ID, notification)
            .setStaticIcon(android.R.drawable.ic_btn_speak_now)
            .setTouchIntent(open)
            .setStatus(Status.Builder().addTemplate(getString(R.string.session_status)).build())
            .build()
            .apply(this)
        startForeground(NOTIFICATION_ID, notification.build())
        return START_NOT_STICKY
    }

    // Swiped away from the recent apps: closing the app ends the session, as leaving from its first screen does
    override fun onTaskRemoved(rootIntent: Intent?) {
        connectionRepository.disconnect()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        lanNetwork.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val CHANNEL_ID = "ptt_session"
        const val NOTIFICATION_ID = 1
        const val ACTION_LEAVE = "com.pttlan.wear.LEAVE"
    }
}
