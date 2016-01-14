/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.examples.failure;

import org.spine3.base.CommandContext;
import org.spine3.examples.failure.commands.CancelTask;
import org.spine3.examples.failure.events.TaskCancelled;
import org.spine3.examples.failure.failures.CannotCancelTaskInProgress;
import org.spine3.server.Assign;
import org.spine3.server.aggregate.Aggregate;

/**
 * A sample of the aggregate that throws a business failure on a command, which cannot be
 * performed.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("unused")
public class TaskAggregate extends Aggregate<TaskId, Task> {

    public TaskAggregate(TaskId id) {
        super(id);
    }

    @Override
    protected Task getDefaultState() {
        return Task.getDefaultInstance();
    }

    @Assign
    public TaskCancelled handle(CancelTask command, CommandContext context) throws CannotCancelTaskInProgress {
        final Task task = getState();
        if (task.getStatus() == Task.Status.IN_PROGRESS) {
            throw new CannotCancelTaskInProgress(task.getId());
        }

        return TaskCancelled.newBuilder().setId(task.getId()).build();
    }
}
