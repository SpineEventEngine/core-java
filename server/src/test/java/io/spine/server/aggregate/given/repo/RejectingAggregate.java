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

package io.spine.server.aggregate.given.repo;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.SubProjectList;
import io.spine.test.aggregate.command.AggCreateProjectWithChildren;
import io.spine.test.aggregate.command.AggStartProjectWithChildren;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.rejection.AggCannotStartArchivedProject;

/**
 * The aggregate with the state of {@linkplain io.spine.test.aggregate.event.AggProjectCreated creation event}
 * wrapped into {@code Any}. The event keeps the list of child project IDs, which we use
 * in rejecting subsequent commands.
 */
class RejectingAggregate
        extends Aggregate<ProjectId, SubProjectList, SubProjectList.Builder> {

    private RejectingAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    AggProjectCreated on(AggCreateProjectWithChildren cmd) {
        return AggProjectCreated.newBuilder()
                                .setProjectId(cmd.getProjectId())
                                .addAllChildProjectId(cmd.getChildProjectIdList())
                                .build();
    }

    @Apply
    private void event(AggProjectCreated event) {
        builder().addAllItem(event.getChildProjectIdList());
    }

    @Assign
    AggProjectStarted on(AggStartProjectWithChildren cmd) throws AggCannotStartArchivedProject {
        throw AggCannotStartArchivedProject.newBuilder()
                .setProjectId(id())
                .addAllChildProjectId(state().getItemList())
                .build();
    }
}
