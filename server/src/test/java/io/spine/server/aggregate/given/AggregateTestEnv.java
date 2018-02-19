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

import io.spine.core.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.user.User;
import io.spine.test.aggregate.user.UserVBuilder;

import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;

/**
 * @author Alexander Yevsyukov
 */
public class AggregateTestEnv {

    private AggregateTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /**
     * The test environment class for test of missing command handler or missing event applier.
     */
    public static class AggregateWithMissingApplier
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private boolean createProjectCommandHandled = false;

        public AggregateWithMissingApplier(ProjectId id) {
            super(id);
        }

        /** There is no event applier for ProjectCreated event (intentionally). */
        @Assign
        AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
            createProjectCommandHandled = true;
            return projectCreated(cmd.getProjectId(), cmd.getName());
        }

        public boolean isCreateProjectCommandHandled() {
            return createProjectCommandHandled;
        }
    }

    /**
     * An aggregate with {@code Integer} ID.
     */
    public static class IntAggregate extends Aggregate<Integer, Project, ProjectVBuilder> {
        public IntAggregate(Integer id) {
            super(id);
        }
    }

    /**
     * The test environment class for checking raising and catching exceptions.
     */
    public static class FaultyAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        public static final String BROKEN_HANDLER = "broken_handler";
        public static final String BROKEN_APPLIER = "broken_applier";

        private final boolean brokenHandler;
        private final boolean brokenApplier;

        public FaultyAggregate(ProjectId id, boolean brokenHandler, boolean brokenApplier) {
            super(id);
            this.brokenHandler = brokenHandler;
            this.brokenApplier = brokenApplier;
        }

        @Assign
        AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
            if (brokenHandler) {
                throw new IllegalStateException(BROKEN_HANDLER);
            }
            return projectCreated(cmd.getProjectId(), cmd.getName());
        }

        @Apply
        void event(AggProjectCreated event) {
            if (brokenApplier) {
                throw new IllegalStateException(BROKEN_APPLIER);
            }

            getBuilder().setStatus(Status.CREATED);
        }
    }

    /**
     * The test environment aggregate for testing validation during aggregate state transition.
     */
    public static class UserAggregate extends Aggregate<String, User, UserVBuilder> {
        private UserAggregate(String id) {
            super(id);
        }
    }
}
