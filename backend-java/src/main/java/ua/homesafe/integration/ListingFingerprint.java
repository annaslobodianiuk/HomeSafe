package ua.homesafe.integration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ListingFingerprint {
    private ListingFingerprint() {
    }

    public static String build(NormalizedListing listing) {
        int areaBucket = (int) Math.round((listing.area() == null ? 0d : listing.area()) / 3d) * 3;
        String identity = String.join("|",
            normalize(listing.city()),
            normalize(listing.district()),
            normalize(listing.address()),
            String.valueOf(listing.rooms() == null ? 0 : listing.rooms()),
            String.valueOf(areaBucket)
        );
        return sha256(identity);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
            .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
            .trim();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte next : bytes) {
                builder.append(String.format("%02x", next));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
