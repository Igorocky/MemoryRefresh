package org.igye.memoryrefresh

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder


class HttpsServerService: Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val self = this
        Thread {
            val NOTIFICATION_CHANNEL_ID = "org.igye.MemoryRefresh"
            val channelName = "MemoryRefresh HTTPS Server"
            val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)!!
            manager!!.createNotificationChannel(chan)
            val pendingIntent: PendingIntent =
                Intent(self, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(self, 0, notificationIntent, 0)
                }
            val notification: Notification = Notification.Builder(self, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("MemoryRefresh HTTPS server is running")
                .setContentText("tap to view details")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build()
            startForeground(1, notification)
        }.start()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}