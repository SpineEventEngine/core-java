/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.aggregate.given;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.command.AggCancelTask;
import io.spine.test.aggregate.command.AggCompleteTask;
import io.spine.test.aggregate.command.AggCreateTask;
import io.spine.test.aggregate.event.AggTaskCanceled;
import io.spine.test.aggregate.event.AggTaskCompleted;
import io.spine.test.aggregate.event.AggTaskCreated;
import io.spine.test.aggregate.task.AggTask;
import io.spine.test.aggregate.task.AggTaskId;
import io.spine.test.aggregate.task.AggTaskVBuilder;

/**
 * Test environment for {@link io.spine.server.aggregate.AggregateRepositoryTest}.
 *
 * @author Alexander Yevsyukov
 */
public class AggregateRepositoryViewTestEnv {

    private AggregateRepositoryViewTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /**
     * The aggregate that can handle status flags.
     *
     * <p>We use {@code StringValue} for messages to save on code generation
     * in the tests. Real aggregates should use generated messages.
     */
    public static class AggregateWithLifecycle
            extends Aggregate<AggTaskId, AggTask, AggTaskVBuilder> {

        private AggregateWithLifecycle(AggTaskId id) {
            super(id);
        }

        @Assign
        AggTaskCreated handle(AggCreateTask command) {
            return AggTaskCreated
                    .newBuilder()
                    .setTaskId(command.getTaskId())
                    .build();
        }

        @Assign
        AggTaskCompleted handle(AggCompleteTask command) {
            return AggTaskCompleted
                    .newBuilder()
                    .setTaskId(command.getTaskId())
                    .build();
        }

        @Assign
        AggTaskCanceled handle(AggCancelTask command) {
            return AggTaskCanceled
                    .newBuilder()
                    .setTaskId(command.getTaskId())
                    .build();
        }

        @Apply
        void on(AggTaskCreated event) {
            getBuilder().setId(event.getTaskId());
        }

        @Apply
        void on(@SuppressWarnings("unused") // OK for an applier.
                AggTaskCompleted event) {
            setArchived(true);
        }


        @Apply
        void on(@SuppressWarnings("unused") // OK for an applier.
                AggTaskCanceled event) {
            setDeleted(true);
        }
    }

    /**
     * The aggregate repository under tests.
     *
     * <p>The repository accepts commands as {@code StringValue}s in the form:
     * {@code AggregateId-CommandMessage}.
     */
    public static class RepoOfAggregateWithLifecycle
            extends AggregateRepository<AggTaskId, AggregateWithLifecycle> {
    }
}
