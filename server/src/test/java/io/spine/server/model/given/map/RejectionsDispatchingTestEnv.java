/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.model.given.map;

import io.spine.core.Subscribe;
import io.spine.model.contexts.projects.Project;
import io.spine.model.contexts.projects.ProjectId;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigStartProject;
import io.spine.model.contexts.projects.event.SigProjectCreated;
import io.spine.model.contexts.projects.event.SigProjectStarted;
import io.spine.model.contexts.projects.rejection.ProjectRejections;
import io.spine.model.contexts.projects.rejection.SigProjectAlreadyCompleted;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.AbstractEventSubscriber;

public final class RejectionsDispatchingTestEnv {

    private RejectionsDispatchingTestEnv() {
    }

    /**
     * A test aggregate that throws {@link SigProjectAlreadyCompleted} rejection for any
     * handled command.
     */
    public static final class ProjectAgg extends Aggregate<ProjectId, Project, Project.Builder> {

        @Assign
        SigProjectStarted handle(SigStartProject c) throws SigProjectAlreadyCompleted {
            throw SigProjectAlreadyCompleted
                    .newBuilder()
                    .setProject(c.getId())
                    .build();
        }

        @Assign
        SigProjectCreated handle(SigCreateProject c) throws SigProjectAlreadyCompleted {
            throw SigProjectAlreadyCompleted
                    .newBuilder()
                    .setProject(c.getId())
                    .build();
        }

        @Apply
        private void on(SigProjectStarted e) {
            // do nothing.
        }

        @Apply
        private void on(SigProjectCreated e) {
            // do nothing.
        }
    }

    /**
     * A test environment for the rejection subscriptions tests.
     *
     * <p>The subscriber is interested in the rejection thrown upon trying to
     * {@linkplain SigStartProject start} the project, but not in rejections thrown when the project
     * is just being {@linkplain SigCreateProject created}.
     */
    public static final class CompletionWatch extends AbstractEventSubscriber {

        @Subscribe
        void on(ProjectRejections.SigProjectAlreadyCompleted rejection, SigStartProject cmd) {
            // do nothing.
        }
    }
}
