package com.jeremyfox.PhotoMojo.Helpers;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import com.jeremyfox.PhotoMojo.MainActivity;
import com.jeremyfox.PhotoMojo.R;

import java.text.Format;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 5/15/13
 * Time: 9:19 PM
 */
public class NotificationHelper extends BroadcastReceiver {

    private static final int TEN_SECONDS = 10000;

    public static void createNotification(Context context, String message, Bitmap image) {

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setSmallIcon(R.drawable.pm_icon);
        if (null != image) notificationBuilder.setLargeIcon(image);
        notificationBuilder.setContentTitle(context.getString(R.string.app_name));
        notificationBuilder.setContentText(message);
        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = notificationBuilder.build();
        notification.tickerText = message;
        mNotificationManager.notify(0, notification);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TAG");
        wl.acquire();

        String message = "Don't forget to stop back soon";
        NotificationHelper.createNotification(context, message, null);

        wl.release();
    }

    public static void setNotificationTimer(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationHelper.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + TEN_SECONDS, pi);
    }
}
