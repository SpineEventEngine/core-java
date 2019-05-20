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

package io.spine.server.aggregate.given.aggregate;

import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggCancelProject;
import io.spine.test.aggregate.command.AggPauseProject;
import io.spine.test.aggregate.event.AggProjectCancelled;
import io.spine.test.aggregate.event.AggProjectPaused;

import java.util.List;

import static io.spine.server.aggregate.given.Given.EventMessage.projectCancelled;
import static io.spine.server.aggregate.given.Given.EventMessage.projectPaused;

/**
 * A test-only aggregate, that handles some commands fine, but does not change own state
 * in any of event appliers.
 *
 * <p>One might say, this aggregate sticks to its roots and denies changes. Hence the name.
 */
public class AmishAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

    public AmishAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    AggProjectPaused handle(AggPauseProject cmd, CommandContext ctx) {
        AggProjectPaused event = projectPaused(cmd.getProjectId());
        return event;
    }

    @Assign
    List<Message> handle(AggCancelProject cmd, CommandContext ctx) {
        AggProjectPaused firstPaused = projectPaused(cmd.getProjectId());
        AggProjectCancelled thenCancelled = projectCancelled(cmd.getProjectId());
        return Lists.newArrayList(firstPaused, thenCancelled);
    }

    @Apply
    private void on(AggProjectPaused event) {
        // do nothing.
    }

    @Apply
    private void on(AggProjectCancelled event) {
        // do nothing.
    }
}
