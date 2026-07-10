package com.wzf.accounting.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wzf.accounting.R

object NotificationTestHelper {

    private const val TEST_CHANNEL_ID = "test_notification_channel"
    private const val TEST_CHANNEL_NAME = "测试通知"
    private var nextId = 2000

    fun sendNormalFinancialNotification(context: Context) {
        ensureChannel(context)
        val id = nextId++
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("微信支付")
            .setContentText("微信支付: 你已成功付款 ¥12.50 给 美团外卖")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("微信支付: 你已成功付款 ¥12.50 给 美团外卖，当前余额 ¥328.00"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        getNotificationManager(context).notify(id, notification)
    }

    fun sendPrivateFinancialNotification(context: Context) {
        ensureChannel(context)
        val id = nextId++
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("支付宝")
            .setContentText("你有一笔 ¥88.00 的转账到账")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("支付宝通知: 你有一笔 ¥88.00 的转账到账，来自 张三，交易单号 2024071012345"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        getNotificationManager(context).notify(id, notification)
    }

    fun sendNonFinancialNotification(context: Context) {
        ensureChannel(context)
        val id = nextId++
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("系统更新")
            .setContentText("你的设备已更新到最新版本，请重启以完成安装")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        getNotificationManager(context).notify(id, notification)
    }

    fun sendBankTransferNotification(context: Context) {
        ensureChannel(context)
        val id = nextId++
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("招商银行")
            .setContentText("您尾号8862的储蓄卡于07月10日消费支出人民币 256.80元")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("【招商银行】您尾号8862的储蓄卡于07月10日14:32消费支出人民币 256.80元，活期余额为12,345.67元。"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        getNotificationManager(context).notify(id, notification)
    }

    fun sendRedPacketNotification(context: Context) {
        ensureChannel(context)
        val id = nextId++
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("微信红包")
            .setContentText("李四 给你发了一个红包，金额 ¥66.66")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        getNotificationManager(context).notify(id, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getNotificationManager(context)
            if (nm.getNotificationChannel(TEST_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    TEST_CHANNEL_ID,
                    TEST_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "用于测试通知捕获功能"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun getNotificationManager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
