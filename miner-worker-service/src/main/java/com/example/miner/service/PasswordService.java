package com.example.miner.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {

    private static final String CHAR_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int PASSWORD_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private final Argon2 argon2 = Argon2Factory.create(
        Argon2Factory.Argon2Types.ARGON2id,
        32,
        64
    );

    /**
     * Generate a random 16-character password
     */
    public String generateRandomPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(CHAR_SET.length());
            password.append(CHAR_SET.charAt(index));
        }
        return password.toString();
    }

    /**
     * Hash password using Argon2
     */
    public String hashPassword(String password) {
        try {
            char[] passwordChars = password.toCharArray();
            String hash = argon2.hash(10, 65536, 1, passwordChars);
            return hash;
        } finally {
            // Clear sensitive data
            argon2.wipeArray(password.toCharArray());
        }
    }

    /**
     * Verify password against Argon2 hash
     */
    public boolean verifyPassword(String hash, String password) {
        try {
            return argon2.verify(hash, password.toCharArray());
        } catch (Exception e) {
            return false;
        }
    }
}
