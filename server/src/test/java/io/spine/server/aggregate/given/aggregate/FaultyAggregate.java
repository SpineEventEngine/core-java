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

import io.spine.core.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;

import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;

/**
 * The test environment class for checking raising and catching exceptions.
 */
public final class FaultyAggregate
        extends Aggregate<ProjectId, Project, Project.Builder> {

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
    private void event(AggProjectCreated event) {
        if (brokenApplier) {
            throw new IllegalStateException(BROKEN_APPLIER);
        }
        builder().setStatus(Status.CREATED);
    }
}
