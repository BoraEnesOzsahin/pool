package com.ayrotek.pool_ser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PoolSerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PoolSerApplication.class, args);
	}

}
