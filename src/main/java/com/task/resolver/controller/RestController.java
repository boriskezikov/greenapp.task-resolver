package com.task.resolver.controller;

import com.task.resolver.logic.VoteForTaskOperation;
import com.task.resolver.logic.VoteForTaskOperation.VoteForTaskRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/task-resolver")
@RequiredArgsConstructor
public class RestController {

    private final static Logger log = LoggerFactory.getLogger(RestController.class);

    private final VoteForTaskOperation voteForTaskOperation;

    @PostMapping("/vote")
    public Mono<Void> voteForTask(@Valid @RequestBody VoteForTaskRequest request) {
        return Mono.just(request)
            .flatMap(voteForTaskOperation::process)
            .doOnSubscribe(s -> log.info("RestController.voteForTask.in request = {}", request))
            .doOnSuccess(s -> log.info("RestController.voteForTask.out"));
    }
}
