package com.manuhd.app.creditos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CreditosApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreditosApplication.class, args);
	}

}
