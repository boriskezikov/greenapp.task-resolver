package com.task.resolver.service.dao;

import com.task.resolver.logic.ObtainTaskOperation.ObtainTaskRequest;
import com.task.resolver.logic.VoteForTaskOperation.VoteForTaskRequest;
import com.task.resolver.logic.scheduled.CheckTaskScheduledOperation.FindByTasksForTrashingRequest;
import com.task.resolver.logic.scheduled.CheckTaskScheduledOperation.FindByTimeShiftAndCounterRequest;
import com.task.resolver.logic.scheduled.CheckTaskScheduledOperation.FindClientIdsForAccrualRequest;
import com.task.resolver.model.TaskEntry;
import io.r2dbc.client.Handle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import javax.annotation.Nullable;

import static java.lang.String.format;
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

    public Mono<Void> insertOnConflictUpdate(@Nullable Handle handle, ObtainTaskRequest request) {
        if (isNull(handle)) {
            return this.handler.withHandle(h -> insertOnConflictUpdate(h, request));
        }
        var sql = "INSERT INTO public.task(task_id, status) VALUES($1, $2::task_status) "
            + "ON CONFLICT ON CONSTRAINT unique_task_id_constr "
            + "DO UPDATE status = $2::task_status";
        return request.bindOnInsert(handle.createUpdate(sql))
            .execute()
            .then();
    }

    public Mono<Void> delete(@Nullable Handle handle, ObtainTaskRequest request) {
        if (isNull(handle)) {
            return this.handler.withHandle(h -> insertOnConflictUpdate(h, request));
        }
        var sql = "DELETE FROM public.task WHERE task_id = $1";
        return request.bindOnDelete(handle.createUpdate(sql))
            .execute()
            .then();
    }

    public Mono<Void> update(@Nullable Handle handle, ObtainTaskRequest request) {
        if (isNull(handle)) {
            return this.handler.withHandle(h -> insertOnConflictUpdate(h, request));
        }
        var sql = " public.task WHERE task_id = $1";
        return request.bindOnDelete(handle.createUpdate(sql))
            .execute()
            .then();
    }

    public Flux<TaskEntry> findTasksForTrashing(@Nullable Handle handle, FindByTasksForTrashingRequest request) {
        if (isNull(handle)) {
            return this.handler.withHandleFlux(h -> findTasksForTrashing(h, request));
        }
        var sql = format(
            "DELETE FROM public.task "
                + "WHERE (created * %d * interval '1 second' <= now() AND counter < $1) "
                + "OR (created * %d * interval '1 second' <= now() AND counter < $2) "
                + "RETURNING (task_id, CAST(status AS VARCHAR))",
            request.approvingShift, request.completingShift);
        return handle.createQuery(sql)
            .bind("$1", request.approvingCounter)
            .bind("$2", request.completingCounter)
            .mapRow(TaskEntry::fromGetByIdRow);
    }

    public Flux<TaskEntry> findTasksForApproving(@Nullable Handle handle, FindByTimeShiftAndCounterRequest request) {
        if (isNull(handle)) {
            return this.handler.withHandleFlux(h -> findTasksForApproving(h, request));
        }
        var sql = format(
            "DELETE FROM public.task "
                + "WHERE created * %d * interval '1 second' > now() AND counter >= $1 "
                + "RETURNING (task_id, CAST(status AS VARCHAR))", request.shift);
        return handle.createQuery(sql)
            .bind("$1", request.counter)
            .mapRow(TaskEntry::fromDeleteRow);
    }

    public Flux<Long> findClientIdsForAccrual(@Nullable Handle handle, FindClientIdsForAccrualRequest request) {
        if (isNull(handle)) {
            return this.handler.withHandleFlux(h -> findClientIdsForAccrual(h, request));
        }
        var sql = "DELETE FROM public.client WHERE task_id = $1 AND type = $2::vote_type "
            + "RETURNING client_id";
        return handle.createQuery(sql)
            .bind("$1", request.taskId)
            .bind("$2", request.type.toString())
            .mapRow(r -> r.get("client_id", Long.class));
    }

    public Mono<Void> deleteClients(@Nullable Handle handle, List<Long> taskId) {
        if (isNull(handle)) {
            return this.handler.withHandle(h -> deleteClients(h, taskId));
        }
        var sql = "DELETE FROM public.client WHERE task_id = $1";
        return handle.createUpdate(sql)
            .bind("$1", taskId)
            .execute()
            .then();
    }
}
