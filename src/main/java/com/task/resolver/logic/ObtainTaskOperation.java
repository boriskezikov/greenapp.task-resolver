package com.task.resolver.logic;

import com.task.resolver.model.Event;
import com.task.resolver.model.Status;
import com.task.resolver.service.dao.R2dbcAdapter;
import com.task.resolver.service.dao.R2dbcHandler;
import com.task.resolver.service.rest.RestAdapter;
import com.task.resolver.service.rest.RestAdapter.ChangeTaskStatusRequest;
import io.r2dbc.client.Update;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.task.resolver.utils.Utils.logProcess;

@Component
@RequiredArgsConstructor
public class ObtainTaskOperation {

    private final static Logger log = LoggerFactory.getLogger(ObtainTaskOperation.class);

    private final R2dbcAdapter r2dbcAdapter;
    private final R2dbcHandler r2dbcHandler;
    private final RestAdapter restAdapter;

    public Mono<Void> process(ObtainTaskRequest request) {
        return r2dbcHandler.inTxMono(
            h -> {
                var updateStatusRequest = resolveUpdateStatusRequest(request.status, request.taskId);
                var insertTaskEntry = resolveInsert(request.status)
                    .flatMap(s -> r2dbcAdapter.insertOnConflictUpdate(h, new ObtainTaskRequest(request.taskId, s)));
                var updateStatus = updateStatusRequest
                    .flatMap(restAdapter::changeTaskStatus);
                return Mono.when(insertTaskEntry, updateStatus);
            }
        ).as(logProcess(log, "ObtainTaskOperation", request));
    }

    private Mono<ChangeTaskStatusRequest> resolveUpdateStatusRequest(Status status, Long taskId) {
        return switch (status) {
            case CREATED -> new ChangeTaskStatusRequest(Status.WAITING_FOR_APPROVE, taskId).asMono();
            case APPROVED -> new ChangeTaskStatusRequest(Status.TO_DO, taskId).asMono();
            default -> Mono.empty();
        };
    }

    private Mono<Status> resolveInsert(Status status) {
        return switch (status) {
            case CREATED -> Status.WAITING_FOR_APPROVE.asMono();
            case RESOLVED -> Status.RESOLVED.asMono();
            default -> Mono.empty();
        };
    }

    @ToString
    @RequiredArgsConstructor
    public static class ObtainTaskRequest {

        public final Long taskId;
        public final Status status;

        public ObtainTaskRequest(Event event) {
            this.taskId = event.taskId;
            this.status = event.status;
        }

        public Update bindOnDelete(Update query) {
            return query
                .bind("$1", taskId);
        }

        public Update bindOnInsert(Update query) {
            return query
                .bind("$1", taskId)
                .bind("$2", status.toString());
        }
    }
}
