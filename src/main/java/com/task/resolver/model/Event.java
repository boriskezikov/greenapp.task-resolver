package com.task.resolver.model;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public  class Event {

    public final String value;
    public final Long taskId;
    public final Long userId;
    public final Status status;
}
