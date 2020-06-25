package com.task.resolver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.resolver.logic.ObtainTaskOperation;
import com.task.resolver.logic.ObtainTaskOperation.ObtainTaskRequest;
import com.task.resolver.model.Event;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;

import static com.task.resolver.utils.Utils.logConsumeFlux;

@Component
@RequiredArgsConstructor
public class KafkaController {

    private static final Logger log = LoggerFactory.getLogger(KafkaController.class);

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObtainTaskOperation obtainTaskOperation;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationStartedEvent.class)
    public void consume() {
        kafkaReceiver.receive()
            .flatMap(e -> {
                try {
                    var event = objectMapper.readValue(e.value(), Event.class);
                    return Mono.just(new ObtainTaskRequest(event));
                } catch (JsonProcessingException jsonProcessingException) {
                    log.error("Cannot deserialize input {}", e);
                    return Mono.empty();
                }
            }).flatMap(obtainTaskOperation::process)
            .as(logConsumeFlux(log, "KafkaController"))
            .subscribe();
    }
}
