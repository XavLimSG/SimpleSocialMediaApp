package com.example.test;

public class TestSecurity {
    // This should be detected by MobScan
    private static final String API_KEY = "sk_live_1234567890abcdef";
    private static final String PASSWORD = "admin123";

    public void insecureCrypto() {
        // This should be detected
        MessageDigest md = MessageDigest.getInstance("MD5");
    }
}
