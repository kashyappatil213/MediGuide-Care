package com.mediguide.util;

import com.mediguide.model.Role;

import java.security.SecureRandom;

/**
 * Utility for generating cryptographically secure, human-readable public IDs
 * for patients and doctors in the MediGuide Care system.
 *
 * Examples:
 * - Patient: PAT-7K4M92
 * - Doctor: DOC-3P8N61
 */
public final class PublicIdGenerator {

    private static final String ALPHANUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SUFFIX_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PublicIdGenerator() {
        // Utility class, no direct instantiation
    }

    /**
     * Resolves the uppercase role prefix: PAT- for PATIENT, DOC- for DOCTOR, ADM- for ADMIN.
     *
     * @param role user role
     * @return 4-character prefix including hyphen (e.g., "PAT-")
     */
    public static String getPrefix(Role role) {
        if (role == null) {
            return "USR-";
        }
        return switch (role) {
            case PATIENT -> "PAT-";
            case DOCTOR -> "DOC-";
            case ADMIN -> "ADM-";
        };
    }

    /**
     * Generates a random public ID with the role prefix and a 6-character secure alphanumeric suffix.
     *
     * @param role user role
     * @return formatted public ID (e.g., "PAT-7K4M92")
     */
    public static String generateRandomPublicId(Role role) {
        String prefix = getPrefix(role);
        char[] suffix = new char[SUFFIX_LENGTH];
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(ALPHANUMERIC_CHARS.length());
            suffix[i] = ALPHANUMERIC_CHARS.charAt(index);
        }
        return prefix + new String(suffix);
    }
}
