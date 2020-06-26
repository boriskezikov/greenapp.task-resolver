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

import static com.task.resolver.utils.Utils.logProcess;

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

    private static final Logger log = LoggerFactory.getLogger(CheckTaskScheduledOperation.class);

    private FindByTimeShiftAndCounterRequest approvingRequest;
    private FindByTimeShiftAndCounterRequest completingRequest;
    private FindByTasksForTrashingRequest trashingRequest;

    private final R2dbcAdapter r2dbcAdapter;
    private final RestAdapter restAdapter;
    private final R2dbcHandler handler;

    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        this.approvingRequest = new FindByTimeShiftAndCounterRequest(waitingForApproveStatusShift, waitingForApproveStatusCount);
        this.completingRequest = new FindByTimeShiftAndCounterRequest(resolvedStatusShift, resolvedStatusCount);
        this.trashingRequest = new FindByTasksForTrashingRequest(
            waitingForApproveStatusShift, waitingForApproveStatusCount,
            resolvedStatusShift, resolvedStatusCount);
    }

    @Override
    @SneakyThrows
    public void run() {
        handler.inTxMono(h -> {
            var readyForCompletingFlux = r2dbcAdapter.findTasksForApproving(h, approvingRequest)
                .cache();
            var readyForApprovingFlux = r2dbcAdapter.findTasksForApproving(h, completingRequest).cache();
            var readyForTrashingFlux = r2dbcAdapter.findTasksForTrashing(h, trashingRequest).cache();

            var approvingClientReward = readyForApprovingFlux
                .flatMap(t -> r2dbcAdapter.findClientIdsForAccrual(h, new FindClientIdsForAccrualRequest(t.task_id, Type.APPROVE)))
                .flatMap(c -> restAdapter.accrualMoney(new AccrualMoneyRequest(c, approveReward)));
            var completingClientReward = readyForCompletingFlux
                .flatMap(t -> r2dbcAdapter.findClientIdsForAccrual(h, new FindClientIdsForAccrualRequest(t.task_id, Type.APPROVE)))
                .flatMap(c -> restAdapter.accrualMoney(new AccrualMoneyRequest(c, completeReward)));
            var trashingClientReward = readyForTrashingFlux
                .flatMap(t -> r2dbcAdapter.findClientIdsForAccrual(h, new FindClientIdsForAccrualRequest(t.task_id, Type.REJECT)))
                .flatMap(c -> restAdapter.accrualMoney(new AccrualMoneyRequest(c, trashReward)));

            var statusToApprove = readyForApprovingFlux
                .flatMap(t -> restAdapter.changeTaskStatus(new ChangeTaskStatusRequest(Status.APPROVED, t.task_id)));
            var statusToComplete = readyForApprovingFlux
                .flatMap(t -> restAdapter.changeTaskStatus(new ChangeTaskStatusRequest(Status.COMPLETED, t.task_id)));
            var statusToTrashed = readyForApprovingFlux
                .flatMap(t -> restAdapter.changeTaskStatus(new ChangeTaskStatusRequest(Status.TRASHED, t.task_id)));

            var cleanUpClientEntries = readyForApprovingFlux
                .concatWith(readyForCompletingFlux)
                .concatWith(readyForTrashingFlux)
                .map(t -> t.task_id)
                .collectList()
                .flatMap(l -> r2dbcAdapter.deleteClients(h, l));

            return Mono.when(
                approvingClientReward, completingClientReward, trashingClientReward,
                statusToApprove, statusToComplete, statusToTrashed,
                cleanUpClientEntries);
        })
            .as(logProcess(log, "CheckTaskScheduledOperation"))
            .subscribe();
    }

    @RequiredArgsConstructor
    public static class FindByTimeShiftAndCounterRequest {

        public final Long shift;
        public final Long counter;
    }

    @RequiredArgsConstructor
    public static class FindByTasksForTrashingRequest {

        public final Long approvingShift;
        public final Long completingShift;
        public final Long approvingCounter;
        public final Long completingCounter;
    }

    @RequiredArgsConstructor
    public static class FindClientIdsForAccrualRequest {

        public final Long taskId;
        public final Type type;
    }
}
