package com.ayrotek.pool_ser.tools;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

/**
 * DEV ONLY - DO NOT USE IN PRODUCTION.
 * Never commit the printed private keys to Git.
 */
public final class TestnetWalletGenerator {

    private TestnetWalletGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(String[] args) throws Exception {
        printWallet("HOT TEST WALLET (SEPOLIA)");
        System.out.println();
        printWallet("COLD TEST WALLET (SEPOLIA)");
    }

    private static void printWallet(String label) throws Exception {
        // DEV ONLY - DO NOT USE IN PRODUCTION
        final ECKeyPair keyPair = Keys.createEcKeyPair();
        final String privateKey = Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPrivateKey(), 64);
        final String address = "0x" + Keys.getAddress(keyPair);

        System.out.println("=== " + label + " ===");
        System.out.println("Private key: " + privateKey);
        System.out.println("Address:    " + address);
        System.out.println("(Never commit the values above to Git)");
    }
}
