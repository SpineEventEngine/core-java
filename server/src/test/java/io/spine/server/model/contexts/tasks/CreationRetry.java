/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.model.contexts.tasks;

import io.spine.base.Identifier;
import io.spine.core.CommandContext;
import io.spine.server.command.AbstractCommander;
import io.spine.server.command.Command;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.test.model.contexts.tasks.TaskId;
import io.spine.test.model.contexts.tasks.commands.CreateTask;
import io.spine.test.model.contexts.tasks.rejections.TaskRejections;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * A {@link io.spine.server.command.Commander Commander} which retries the task creation if it
 * fails.
 *
 * @author Dmytro Dashenkov
 */
public final class CreationRetry extends AbstractCommander {

    private static final Set<TaskId> rejectedTasks = newHashSet();

    CreationRetry(CommandBus commandBus, EventBus eventBus) {
        super(commandBus, eventBus);
    }

    @Command
    CreateTask on(TaskRejections.TaskAlreadyExists rejection, CommandContext commandContext) {
        TaskId id = Identifier.generate(TaskId.class);
        rejectedTasks.add(rejection.getId());
        return CreateTask
                .newBuilder()
                .setId(id)
                .setName(rejection.getName())
                .setDescription(rejection.getDescription())
                .build();
    }

    public static boolean retried(TaskId id) {
        return rejectedTasks.contains(id);
    }
}
