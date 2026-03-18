package com.umc.linkyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinkyouApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkyouApplication.class, args);
	}

}
