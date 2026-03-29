package com.boilerplate.springauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringauthApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringauthApplication.class, args);
	}

}
