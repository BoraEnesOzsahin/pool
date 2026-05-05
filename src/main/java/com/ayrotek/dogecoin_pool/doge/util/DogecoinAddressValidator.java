package com.ayrotek.dogecoin_pool.doge.util;

import java.util.Locale;

public final class DogecoinAddressValidator {

    private DogecoinAddressValidator() {
    }

    /**
        * @return null if valid enough to attempt an upstream lookup, otherwise an actionable error message
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
            return "Address looks like a placeholder (configure your HOT/COLD DOGE addresses)";
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

    /**
     * Stricter validation aligned with Tatum's DOGE transaction endpoint.
     * Tatum rejects addresses that are not 34 chars and not starting with D (mainnet) or n (testnet).
     *
     * @return null if valid for Tatum /v3/dogecoin/transaction, otherwise an actionable error message
     */
    public static String validateForTatumTransaction(String address) {
        String basic = validate(address);
        if (basic != null) {
            return basic;
        }

        String trimmed = address.trim();

        if (!(trimmed.startsWith("D") || trimmed.startsWith("n"))) {
            return "Tatum expects a DOGE P2PKH address starting with D (mainnet) or n (testnet)";
        }

        // Tatum error message: "must contain 33 characters after" => total length 34
        if (trimmed.length() != 34) {
            return "DOGE address must be 34 characters long (got " + trimmed.length() + ")";
        }

        return null;
    }
}
