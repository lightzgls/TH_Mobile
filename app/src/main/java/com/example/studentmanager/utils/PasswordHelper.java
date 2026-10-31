package com.example.studentmanager.utils;

/**
 * TODO An: hash va verify cung format voi seed trong DBHelper.
 * PBKDF2WithHmacSHA256, UTF-8, 600000 iterations, random salt 16 bytes, key 256 bit.
 * Luu: pbkdf2_sha256$600000$saltHex$hashHex. Khong log hoac luu raw password.
 */
public final class PasswordHelper {
    private PasswordHelper() {}
    public static String hash(String rawPassword) {
        throw new UnsupportedOperationException("TODO An: PasswordHelper.hash");
    }
    public static boolean verify(String rawPassword, String savedHash) {
        return false;
    }
}
