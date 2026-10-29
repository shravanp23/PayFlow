package com.payflow.common;

import java.security.SecureRandom;

/** Generates human-readable, prefixed, non-guessable IDs, e.g. PAY_8F29A1C4. */
public final class IdGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {}

    public static String generate(String prefix) {
        return generate(prefix, 8);
    }

    public static String generate(String prefix, int length) {
        StringBuilder sb = new StringBuilder(prefix).append("_");
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
