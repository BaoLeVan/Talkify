package com.talkify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TalkifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(TalkifyApplication.class, args);
	}

}
