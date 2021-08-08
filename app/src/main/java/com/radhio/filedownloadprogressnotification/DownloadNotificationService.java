package com.radhio.filedownloadprogressnotification;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Random;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;

import static com.radhio.filedownloadprogressnotification.AppController.DOWNLOAD_CHANNEL_ID;
import static com.radhio.filedownloadprogressnotification.AppController.NOTIFICATION_MANAGER_ID_DOWNLOAD;
/**
 * Created by Azmia Hoque Radhio on 6/3/2021.
 */
public class DownloadNotificationService extends IntentService {

    private static final String TAG = "DownloadNotification";
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private PowerManager.WakeLock wakeLock;
    public Call<ResponseBody> documentFileDownloadCall;

    private String fileName = "";
    private String fileUrl = "";

    public DownloadNotificationService() {
        super("DownloadIntentService");
        setIntentRedelivery(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        /*WakeLock for Service running on screen locked*/
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        /*PARTIAL_WAKE_LOCK if the Screen is locked run the CPU*/
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                TAG + ":Wakelock");
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        Log.d(TAG, "Wakelock acquired");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            fileUrl = intent.getStringExtra("fileUrl");
            fileName = intent.getStringExtra("fileName");
        }
        intNotification();
    }

    private void intNotification() {
        Intent actionIntent = new Intent(this, NotificationActionReceiver.class);
        actionIntent.putExtra("notification_id", NOTIFICATION_MANAGER_ID_DOWNLOAD);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, actionIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(fileName)
                    .setContentText("Downloading...")
                    .addAction(R.drawable.ic_launcher_background, "Cancel", pendingIntent)
                    .setColor(getResources().getColor(R.color.purple_200));

            startForeground(NOTIFICATION_MANAGER_ID_DOWNLOAD, notificationBuilder.build());
        }
        notificationManager.notify(NOTIFICATION_MANAGER_ID_DOWNLOAD, notificationBuilder.build());
        intRetrofit();
    }

    private void intRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://unsplash.com/")
                .build();

        IClientDocument iClientDocument = retrofit.create(IClientDocument.class);
        documentFileDownloadCall = iClientDocument.downloadImage(fileUrl);
        try {
            Log.d(TAG, "server connected and has a file");
            boolean writtenToDisk =
                    writeResponseBodyToDisk(Objects.requireNonNull(documentFileDownloadCall.execute().body()));
            Log.d(TAG, "file download was a success? " + writtenToDisk);
            Log.d(TAG, "File sent to save in Memory");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "onHandleIntent: No file found from server" + e.getMessage());
        }
    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            int count;
            long fileSizeDownloaded = 0;
            boolean downloadComplete = false;

            File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),generateDocumentFileName());
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] data = new byte[1024 * 4];
                long fileSize = body.contentLength();
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(outputFile);

                while ((count = inputStream.read(data)) != -1) {
                    fileSizeDownloaded += count;
                    int progress = (int) ((double) (fileSizeDownloaded * 100) / (double) fileSize);
                    updateNotification(progress);
                    outputStream.write(data, 0, count);
                    downloadComplete = true;
                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }
                onDownloadComplete(downloadComplete);
                Log.d(TAG, "writeResponseBodyToDisk: DownloadComplete? "+downloadComplete);
                outputStream.flush();
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    private String generateDocumentFileName(){
        Random generator = new Random();
        int randomNumber = 10000;
        randomNumber = generator.nextInt(randomNumber);
        String file = fileName+" "+randomNumber+"."+getExtension();
        Log.d(TAG, "generateDocumentFileName: "+file);
        return fileName;
    }

    private String getExtension(){
        String extension = "";
        int i = fileUrl.lastIndexOf('.');
        if (i > 0) {
            extension = fileUrl.substring(i+1);
        }
        return "jpg";
    }


    private void updateNotification(int currentProgress) {
        notificationBuilder.setProgress(100, currentProgress, false);
        notificationBuilder.setContentText("Downloaded: " + currentProgress + "%");
        notificationManager.notify(NOTIFICATION_MANAGER_ID_DOWNLOAD, notificationBuilder.build());
    }


    private void sendProgressUpdate(boolean downloadComplete) {
        Intent intent = new Intent(MainActivity.PROGRESS_UPDATE);
        intent.putExtra("downloadComplete", downloadComplete);
        LocalBroadcastManager.getInstance(DownloadNotificationService.this).sendBroadcast(intent);
    }

    private void onDownloadComplete(boolean downloadComplete) {
        sendProgressUpdate(downloadComplete);

        notificationManager.cancel(NOTIFICATION_MANAGER_ID_DOWNLOAD);
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.setContentText("File Download Complete");
        notificationManager.notify(NOTIFICATION_MANAGER_ID_DOWNLOAD, notificationBuilder.build());

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        notificationManager.cancel(NOTIFICATION_MANAGER_ID_DOWNLOAD);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        documentFileDownloadCall.cancel();
        AppController.isFileDownloading = false;
        Log.d(TAG, "stopService: cancel");
        stopSelf();
        Log.d(TAG, "onDestroy");
        wakeLock.release();
        Log.d(TAG, "Wakelock released");
        notificationManager.cancel(NOTIFICATION_MANAGER_ID_DOWNLOAD);
    }
}
