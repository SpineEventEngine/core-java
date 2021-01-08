/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.client;

import com.google.common.collect.ImmutableList;
import io.spine.base.EventMessage;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.client.tasks.CreateTask;
import io.spine.test.client.tasks.Task;
import io.spine.test.client.tasks.TaskCreated;
import io.spine.test.client.tasks.TaskId;

import java.util.List;

public class TaskAggregate extends Aggregate<TaskId, Task, Task.Builder> {

    /**
     * Handles a {@code CreateTask} command and emits a {@code TaskCreated} event.
     *
     * <p>The event is not declared in the signature on purpose. As for now, there is no reliable
     * way to tell which Bounded Context the {@code TaskCreated} event belongs to.
     */
    @Assign
    List<EventMessage> handle(CreateTask cmd) {
        return ImmutableList.of(
                TaskCreated
                        .newBuilder()
                        .setId(cmd.getId())
                        .setName(cmd.getName())
                        .setAuthor(cmd.getAuthor())
                        .vBuild()
        );
    }

    @Apply
    private void on(TaskCreated event) {
        builder().setName(event.getName())
                 .setAuthor(event.getAuthor());
    }
}
