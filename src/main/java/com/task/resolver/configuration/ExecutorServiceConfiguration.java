package com.task.resolver.configuration;

import com.task.resolver.logic.scheduled.CheckTaskScheduledOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class ExecutorServiceConfiguration {

    @Bean
    public ExecutorService executorService(CheckTaskScheduledOperation scheduledOperation) {
        var scheduler = Executors.newScheduledThreadPool(3);
        scheduler.scheduleWithFixedDelay(scheduledOperation, 10, 60, TimeUnit.SECONDS);
        return scheduler;
    }
}
