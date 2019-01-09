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

package io.spine.server.aggregate.given.repo;

import io.spine.base.CommandMessage;
import io.spine.core.CommandEnvelope;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.testdata.Sample;
import io.spine.testing.server.entity.given.Given;

import static io.spine.server.aggregate.given.repo.AggregateRepositoryTestEnv.requestFactory;
import static io.spine.testdata.Sample.builderForType;
import static io.spine.testing.server.aggregate.AggregateMessageDispatcher.dispatchCommand;

/** Utility factory for test aggregates. */
public class GivenAggregate {

    /** Prevents instantiation of this utility class. */
    private GivenAggregate() {
    }

    public static ProjectAggregate withUncommittedEvents() {
        return withUncommittedEvents(Sample.messageOfType(ProjectId.class));
    }

    public static ProjectAggregate withUncommittedEvents(ProjectId id) {
        ProjectAggregate aggregate = Given.aggregateOfClass(ProjectAggregate.class)
                                          .withId(id)
                                          .build();

        AggCreateProject.Builder createProject = builderForType(AggCreateProject.class);
        createProject.setProjectId(id);
        AggAddTask.Builder addTask = builderForType(AggAddTask.class);
        addTask.setProjectId(id);
        AggStartProject.Builder startProject = builderForType(AggStartProject.class);
        startProject.setProjectId(id);

        dispatchCommand(aggregate, env(createProject.build()));
        dispatchCommand(aggregate, env(addTask.build()));
        dispatchCommand(aggregate, env(startProject.build()));

        return aggregate;
    }

    /** Generates a command for the passed message and wraps it into the envelope. */
    private static CommandEnvelope env(CommandMessage commandMessage) {
        return CommandEnvelope.of(requestFactory().command()
                                                  .create(commandMessage));
    }
}
