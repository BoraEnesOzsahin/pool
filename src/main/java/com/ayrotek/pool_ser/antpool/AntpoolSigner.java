package com.ayrotek.pool_ser.antpool;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AntpoolSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private AntpoolSigner() {
    }

    public static String sign(String apiKey, String apiSecret, String nonce) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] signature = mac.doFinal((apiKey + nonce).getBytes(StandardCharsets.UTF_8));
            return toHex(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign AntPool request", ex);
        }
    }

    private static String toHex(byte[] input) {
        StringBuilder builder = new StringBuilder(input.length * 2);
        for (byte b : input) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
