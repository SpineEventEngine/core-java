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

package io.spine.server.aggregate.given.part;

import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.TaskVBuilder;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.event.AggTaskAdded;

/**
 * An aggregate part with {@link Task} state, which belongs to the aggregate
 * represented by {@link AnAggregateRoot}.
 *
 * @author Alexander Yevsyukov
 */
public class TaskPart
        extends AggregatePart<String, Task, TaskVBuilder, AnAggregateRoot> {

    private static final String TASK_DESCRIPTION = "Description";

    public TaskPart(AnAggregateRoot root) {
        super(root);
    }

    @Assign
    AggTaskAdded handle(AggAddTask msg) {
        AggTaskAdded result = AggTaskAdded.newBuilder()
                                          .build();
        //This command can be empty since we use apply method to setup aggregate part.
        return result;
    }

    @Apply
    void apply(AggTaskAdded event) {
        builder().setDescription(TASK_DESCRIPTION);
    }
}
