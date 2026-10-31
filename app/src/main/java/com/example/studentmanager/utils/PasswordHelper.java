package com.example.studentmanager.utils;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Dung chung cho login va tao tai khoan. Khong trim mat khau. */
public final class PasswordHelper {
    private static final int ITERATIONS = 600000;
    private PasswordHelper() {}

    public static String hash(String password) {
        if (password == null || password.trim().isEmpty() || password.length() > 256) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 1 đến 256 ký tự, không chỉ có khoảng trắng.");
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2_sha256$" + ITERATIONS + "$" + toHex(salt) + "$" + toHex(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String savedHash) {
        if (password == null || password.length() > 256 || savedHash == null) return false;
        try {
            String[] parts = savedHash.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 10000 || iterations > 1000000 || parts[2].length() != 32 || parts[3].length() != 64) return false;
            return MessageDigest.isEqual(fromHex(parts[3]), derive(password, fromHex(parts[2]), iterations));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Không thể xử lý mật khẩu.", error);
        } finally {
            spec.clearPassword();
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(Character.forDigit((value & 255) / 16, 16));
            result.append(Character.forDigit(value & 15, 16));
        }
        return result.toString();
    }

    private static byte[] fromHex(String text) {
        byte[] result = new byte[text.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(text.charAt(i * 2), 16);
            int low = Character.digit(text.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) throw new IllegalArgumentException("Invalid hex");
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }
}
