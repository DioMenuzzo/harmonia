package com.hospital.harmonia.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void verify_returnsTrueForTheCorrectPassword() {
        String hash = PasswordUtil.hash("Sup3rSecret!");
        assertTrue(PasswordUtil.verify("Sup3rSecret!", hash));
    }

    @Test
    void verify_returnsFalseForTheWrongPassword() {
        String hash = PasswordUtil.hash("Sup3rSecret!");
        assertFalse(PasswordUtil.verify("wrong-password", hash));
    }

    @Test
    void hash_neverStoresThePasswordInPlainText() {
        String password = "Sup3rSecret!";
        String hash = PasswordUtil.hash(password);
        assertNotEquals(password, hash);
    }

    @Test
    void hash_producesADifferentSaltEveryTime() {
        // BCrypt embeds a random salt in every hash, so hashing the same
        // password twice must never produce the same string -- this is what
        // protects against rainbow-table attacks even if two users share a password.
        String password = "Sup3rSecret!";
        assertNotEquals(PasswordUtil.hash(password), PasswordUtil.hash(password));
    }

    @Test
    void verify_returnsFalseInsteadOfThrowingOnANullPassword() {
        assertFalse(PasswordUtil.verify(null, PasswordUtil.hash("anything")));
    }

    @Test
    void verify_returnsFalseInsteadOfThrowingOnANullOrCorruptedHash() {
        assertFalse(PasswordUtil.verify("anything", null));
        assertFalse(PasswordUtil.verify("anything", "not-a-valid-bcrypt-hash"));
    }
}
