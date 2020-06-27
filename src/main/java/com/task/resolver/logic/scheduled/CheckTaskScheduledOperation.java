package com.task.resolver.logic.scheduled;

import com.task.resolver.logic.VoteForTaskOperation.VoteForTaskRequest.Type;
import com.task.resolver.model.Status;
import com.task.resolver.service.dao.R2dbcAdapter;
import com.task.resolver.service.dao.R2dbcHandler;
import com.task.resolver.service.rest.RestAdapter;
import com.task.resolver.service.rest.RestAdapter.AccrualMoneyRequest;
import com.task.resolver.service.rest.RestAdapter.ChangeTaskStatusRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CheckTaskScheduledOperation extends Thread {

    @Value("${task.processing.waiting-for-approve-m}")
    private Long waitingForApproveStatusShift;
    @Value("${task.processing.resolved-m}")
    private Long resolvedStatusShift;
    @Value("${task.processing.waiting-for-approve-count}")
    private Long waitingForApproveStatusCount;
    @Value("${task.processing.resolved-count}")
    private Long resolvedStatusCount;
    @Value("${client.reward.approve-attendee}")
    private Long approveReward;
    @Value("${client.reward.complete-attendee}")
    private Long completeReward;
    @Value("${client.reward.trash-attendee}")
    private Long trashReward;
    @Value("${client.reward.creator}")
    private Long creatorReward;

    private static final Logger log = LoggerFactory.getLogger(CheckTaskScheduledOperation.class);

    private FindByTimeShiftAndCounterRequest approvingRequest;
    private FindByTimeShiftAndCounterRequest completingRequest;
    private FindByTasksForTrashingRequest trashingRequest;

    private final R2dbcAdapter r2dbcAdapter;
    private final RestAdapter restAdapter;
    private final R2dbcHandler handler;

    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        this.approvingRequest =
            new FindByTimeShiftAndCounterRequest(waitingForApproveStatusShift, waitingForApproveStatusCount, Status.WAITING_FOR_APPROVE);
        this.completingRequest = new FindByTimeShiftAndCounterRequest(resolvedStatusShift, resolvedStatusCount, Status.RESOLVED);
        this.trashingRequest = new FindByTasksForTrashingRequest(waitingForApproveStatusShift, resolvedStatusShift);
    }

    @Override
    @SneakyThrows
    public void run() {
        log.info("CheckTaskScheduledOperation.run.in");
        handler.inTxMono(h -> {
            var readyForCompletingFlux = r2dbcAdapter.findTasksForApproving(h, completingRequest).cache();

            readyForCompletingFlux
                .flatMap(t -> r2dbcAdapter.findClientIdsForAccrual(h, new FindClientIdsForAccrualRequest(t.task_id, Type.APPROVE)))
                .flatMap(c -> restAdapter.accrualMoney(new AccrualMoneyRequest(c, completeReward))).subscribe();

            readyForCompletingFlux
                .flatMap(t -> restAdapter.changeTaskStatus(new ChangeTaskStatusRequest(Status.COMPLETED, t.task_id)))
                .subscribe();

            readyForCompletingFlux
                .flatMap(t -> r2dbcAdapter.deleteClients(h, t.task_id))
                .subscribe();

            readyForCompletingFlux
                .flatMap(t -> restAdapter.getTaskById(t.task_id))
                .flatMap(t -> Mono.when(
                    restAdapter.accrualMoney(new AccrualMoneyRequest(t.getAssignee(), t.getReward())),
                    restAdapter.accrualMoney(new AccrualMoneyRequest(t.getCreatedBy(), creatorReward))
                )).subscribe();

            return Mono.just(1);
        }).block();

        handler.inTxMono(h -> {
            var readyForApprovingFlux = r2dbcAdapter.findTasksForApproving(h, approvingRequest).cache();

            readyForApprovingFlux
                .flatMap(t -> r2dbcAdapter.findClientIdsForAccrual(h, new FindClientIdsForAccrualRequest(t.task_id, Type.APPROVE)))
                .flatMap(c -> restAdapter.accrualMoney(new AccrualMoneyRequest(c, approveReward)))
                .subscribe();

            readyForApprovingFlux
                .flatMap(t -> restAdapter.changeTaskStatus(new ChangeTaskStatusRequest(Status.TO_DO, t.task_id)))
                .subscribe();

            readyForApprovingFlux
                .flatMap(t -> r2dbcAdapter.deleteClients(h, t.task_id))
                .subscribe();

            return Mono.just(1);
        }).block();

        handler.inTxMono(h -> {
            var readyForTrashingFlux = r2dbcAdapter.findTasksForTrashing(h, trashingRequest).cache();

            readyForTrashingFlux
                .flatMap(t -> r2dbcAdapter.findClientIdsForAccrual(h, new FindClientIdsForAccrualRequest(t.task_id, Type.REJECT)))
                .flatMap(c -> restAdapter.accrualMoney(new AccrualMoneyRequest(c, trashReward))).subscribe();

            readyForTrashingFlux
                .flatMap(t -> restAdapter.changeTaskStatus(new ChangeTaskStatusRequest(Status.TRASHED, t.task_id))).subscribe();

            readyForTrashingFlux
                .flatMap(t -> r2dbcAdapter.deleteClients(h, t.task_id)).subscribe();

            return Mono.just(1);
        }).block();
        log.info("CheckTaskScheduledOperation.run.out");
    }

    @RequiredArgsConstructor
    public static class FindByTimeShiftAndCounterRequest {

        public final Long shift;
        public final Long counter;
        public final Status status;
    }

    @RequiredArgsConstructor
    public static class FindByTasksForTrashingRequest {

        public final Long approvingShift;
        public final Long completingShift;
    }

    @RequiredArgsConstructor
    public static class FindClientIdsForAccrualRequest {

        public final Long taskId;
        public final Type type;
    }
}
