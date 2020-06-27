package com.task.resolver.logic;

import com.task.resolver.logic.VoteForTaskOperation.VoteForTaskRequest.Type;
import com.task.resolver.service.dao.R2dbcAdapter;
import io.r2dbc.client.Update;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.task.resolver.exception.ApplicationError.ENTRY_NOT_FOUND_BY_TASK_ID;
import static com.task.resolver.utils.Utils.logProcess;
import static java.lang.String.format;

@Component
@RequiredArgsConstructor
public class VoteForTaskOperation {

    private final static Logger log = LoggerFactory.getLogger(VoteForTaskOperation.class);

    private final R2dbcAdapter r2dbcAdapter;

    public Mono<Void> process(VoteForTaskRequest request) {
        return r2dbcAdapter.findByTaskId(request.taskId)
            .switchIfEmpty(ENTRY_NOT_FOUND_BY_TASK_ID.exceptionMono(format("Entry with task id = %s not found", request.taskId)))
            .then(Mono.just(request.type)
                      .flatMap(t -> t.equals(Type.APPROVE) ?
                                    r2dbcAdapter.insertTaskVotePlus(request) :
                                    r2dbcAdapter.insertTaskVoteMinus(request)
                      ))
            .then(r2dbcAdapter.insertClientVote(request))
            .as(logProcess(log, "VoteForTaskOperation", request));
    }

    @ToString
    @RequiredArgsConstructor
    public static class VoteForTaskRequest {

        public final Long taskId;
        public final Long clientId;
        public final Type type;

        public Update bindOnClient(Update query) {
            return query
                .bind("$1", this.clientId)
                .bind("$2", this.taskId)
                .bind("$3", this.type.toString());
        }

        public enum Type {
            APPROVE,
            REJECT;
        }
    }
}
