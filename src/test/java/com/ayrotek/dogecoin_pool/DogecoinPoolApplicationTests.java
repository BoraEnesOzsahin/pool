package com.ayrotek.dogecoin_pool;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"DOGE_SWEEP_ON_STARTUP=false",
		"DOGE_SWEEP_PERIODIC_ENABLED=false"
})
class DogecoinPoolApplicationTests {

	@Test
	void contextLoads() {
	}

}
