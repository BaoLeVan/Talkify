package com.talkify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync        // non-blocking Kafka publish trong @TransactionalEventListener
@EnableScheduling   // kích hoạt @Scheduled jobs (ReadMessageFlushJob)
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = {
        "com.talkify.identity.infrastructure.persistence.repository",
        "com.talkify.messaging.infrastructure.persistence.repository"
    }
)
public class TalkifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalkifyApplication.class, args);
    }
}
