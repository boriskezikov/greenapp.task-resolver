package com.task.resolver.model;

import io.r2dbc.spi.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Builder
@ToString
@EqualsAndHashCode(exclude = {"created"})
@Getter
@AllArgsConstructor
public class TaskEntry {

    public final Long task_id;
    public final Status status;
    public final Long counter;
    public final LocalDateTime created;

    public static TaskEntry fromGetByIdRow(Row row) {
        return TaskEntry.builder()
            .task_id(row.get("task_id", Long.class))
            .status(Status.valueOf(row.get("status", String.class)))
            .counter(row.get("counter", Long.class))
            .created(row.get("created", LocalDateTime.class))
            .build();
    }

    public static TaskEntry fromDeleteRow(Row row) {
        return TaskEntry.builder()
            .task_id(row.get("task_id", Long.class))
            .status(Status.valueOf(row.get("status", String.class)))
            .build();
    }
}
