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
package io.spine.server.procman;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.base.Event;
import io.spine.base.Subscribe;
import io.spine.base.Version;
import io.spine.server.entity.ThrowingValidatingBuilder;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionShould;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.event.ProjectCreated;
import io.spine.test.procman.event.TaskAdded;
import io.spine.validate.ConstraintViolation;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.Assert.assertTrue;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * @author Alex Tymchenko
 */
public class ProcManTransactionShould extends TransactionShould<ProjectId,
        ProcessManager<ProjectId, Project, ProcManTransactionShould.PatchedProjectBuilder>,
        Project,
        ProcManTransactionShould.PatchedProjectBuilder> {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("procman-transaction-should-project")
                                                 .build();

    @Override
    protected Transaction<ProjectId,
                ProcessManager<ProjectId, Project, PatchedProjectBuilder>,
                Project,
                PatchedProjectBuilder>
    createTx(ProcessManager<ProjectId, Project, PatchedProjectBuilder> entity) {
        return new ProcManTransaction<>(entity);
    }

    @Override
    protected Transaction<ProjectId,
            ProcessManager<ProjectId, Project, PatchedProjectBuilder>,
            Project,
            PatchedProjectBuilder> createTxWithState(
            ProcessManager<ProjectId, Project, PatchedProjectBuilder> entity, Project state,
            Version version) {
        return new ProcManTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<ProjectId, ProcessManager<ProjectId, Project, PatchedProjectBuilder>,
                          Project, PatchedProjectBuilder>
    createTxWithListener(ProcessManager<ProjectId, Project, PatchedProjectBuilder> entity,
                         TransactionListener<ProjectId,
                                             ProcessManager<ProjectId,
                                                            Project,
                                                            PatchedProjectBuilder>,
                                             Project, PatchedProjectBuilder> listener) {
        return new ProcManTransaction<>(entity, listener);
    }

    @Override
    protected ProcessManager<ProjectId, Project, PatchedProjectBuilder> createEntity() {
        return new TestProcessManager(ID);
    }

    @Override
    protected ProcessManager<ProjectId, Project, PatchedProjectBuilder> createEntity(
            List<ConstraintViolation> violations) {
        return new TestProcessManager(ID, violations);
    }

    @Override
    protected Project createNewState() {
        return Project.newBuilder()
                      .setId(ID)
                      .setName("The new project name for procman tx tests")
                      .build();
    }

    @Override
    protected void checkEventReceived(
            ProcessManager<ProjectId, Project, PatchedProjectBuilder> entity,
            Event event) {

        final TestProcessManager aggregate = (TestProcessManager) entity;
        final Message actualMessage = unpack(event.getMessage());
        assertTrue(aggregate.getReceivedEvents()
                            .contains(actualMessage));
    }

    @Override
    protected Message createEventMessage() {
        return ProjectCreated.newBuilder()
                             .setProjectId(ID)
                             .build();
    }

    @Override
    protected Message createEventMessageThatFailsInHandler() {
        return TaskAdded.newBuilder()
                        .setProjectId(ID)
                        .build();
    }

    @Override
    protected void breakEntityValidation(
            ProcessManager<ProjectId, Project, PatchedProjectBuilder> entity,
            RuntimeException toThrow) {
        entity.getBuilder().setShouldThrow(toThrow);
    }

    @SuppressWarnings({"MethodMayBeStatic", "unused"})  // Methods accessed via reflection.
    static class TestProcessManager
            extends ProcessManager<ProjectId, Project, PatchedProjectBuilder> {

        private final List<Message> receivedEvents = newLinkedList();
        private final List<ConstraintViolation> violations;

        private TestProcessManager(ProjectId id) {
            this(id, null);
        }

        private TestProcessManager(ProjectId id, @Nullable List<ConstraintViolation> violations) {
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

        @Subscribe
        public void event(ProjectCreated event) {
            receivedEvents.add(event);
            final Project newState = Project.newBuilder(getState())
                                            .setId(event.getProjectId())
                                            .build();
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        public void event(TaskAdded event) {
            throw new RuntimeException("that tests the tx behaviour for process manager");
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
     * {@linkplain io.spine.validate.ValidatingBuilders#newInstance(Class) factory method}.
     */
    public static class PatchedProjectBuilder
            extends ThrowingValidatingBuilder<Project, Project.Builder> {

        public static PatchedProjectBuilder newBuilder() {
            return new PatchedProjectBuilder();
        }
    }
}
