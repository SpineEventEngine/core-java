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

package io.spine.server.aggregate.given;

import com.google.protobuf.Message;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.TenantId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.AggregateShould;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;

import static io.spine.Identifier.newUuid;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.projectStarted;

/**
 * @author Mykhailo Drachuk
 */
public class IdempotencyGuardTestEnv {

    /** Prevents instantiation of this test environment. */
    private IdempotencyGuardTestEnv() {
        // Do nothing.
    }

    public static ProjectId newProjectId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    public static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    public static AggCreateProject createProject(ProjectId projectId) {
        return AggCreateProject.newBuilder()
                               .setProjectId(projectId)
                               .build();
    }

    public static AggStartProject startProject(ProjectId projectId) {
        return AggStartProject.newBuilder()
                              .setProjectId(projectId)
                              .build();
    }

    public static TestActorRequestFactory newRequestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(AggregateShould.class, tenantId);
    }

    public static Command command(Message commandMessage, TenantId tenantId) {
        return newRequestFactory(tenantId).command()
                                          .create(commandMessage);
    }

    public static class TestAggregateRepository
            extends AggregateRepository<ProjectId, TestAggregate> {
    }

    /**
     * An aggregate class with handlers and appliers.
     *
     * <p>This class is declared here instead of being inner class of {@link AggregateTestEnv}
     * because it is heavily connected with internals of this test suite.
     */
    public static class TestAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        public TestAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        AggProjectCreated handle(AggCreateProject cmd) {
            final AggProjectCreated event = projectCreated(cmd.getProjectId(),
                                                           cmd.getName());
            return event;
        }

        @Assign
        AggProjectStarted handle(AggStartProject cmd) {
            final AggProjectStarted message = projectStarted(cmd.getProjectId());
            return message;
        }

        @Apply
        void event(AggProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Status.CREATED);
        }

        @Apply
        void event(AggProjectStarted event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Status.STARTED);
        }
    }
}
