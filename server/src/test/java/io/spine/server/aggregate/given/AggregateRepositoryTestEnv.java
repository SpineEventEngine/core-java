/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.CommandEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.AggregateRepositoryShould;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.given.Given;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AddTask;
import io.spine.test.aggregate.command.CreateProject;
import io.spine.test.aggregate.command.StartProject;
import io.spine.test.aggregate.event.ProjectCreated;
import io.spine.test.aggregate.event.ProjectStarted;
import io.spine.test.aggregate.event.TaskAdded;
import io.spine.testdata.Sample;

import static io.spine.server.aggregate.AggregateCommandDispatcher.dispatch;

/**
 * @author Alexander Yevsyukov
 */
public class AggregateRepositoryTestEnv {

    private static final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(AggregateRepositoryShould.class);

    private AggregateRepositoryTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /** Generates a command for the passed message and wraps it into the envelope. */
    private static CommandEnvelope env(Message commandMessage) {
        return CommandEnvelope.of(factory.command().create(commandMessage));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static class GivenAggregate {

        private GivenAggregate() {
            // Prevent instantiation of this utility class.
        }

        public static ProjectAggregate withUncommittedEvents() {
            return withUncommittedEvents(Sample.messageOfType(ProjectId.class));
        }

        public static ProjectAggregate withUncommittedEvents(ProjectId id) {
            final ProjectAggregate aggregate = Given.aggregateOfClass(ProjectAggregate.class)
                                                    .withId(id)
                                                    .build();

            final CreateProject createProject =
                    ((CreateProject.Builder) Sample.builderForType(CreateProject.class))
                            .setProjectId(id)
                            .build();
            final AddTask addTask =
                    ((AddTask.Builder) Sample.builderForType(AddTask.class))
                            .setProjectId(id)
                            .build();
            final StartProject startProject =
                    ((StartProject.Builder) Sample.builderForType(StartProject.class))
                            .setProjectId(id)
                            .build();

            dispatch(aggregate, env(createProject));
            dispatch(aggregate, env(addTask));
            dispatch(aggregate, env(startProject));

            return aggregate;
        }
    }

    @SuppressWarnings({"TypeMayBeWeakened", "ResultOfMethodCallIgnored"})
    public static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Override
        public int uncommittedEventsCount() {
            return super.uncommittedEventsCount();
        }

        @Assign
        ProjectCreated handle(CreateProject msg) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .setName(msg.getName())
                                 .build();
        }

        @Apply
        private void apply(ProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(event.getName());
        }

        @Assign
        TaskAdded handle(AddTask msg) {
            return TaskAdded.newBuilder()
                            .setProjectId(msg.getProjectId())
                            .setTask(msg.getTask())
                            .build();
        }

        @Apply
        private void apply(TaskAdded event) {
            getBuilder().setId(event.getProjectId())
                        .addTask(event.getTask());
        }

        @Assign
        ProjectStarted handle(StartProject msg) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .build();
        }

        @Apply
        private void apply(ProjectStarted event) {
            getBuilder().setStatus(Status.STARTED);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Overrides to open the method to the test suite.
         */
        @Override
        @VisibleForTesting
        public void setArchived(boolean archived) {
            super.setArchived(archived);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Overrides to open the method to the test suite.
         */
        @Override
        @VisibleForTesting
        public void setDeleted(boolean deleted) {
            super.setDeleted(deleted);
        }
    }

    public static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {

        public static final ProjectId troublesome = ProjectId.newBuilder()
                                                              .setId("INVALID_ID")
                                                              .build();
        @Override
        public Optional<ProjectAggregate> find(ProjectId id) {
            if (id.equals(troublesome)) {
                return Optional.absent();
            }
            return super.find(id);
        }
    }
}
