package com.ayrotek.dogecoin_pool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DogecoinPoolApplication {

	public static void main(String[] args) {
		SpringApplication.run(DogecoinPoolApplication.class, args);
	}

}
