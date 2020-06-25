package com.task.resolver.service.dao;

import com.task.resolver.logic.ObtainTaskOperation.ObtainTaskRequest;
import com.task.resolver.logic.VoteForTaskOperation.VoteForTaskRequest;
import com.task.resolver.model.TaskEntry;
import io.r2dbc.client.Handle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
public class R2dbcAdapter {

    private final R2dbcHandler handler;

    public Mono<TaskEntry> findByTaskId(Long taskId) {
        return this.handler.withHandle(h -> {
            var sql = "SELECT task_id, CAST(status AS VARCHAR), counter, created FROM public.task WHERE task_id = $1";
            return h.createQuery(sql)
                .bind("$1", taskId)
                .mapRow(TaskEntry::fromGetByIdRow)
                .next();
        });
    }

    public Mono<Void> insertTaskVote(VoteForTaskRequest request) {
        return this.handler.withHandle(h -> {
            var sql = "UPDATE SET counter = counter + 1";
            return request.bindOnTask(h.createUpdate(sql))
                .execute()
                .then();
        });
    }

    public Mono<Void> insertClientVote(VoteForTaskRequest request) {
        return this.handler.withHandle(h -> {
            var sql = "INSERT INTO public.client(client_id, task_id, type) VALUES($1, $2, $3::vote_type)";
            return request.bindOnClient(h.createUpdate(sql))
                .execute()
                .then();
        });
    }

    public Mono<Void> insert(@Nullable Handle handle, ObtainTaskRequest request) {
        if (isNull(handle)) {
            return this.handler.withHandle(h -> insert(h, request));
        }
        var sql = "INSERT INTO public.task(task_id, status) VALUES($1, $2::task_status)";
        return request.bindOnInsert(handle.createUpdate(sql))
            .execute()
            .then();
    }

    public Mono<Void> delete(@Nullable Handle handle, ObtainTaskRequest request) {
        if (isNull(handle)) {
            return this.handler.withHandle(h -> insert(h, request));
        }
        var sql = "DELETE FROM public.task WHERE task_id = $1";
        return request.bindOnDelete(handle.createUpdate(sql))
            .execute()
            .then();
    }
}
