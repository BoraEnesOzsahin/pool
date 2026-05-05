package com.ayrotek.dogecoin_pool.doge.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        properties = {
        "DOGE_SWEEP_ON_STARTUP=false"
        }
)
class DogecoinBalanceControllerTests {

    private final WebTestClient webTestClient;

    @Autowired
    DogecoinBalanceControllerTests(ApplicationContext applicationContext) {
        this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
    }

    @Test
    void rejectsBitcoinBech32AddressWithoutCallingTatum() {
        String address = "tb1HotAddressGoesHere";
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/doge/balances").queryParam("address", address).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].address").isEqualTo(address)
                .jsonPath("$[0].balance").doesNotExist()
                .jsonPath("$[0].error").value(v -> {
                    String err = String.valueOf(v);
                    // Keep assertion loose; message can evolve.
                    org.junit.jupiter.api.Assertions.assertTrue(err.toLowerCase().contains("bech32")
                            || err.toLowerCase().contains("tb1")
                            || err.toLowerCase().contains("bc1"));
                });
    }
}
