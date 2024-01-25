package com.wayThereTeam.wayThere.utilities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wayThereTeam.wayThere.R
import java.time.Duration

/**
 * Managing notifications
 */
class Notification : BroadcastReceiver() {
    // Static variable
    companion object {
        private var notificationId = 0
    }

    private fun getChannelId(): String {
        notificationId++
        return "channel_$notificationId"
    }

    /**
     * Method from BroadcastReceiver is called when we receive action on receiver after delay
     * @param context from an activity currently working from
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "DELAYED_NOTIFICATION") {
            context ?: return
            val notificationData = intent.getParcelableExtra<NotificationData>("notification_data") ?: return
            send(context, notificationData)
        }
    }

    /**
     * Sending notification and displaying it
     * @param context from an activity currently working from
     * @param notificationData data, for notification
     */
    @SuppressLint("MissingPermission")
    fun send(context: Context, notificationData: NotificationData) {
        val channelId = getChannelId()
        // Create a notification manager
        val notificationManager = NotificationManagerCompat.from(context)

        // Create a notification channel if not already created
        val channel = NotificationChannel(
            channelId, notificationData.channelName, notificationData.importance
        )
        notificationManager.createNotificationChannel(channel)

        // Create a notification
        val builder = NotificationCompat.Builder(context, channelId).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(ContextCompat.getString(context, R.string.app_name)).setContentText(notificationData.text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Delaying sending notification
     * @param context from an activity currently working from
     * @param notificationData data, for notification
     * @param delayTime for delaying sending notification
     */
    @SuppressLint("ScheduleExactAlarm")
    fun delay(context: Context, notificationData: NotificationData, delayTime: Duration) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, Notification::class.java)
        intent.action = "DELAYED_NOTIFICATION"
        intent.putExtra("notification_data", notificationData)

        val pendingIntent = PendingIntent.getBroadcast(
            context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + delayTime.toMillis()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
        )
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        if (intent == null) return

        val notification = Notification()
        val notificationData = intent.getParcelableExtra<NotificationData>("notification_data") ?: return
        notification.send(context, notificationData)
    }
}
