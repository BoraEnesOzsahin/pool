package com.ayrotek.dogecoin_pool.doge.util;

import java.util.Locale;

public final class DogecoinAddressValidator {

    private DogecoinAddressValidator() {
    }

    /**
     * @return null if valid enough to attempt a Tatum call, otherwise an actionable error message
     */
    public static String validate(String address) {
        if (address == null || address.isBlank()) {
            return "Dogecoin address must not be null or blank";
        }

        String trimmed = address.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (lower.startsWith("bc1") || lower.startsWith("tb1")) {
            return "Address looks like a Bitcoin bech32 address (bc1/tb1). Dogecoin addresses are base58, not bech32.";
        }

        if (lower.contains("goeshere") || lower.contains("changeme") || lower.contains("youraddress")) {
            return "Address looks like a placeholder (set TATUM_DOGE_HOT_ADDRESS / TATUM_DOGE_COLD_ADDRESS)";
        }

        // Basic base58 sanity: avoid obvious typos and unsupported formats.
        // Dogecoin P2PKH/P2SH are base58; we keep this permissive to avoid false negatives.
        if (trimmed.length() < 20 || trimmed.length() > 60) {
            return "Address length looks invalid";
        }

        // Base58 alphabet: 1-9A-HJ-NP-Za-km-z
        if (!trimmed.matches("^[1-9A-HJ-NP-Za-km-z]+$")) {
            return "Address contains invalid characters for base58";
        }

        return null;
    }
}
