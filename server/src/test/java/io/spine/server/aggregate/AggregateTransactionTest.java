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
package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.aggregate.given.Given;
import io.spine.server.command.Assign;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionTest;
import io.spine.server.type.EventEnvelope;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.validate.ConstraintViolation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AggregateTransaction should")
class AggregateTransactionTest
        extends TransactionTest<ProjectId,
                                Aggregate<ProjectId,
                                          Project,
                                          Project.Builder>,
                                Project,
                                Project.Builder> {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("aggregate-transaction-should-project")
                                                 .build();

    @Override
    protected Transaction<ProjectId,
            Aggregate<ProjectId, Project, Project.Builder>,
            Project,
            Project.Builder>
    createTx(Aggregate<ProjectId, Project, Project.Builder> entity) {
        return new AggregateTransaction<>(entity);
    }

    @Override
    protected Transaction<ProjectId,
                          Aggregate<ProjectId, Project, Project.Builder>,
                          Project,
                          Project.Builder>
    createTxWithState(Aggregate<ProjectId, Project, Project.Builder> entity,
                      Project state,
                      Version version) {
        return new AggregateTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<ProjectId,
                          Aggregate<ProjectId, Project, Project.Builder>,
                          Project,
                          Project.Builder>
    createTxWithListener(Aggregate<ProjectId, Project, Project.Builder> entity,
                         TransactionListener<ProjectId,
                                             Aggregate<ProjectId, Project, Project.Builder>,
                                             Project,
                                             Project.Builder> listener) {
        AggregateTransaction<ProjectId, Project, Project.Builder> transaction =
                new AggregateTransaction<>(entity);
        transaction.setListener(listener);
        return transaction;
    }

    @Override
    protected Aggregate<ProjectId, Project, Project.Builder> createEntity() {
        return new TestAggregate(ID);
    }

    @Override
    protected Aggregate<ProjectId, Project, Project.Builder> createEntity(
            ImmutableList<ConstraintViolation> violations) {
        return new TestAggregate(ID, violations);
    }

    @Override
    protected Project createNewState() {
        return Project.newBuilder()
                      .setId(ID)
                      .setName("The new project name to set in tx")
                      .build();
    }

    @Override
    protected void checkEventReceived(
            Aggregate<ProjectId, Project, Project.Builder> entity,
            Event event) {

        TestAggregate aggregate = (TestAggregate) entity;
        Message actualMessage = unpack(event.getMessage());
        assertTrue(aggregate.receivedEvents()
                            .contains(actualMessage));
    }

    @Override
    protected EventMessage createEventMessage() {
        return projectCreated(ID, "Project created in a transaction");
    }

    @Override
    protected EventMessage createEventMessageThatFailsInHandler() {
        return Given.EventMessage.taskAdded(ID);
    }

    @Override
    protected void applyEvent(Transaction tx, Event event) {
        AggregateTransaction cast = (AggregateTransaction) tx;
        EventEnvelope envelope = EventEnvelope.of(event);
        cast.play(envelope);
    }

    @Test
    @DisplayName("advance version from event")
    void eventFromVersion() {
        advanceVersionFromEvent();
    }

    @SuppressWarnings("unused")  // Methods accessed via reflection.
    static class TestAggregate
            extends Aggregate<ProjectId, Project, Project.Builder> {

        private final List<Message> receivedEvents = newArrayList();
        private final List<ConstraintViolation> violations;

        private TestAggregate(ProjectId id) {
            this(id, null);
        }

        private TestAggregate(ProjectId id, @Nullable List<ConstraintViolation> violations) {
            super(id);
            this.violations = violations;
        }

        @Override
        protected List<ConstraintViolation> checkEntityState(Project newState) {
            if (violations != null) {
                return ImmutableList.copyOf(violations);
            }
            return super.checkEntityState(newState);
        }

        @Assign
        AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
            return projectCreated(cmd.getProjectId(), cmd.getName());
        }

        @Apply
        private void event(AggProjectCreated event) {
            receivedEvents.add(event);
            Project newState = Project
                    .newBuilder(state())
                    .setId(event.getProjectId())
                    .setName(event.getName())
                    .build();
            builder().mergeFrom(newState);
        }

        @Apply
        @SuppressWarnings("MethodMayBeStatic")
        private void event(AggTaskAdded event) {
            throw new RuntimeException("that tests the tx behaviour");
        }

        private List<Message> receivedEvents() {
            return ImmutableList.copyOf(receivedEvents);
        }
    }
}
