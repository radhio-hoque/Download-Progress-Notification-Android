package com.radhio.filedownloadprogressnotification;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * Created by Azmia Hoque Radhio on 6/3/2021.
 */
public class AppController extends Application {

    private static final String TAG = "AppController";
    public static final String DOWNLOAD_CHANNEL_ID = "radhio_download";
    public static final int NOTIFICATION_MANAGER_ID_DOWNLOAD = 2001;
    private static AppController appController;
    public static boolean isFileDownloading = false;

    @Override
    public void onCreate() {
        super.onCreate();
        appController = this;
        /*Channel For Notification*/
        createNotificationChannelForDownload();
    }

    public static synchronized AppController getInstance() {
        return appController;
    }

    public void setDownloadCancelListener(IDownloadCancel downloadCancelListener) {
        NotificationActionReceiver.iDownloadCancel = downloadCancelListener;
    }

    private void createNotificationChannelForDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel downloadChannel = new NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    "radhio_channel",
                    NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(downloadChannel);
        }
    }

}
