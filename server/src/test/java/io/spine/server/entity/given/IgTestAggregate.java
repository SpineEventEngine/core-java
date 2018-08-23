/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity.given;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.entity.Project;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.ProjectVBuilder;
import io.spine.test.entity.command.EntCreateProject;
import io.spine.test.entity.command.EntStartProject;
import io.spine.test.entity.event.EntProjectCreated;
import io.spine.test.entity.event.EntProjectStarted;

import static io.spine.test.entity.Project.Status.CREATED;
import static io.spine.test.entity.Project.Status.STARTED;

/**
 * An aggregate class for {@linkplain io.spine.server.entity.IdempotencyGuardTest
 * IdempotencyGuard tests}.
 *
 * @author Mykhailo Drachuk
 */
public class IgTestAggregate
        extends Aggregate<ProjectId, Project, ProjectVBuilder> {

    public IgTestAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    EntProjectCreated handle(EntCreateProject cmd) {
        EntProjectCreated event = EntProjectCreated
                .newBuilder()
                .setProjectId(cmd.getProjectId())
                .build();
        return event;
    }

    @Assign
    EntProjectStarted handle(EntStartProject cmd) {
        EntProjectStarted message = EntProjectStarted
                .newBuilder()
                .setProjectId(cmd.getProjectId())
                .build();
        return message;
    }

    @Apply
    void event(EntProjectCreated event) {
        getBuilder()
                .setId(event.getProjectId())
                .setStatus(CREATED);
    }

    @Apply
    void event(EntProjectStarted event) {
        getBuilder()
                .setId(event.getProjectId())
                .setStatus(STARTED);
    }
}
