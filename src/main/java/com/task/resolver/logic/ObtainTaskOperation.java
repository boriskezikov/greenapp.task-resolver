package com.task.resolver.logic;

import com.task.resolver.model.Event;
import com.task.resolver.model.Status;
import com.task.resolver.service.dao.R2dbcAdapter;
import com.task.resolver.service.dao.R2dbcHandler;
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

    public Mono<Void> process(ObtainTaskRequest request) {
        return r2dbcHandler.inTxMono(
            h -> {
                var oldTaskEntry = r2dbcAdapter.findByTaskId(request.taskId);
                var updateOldTaskEntry = oldTaskEntry
                    .flatMap(t -> r2dbcAdapter.delete(h, request));
                var insertTaskEntry = r2dbcAdapter.insert(h, request);

                return Mono.when(oldTaskEntry, updateOldTaskEntry, insertTaskEntry);
            }
        ).as(logProcess(log, "ObtainTaskOperation", request));
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
