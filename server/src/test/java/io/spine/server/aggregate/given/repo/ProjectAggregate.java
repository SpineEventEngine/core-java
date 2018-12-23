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

package io.spine.server.aggregate.given.repo;

import com.google.common.annotations.VisibleForTesting;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectArchived;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectDeleted;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * The main aggregate class for positive scenarios tests.
 */
public class ProjectAggregate
        extends Aggregate<ProjectId, Project, ProjectVBuilder> {

    private ProjectAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    AggProjectCreated handle(AggCreateProject msg) {
        return AggProjectCreated.newBuilder()
                                .setProjectId(msg.getProjectId())
                                .setName(msg.getName())
                                .build();
    }

    @Apply
    void apply(AggProjectCreated event) {
        getBuilder().setId(event.getProjectId())
                    .setName(event.getName());
    }

    @Assign
    AggTaskAdded handle(AggAddTask msg) {
        return AggTaskAdded.newBuilder()
                           .setProjectId(msg.getProjectId())
                           .setTask(msg.getTask())
                           .build();
    }

    @Apply
    void apply(AggTaskAdded event) {
        getBuilder().setId(event.getProjectId())
                    .addTask(event.getTask());
    }

    @Assign
    AggProjectStarted handle(AggStartProject msg) {
        return AggProjectStarted.newBuilder()
                                .setProjectId(msg.getProjectId())
                                .build();
    }

    @Apply
    void apply(AggProjectStarted event) {
        getBuilder().setStatus(Status.STARTED);
    }

    /**
     * Emits {@link io.spine.test.aggregate.event.AggProjectArchived} if the event is from the parent project.
     * Otherwise returns empty iterable.
     */
    @React
    Optional<AggProjectArchived> on(AggProjectArchived event) {
        if (event.getChildProjectIdList()
                 .contains(getId())) {
            AggProjectArchived reaction = AggProjectArchived
                    .newBuilder()
                    .setProjectId(getId())
                    .build();
            return Optional.of(reaction);
        } else {
            return Optional.empty();
        }
    }

    @Apply
    void apply(AggProjectArchived event) {
        setArchived(true);
    }

    /**
     * Emits {@link io.spine.test.aggregate.event.AggProjectDeleted} if the event is from the parent project.
     * Otherwise returns empty iterable.
     */
    @React
    Optional<AggProjectDeleted> on(AggProjectDeleted event) {
        if (event.getChildProjectIdList()
                 .contains(getId())) {
            AggProjectDeleted reaction = AggProjectDeleted
                    .newBuilder()
                    .setProjectId(getId())
                    .build();
            return of(reaction);
        } else {
            return empty();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open the method to the test suite.
     */
    @Override
    @VisibleForTesting
    public void setArchived(boolean archived) {
        super.setArchived(archived);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open the method to the test suite.
     */
    @Override
    @VisibleForTesting
    public void setDeleted(boolean deleted) {
        super.setDeleted(deleted);
    }
}
