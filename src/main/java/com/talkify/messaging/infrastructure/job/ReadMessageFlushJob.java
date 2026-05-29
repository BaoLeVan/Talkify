package com.talkify.messaging.infrastructure.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.talkify.messaging.application.handler.FlushReadPositionHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadMessageFlushJob {
    private final FlushReadPositionHandler flushHandler;

    @Scheduled(fixedDelayString = "${app.jobs.flush-read-position.delay-ms:10000}")
    @SchedulerLock(name = "ReadMessageFlushJob", lockAtMostFor = "35s", lockAtLeastFor = "5s")
    public void flush() {
        flushHandler.flush();
    }
}
