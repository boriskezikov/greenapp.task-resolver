package com.task.resolver.logic;

import com.task.resolver.service.dao.R2dbcAdapter;
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
public class VoteForTaskOperation {

    private final static Logger log = LoggerFactory.getLogger(VoteForTaskOperation.class);

    private final R2dbcAdapter r2dbcAdapter;

    public Mono<Void> process(VoteForTaskRequest request) {
        return r2dbcAdapter.insertTaskVote(request)
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
                .bind("$1", this.taskId)
                .bind("$2", this.clientId)
                .bind("$3", this.type.toString());
        }

        public Update bindOnTask(Update query) {
            return query
                .bind("$1", this.taskId)
                .bind("$2", this.clientId);
        }

        public enum Type {
            APPROVE,
            REJECT;
        }
    }
}
