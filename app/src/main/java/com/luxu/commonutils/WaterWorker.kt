package com.luxu.commonutils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.luxu.commonutils.activity.WaterActivity

class WaterWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        sendWaterNotify(applicationContext)
        Log.d("------------- WaterWorker --------","")
        return Result.success()

    }

    @SuppressLint("MissingPermission")
    fun sendWaterNotify(appContext: Context) {
        //  通知后跳转的页面
        val intent = Intent(appContext, WaterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 创建渠道
        val channelId = "channelID"
        val manager =
            appContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(channelId, "WaterMinder", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        // 添加操作按钮
        val snoozeIntent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra(Notification.EXTRA_NOTIFICATION_ID, 0)
        }
        val snoozePendingIntent =
            PendingIntent.getBroadcast(appContext, 0, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)

        // 设置通知
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setContentTitle("通知")
            .setContentText("收到一条消息")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.mipmap.add, "按钮", snoozePendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(appContext)) {
            notify(100, notification.build())
        }
    }
}