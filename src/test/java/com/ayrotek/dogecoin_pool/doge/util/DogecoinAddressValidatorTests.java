package com.ayrotek.dogecoin_pool.doge.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DogecoinAddressValidatorTests {

    @Test
    void rejectsBlank() {
        assertNotNull(DogecoinAddressValidator.validate(""));
        assertNotNull(DogecoinAddressValidator.validate("   "));
        assertNotNull(DogecoinAddressValidator.validate(null));
    }

    @Test
    void rejectsBitcoinBech32() {
        String err = DogecoinAddressValidator.validate("tb1HotAddressGoesHere");
        assertNotNull(err);
        String lower = err.toLowerCase();
        assertTrue(lower.contains("bech32")
                        || lower.contains("bc1")
                        || lower.contains("tb1")
                        || lower.contains("placeholder"));
    }

    @Test
    void acceptsBase58LookingAddresses() {
        // Not asserting that it is a real funded DOGE address; just that it passes basic sanity checks.
        assertNull(DogecoinAddressValidator.validate("D8B2r9yWm4z7b8y8t9R8xk7q7b1u8YpC3v"));
        assertNull(DogecoinAddressValidator.validate("A7JqQxw7m3qvZyYQ3zHq8m6q7p4ZyZyZyZ"));
    }
}
