package com.task.resolver.utils;

import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class Utils {

    public static <T> Function<Mono<T>, Mono<T>> logProcess(Logger log, String point, Object request) {
        return mono -> Mono.deferWithContext((ctx) -> {
            log.info("{}.process.in request = {}", point, request);
            return mono
                .doOnSuccess(n -> log.info("{}.process.out", point))
                .doOnError(e -> log.warn("{}.thrown {}", point, e));
        });
    }

    public static <T> Function<Flux<T>, Flux<T>> logConsumeFlux(Logger log, String point, Object... request) {
        return flux -> Flux.deferWithContext((ctx) -> {
            log.info("{}.consume.in request = {}", point, request);
            return flux
                .doOnComplete(() -> log.info("{}.consume.out", point))
                .doOnError(e -> log.warn("{}.consume.thrown {}", point, e));
        });
    }
}
