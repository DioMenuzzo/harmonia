package com.hospital.harmonia.util;

import org.mindrot.jbcrypt.BCrypt;

/** Utility for hashing and securely verifying passwords (never store a password in plain text). */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // invalid/corrupted hash
            return false;
        }
    }
}
