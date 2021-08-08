package com.radhio.filedownloadprogressnotification;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.radhio.filedownloadprogressnotification.databinding.ActivityMainBinding;
/**
 * Created by Azmia Hoque Radhio on 6/3/2021.
 */
public class MainActivity extends AppCompatActivity implements IDownloadCancel{


    public static final String PROGRESS_UPDATE = "progress_update";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private ActivityMainBinding mainBinding;
    private static final String TAG = "MainActivity";
    private NotificationActionReceiver notificationActionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        if (AppController.isFileDownloading){
            fileDownloading();
        }


        mainBinding.downloadButton.setOnClickListener(v -> {
            if (checkPermission()) {
                if (!isConnected()) {
                    Toast.makeText(this, "You are offline. Make sure Wi-Fi or mobile data is turned on, then try again.", Toast.LENGTH_SHORT).show();
                } else {
                    AppController.isFileDownloading = true;
                    registerReceiver();
                    fileDownloading();
                    downloadDocumentFile();
                }
            } else {
                requestPermission();
            }
        });
    }

    //region File Download
    private void downloadDocumentFile() {
        String fileUrl = "photos/YYW9shdLIwo/download?force=true";
        String fileName = "radhio";
        Intent serviceIntent = new Intent(this, DownloadNotificationService.class);
        serviceIntent.putExtra("fileUrl", fileUrl);
        serviceIntent.putExtra("fileName", fileName);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void registerReceiver() {
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PROGRESS_UPDATE);
        bManager.registerReceiver(downloadBroadcastReceiver, intentFilter);
    }



    private final BroadcastReceiver downloadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PROGRESS_UPDATE)) {
                boolean downloadComplete = intent.getBooleanExtra("downloadComplete", false);
                if (!downloadComplete) {
                    Toast.makeText(context, "File does not download completely. Try Again", Toast.LENGTH_SHORT).show();
                }
                AppController.isFileDownloading = false;
                fileNotDownloading();
            }
        }
    };


    private void fileDownloading(){
        mainBinding.downloadButton.setVisibility(View.GONE);
        mainBinding.progressbarDownload.setVisibility(View.VISIBLE);
    }

    private void fileNotDownloading(){
        mainBinding.downloadButton.setVisibility(View.VISIBLE);
        mainBinding.progressbarDownload.setVisibility(View.GONE);
    }
    //endregion


    //region Get Write Permission
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }
    //endregion

    /*Manual check internet connection*/
    public static boolean isConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) AppController
                    .getInstance()
                    .getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onDownloadCancel(boolean isCanceled) {
        if (isCanceled){
            fileNotDownloading();
            Log.d(TAG, "onDownloadCancel: "+ true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerAllBroadcastReceiver();
        AppController.getInstance().setDownloadCancelListener(this);
    }

    protected void registerAllBroadcastReceiver() {
        notificationActionReceiver = new NotificationActionReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(notificationActionReceiver, new IntentFilter("Download_cancel"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(notificationActionReceiver, new IntentFilter("Download_cancel"));
        }
    }

    protected void unregisterNetwork() {
        try {
            if (notificationActionReceiver != null) {
                unregisterReceiver(notificationActionReceiver);
                notificationActionReceiver = null;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterNetwork();
    }
}