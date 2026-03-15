package com.paulo.pricemonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PricemonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(PricemonitorApplication.class, args);
	}

}