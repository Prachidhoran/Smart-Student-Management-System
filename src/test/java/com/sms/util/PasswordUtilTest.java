package com.sms.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PasswordUtilTest {
    @Test void hashesAreSaltedAndVerifyOnlyTheCorrectPassword() {
        String first = PasswordUtil.hash("correct horse battery staple".toCharArray());
        String second = PasswordUtil.hash("correct horse battery staple".toCharArray());
        assertNotEquals(first, second);
        assertTrue(PasswordUtil.matches("correct horse battery staple".toCharArray(), first));
        assertFalse(PasswordUtil.matches("wrong password".toCharArray(), first));
    }
}
