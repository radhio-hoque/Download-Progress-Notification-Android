package com.radhio.filedownloadprogressnotification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
/**
 * Created by Azmia Hoque Radhio on 6/3/2021.
 */
public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationAction";

    public static IDownloadCancel iDownloadCancel;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int notificationId = intent.getIntExtra("notification_id", 0);
            Log.d(TAG, "onReceive: " + notificationId);
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
            context.stopService(new Intent(context, DownloadNotificationService.class));
            Log.d(TAG, "onDownloadCancel: " + true);
            AppController.isFileDownloading = false;
            try {
                if (iDownloadCancel != null) {
                    iDownloadCancel.onDownloadCancel(true);
                    Log.d(TAG, "onReceive: " + true);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "onReceive: Empty");
        }
    }
}
