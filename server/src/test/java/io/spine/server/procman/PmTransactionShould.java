/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionShould;
import io.spine.server.procman.given.PmTransactionTestEnv.PatchedProjectBuilder;
import io.spine.server.procman.given.PmTransactionTestEnv.TestProcessManager;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.validate.ConstraintViolation;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class PmTransactionShould
        extends TransactionShould<ProjectId,
                                  ProcessManager<ProjectId, Project, PatchedProjectBuilder>,
                                  Project,
                                  PatchedProjectBuilder> {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("procman-transaction-should-project")
                                                 .build();

    @Override
    protected Transaction<ProjectId,
            ProcessManager<ProjectId, Project, PatchedProjectBuilder>,
            Project,
            PatchedProjectBuilder>
    createTx(ProcessManager<ProjectId, Project, PatchedProjectBuilder> entity) {
        return new PmTransaction<>(entity);
    }

    @Override
    protected Transaction<ProjectId,
            ProcessManager<ProjectId, Project, PatchedProjectBuilder>,
            Project,
            PatchedProjectBuilder> createTxWithState(
            ProcessManager<ProjectId, Project, PatchedProjectBuilder> entity, Project state,
            Version version) {
        return new PmTransaction<>(entity, state, version);
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
        return new PmTransaction<>(entity, listener);
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
        return PmProjectCreated.newBuilder()
                                 .setProjectId(ID)
                                 .build();
    }

    @Override
    protected Message createEventMessageThatFailsInHandler() {
        return PmTaskAdded.newBuilder()
                            .setProjectId(ID)
                            .build();
    }

    @Override
    protected void breakEntityValidation(
            ProcessManager<ProjectId, Project, PatchedProjectBuilder> entity,
            RuntimeException toThrow) {
        entity.getBuilder()
              .setShouldThrow(toThrow);
    }

    @Ignore // The behavior is changed. The version should be auto incremented.
    @Test
    @Override
    public void advance_version_from_event() {}
}
