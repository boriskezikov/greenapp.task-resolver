package com.task.resolver.service.rest;

import com.task.resolver.model.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RestAdapter {

    @Value("${rest.base-url.task-provider}")
    private String taskProviderUrl;
    @Value("${rest.base-url.reward-manager}")
    private String rewardManagerUrl;

    public Mono<Void> changeTaskStatus(ChangeTaskStatusRequest request) {
        return WebClient.create(taskProviderUrl)
            .patch()
            .uri("/task-provider/task/" + request.taskId.toString())
            .header("X-GREEN-APP-ID", "GREEN")
            .attribute("status", request.status.toString())
            .exchange()
            .then();
    }

    public Mono<Task> getTaskById(Long taskId) {
        return WebClient.create(taskProviderUrl)
            .get()
            .uri("/task-provider/task/" + taskId.toString())
            .header("X-GREEN-APP-ID", "GREEN")
            .exchange()
            .flatMap(r -> r.bodyToMono(Task.class));
    }

    public Mono<Void> accrualMoney(AccrualMoneyRequest request) {
        return WebClient.create(rewardManagerUrl)
            .patch()
            .uri("/task-provider/accrual")
            .header("X-GREEN-APP-ID", "GREEN")
            .header("X-GREEN-APP-INITIATOR", "TASK-RESOLVER")
            .bodyValue(request)
            .exchange()
            .then();
    }

    @RequiredArgsConstructor
    public static class ChangeTaskStatusRequest {

        public final Status status;
        public final Long taskId;

        public Mono<ChangeTaskStatusRequest> asMono() {
            return Mono.just(this);
        }
    }

    @RequiredArgsConstructor
    public static class AccrualMoneyRequest {

        public final Long clientId;
        public final Long amount;
        public final String initiator = "task-resolver";
    }

    @ToString
    @Getter
    @AllArgsConstructor
    public class Task {

        public final Long id;
        public final Long reward;
        public final Long assignee;
        public final Long createdBy;
    }
}
