package com.example.studentmanager.utils;

import static org.junit.Assert.*;
import org.junit.Test;

public class PasswordHelperTest {
    private static final String ADMIN = "pbkdf2_sha256$600000$81c63b2b2bbdec35791a438969253ab6$d0b31912d211922fbfc88c7a471b9bd4c88fa08f8a5fc7d1c745ea6626fe8af7";
    private static final String STUDENT = "pbkdf2_sha256$600000$3e0811702098364cdf30b83b7da434d3$411e9eda26f39b2fe0eb5d428f95734d8d5f18286800bf4885b89ad7de856fd9";

    @Test public void acceptsSeedPasswordsButRejectsWrongPassword() {
        assertTrue(PasswordHelper.verify("Admin@123", ADMIN));
        assertTrue(PasswordHelper.verify("Student@123", STUDENT));
        assertFalse(PasswordHelper.verify("admin@123", ADMIN));
        assertFalse(PasswordHelper.verify(" Admin@123", ADMIN));
    }

    @Test public void randomSaltAndUnicodeRoundTrip() {
        String raw = " Mật khẩu học tập 123 ";
        String first = PasswordHelper.hash(raw);
        String second = PasswordHelper.hash(raw);
        assertNotEquals(first, second);
        assertTrue(PasswordHelper.verify(raw, first));
        assertFalse(PasswordHelper.verify(raw.trim(), first));
    }

    @Test public void malformedHashesAreRejected() {
        assertFalse(PasswordHelper.verify(null, ADMIN));
        assertFalse(PasswordHelper.verify("x", null));
        assertFalse(PasswordHelper.verify("x", "plain_password"));
        assertFalse(PasswordHelper.verify("x", ADMIN.replace("$600000$", "$999999999$")));
        assertFalse(PasswordHelper.verify("x", ADMIN.replace("$600000$", "$abc$")));
        assertFalse(PasswordHelper.verify("x", ADMIN.replace("81c63", "zzzzz")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankNewPassword() { PasswordHelper.hash("   "); }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTooLongNewPassword() { PasswordHelper.hash(new String(new char[257]).replace('\0', 'a')); }
}
