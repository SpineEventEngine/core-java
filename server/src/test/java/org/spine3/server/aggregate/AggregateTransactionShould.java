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
package org.spine3.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.Version;
import org.spine3.server.command.Assign;
import org.spine3.server.entity.ThrowingValidatingBuilder;
import org.spine3.server.entity.Transaction;
import org.spine3.server.entity.TransactionListener;
import org.spine3.server.entity.TransactionShould;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.validate.ConstraintViolation;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.server.aggregate.Given.EventMessage.projectCreated;

/**
 * @author Alex Tymchenko
 */
public class AggregateTransactionShould
        extends TransactionShould<ProjectId,
        Aggregate<ProjectId, Project, AggregateTransactionShould.PatchedProjectBuilder>,
        Project,
        AggregateTransactionShould.PatchedProjectBuilder> {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("aggregate-transaction-should-project")
                                                 .build();

    @Override
    protected Transaction<ProjectId,
            Aggregate<ProjectId, Project, PatchedProjectBuilder>,
            Project,
            PatchedProjectBuilder>
    createTx(Aggregate<ProjectId, Project, PatchedProjectBuilder> entity) {
        return new AggregateTransaction<>(entity);
    }

    @Override
    protected Transaction<ProjectId,
                          Aggregate<ProjectId, Project, PatchedProjectBuilder>,
                          Project,
                          PatchedProjectBuilder> createTxWithState(
            Aggregate<ProjectId, Project, PatchedProjectBuilder> entity, Project state,
            Version version) {
        return new AggregateTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<ProjectId,
                          Aggregate<ProjectId, Project, PatchedProjectBuilder>,
                          Project,
                          PatchedProjectBuilder>
    createTxWithListener(Aggregate<ProjectId, Project, PatchedProjectBuilder> entity,
                         TransactionListener<ProjectId,
                                             Aggregate<ProjectId,
                                                       Project,
                                                     PatchedProjectBuilder>,
                                             Project, PatchedProjectBuilder> listener) {
        return new AggregateTransaction<>(entity, listener);
    }

    @Override
    protected Aggregate<ProjectId, Project, PatchedProjectBuilder> createEntity() {
        return new TestAggregate(ID);
    }

    @Override
    protected Aggregate<ProjectId, Project, PatchedProjectBuilder> createEntity(
            List<ConstraintViolation> violations) {
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
            Aggregate<ProjectId, Project, PatchedProjectBuilder> entity,
            Event event) {

        final TestAggregate aggregate = (TestAggregate) entity;
        final Message actualMessage = unpack(event.getMessage());
        assertTrue(aggregate.getReceivedEvents()
                            .contains(actualMessage));
    }

    @Override
    protected Message createEventMessage() {
        return projectCreated(ID, "Project created in a transaction");
    }

    @Override
    protected Message createEventMessageThatFailsInHandler() {
        return Given.EventMessage.taskAdded(ID);
    }

    @Override
    protected void breakEntityValidation(
            Aggregate<ProjectId, Project, PatchedProjectBuilder> entity,
            RuntimeException toThrow) {
        entity.getBuilder().setShouldThrow(toThrow);
    }

    @SuppressWarnings({"MethodMayBeStatic", "unused"})  // Methods accessed via reflection.
    static class TestAggregate
            extends Aggregate<ProjectId, Project, PatchedProjectBuilder> {

        private final List<Message> receivedEvents = newLinkedList();
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
            if(violations != null) {
                return ImmutableList.copyOf(violations);
            }
            return super.checkEntityState(newState);
        }

        @Assign
        ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            return projectCreated(cmd.getProjectId(), cmd.getName());
        }

        @Apply
        private void event(ProjectCreated event) {
            receivedEvents.add(event);
            final Project newState = Project.newBuilder(getState())
                                            .setId(event.getProjectId())
                                            .setName(event.getName())
                                            .build();
            getBuilder().mergeFrom(newState);
        }

        @Apply
        private void event(TaskAdded event) {
            throw new RuntimeException("that tests the tx behaviour");
        }

        private List<Message> getReceivedEvents() {
            return ImmutableList.copyOf(receivedEvents);
        }
    }

    /**
     * Custom implementation of {@code ValidatingBuilder}, which allows to simulate an error
     * during the state building.
     *
     * <p>Must be declared {@code public} to allow accessing from the
     * {@linkplain org.spine3.validate.ValidatingBuilders#newInstance(Class) factory method}.
     */
    public static class PatchedProjectBuilder
            extends ThrowingValidatingBuilder<Project, Project.Builder> {

        public static PatchedProjectBuilder newBuilder() {
            return new PatchedProjectBuilder();
        }
    }
}
