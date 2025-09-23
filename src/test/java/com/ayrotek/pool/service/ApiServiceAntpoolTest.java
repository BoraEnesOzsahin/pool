package com.ayrotek.pool.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ApiServiceAntpoolTest {

    @Autowired
    private ApiService apiService;

    @Test
    void testGetAntpoolPoolStats() {
        String result = apiService.getAntpoolPoolStats("BTC");
        assertNotNull(result);
        System.out.println(result);
    }
}
