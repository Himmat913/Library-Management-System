package com.ucs553.library.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void correctPasswordVerifies() {
        String stored = PasswordHasher.hash("secret1".toCharArray());
        assertTrue(PasswordHasher.verify("secret1".toCharArray(), stored));
        assertFalse(PasswordHasher.verify("secret2".toCharArray(), stored));
    }

    @Test
    void samePasswordGetsDifferentHashes() {
        assertNotEquals(PasswordHasher.hash("secret1".toCharArray()), PasswordHasher.hash("secret1".toCharArray()));
    }

    @Test
    void damagedStoredValueIsRejected() {
        assertFalse(PasswordHasher.verify("secret1".toCharArray(), "garbage"));
        assertFalse(PasswordHasher.verify("secret1".toCharArray(), "x:y:z"));
    }
}
