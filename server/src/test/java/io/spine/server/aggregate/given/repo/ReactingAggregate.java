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

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.event.React;
import io.spine.server.test.shared.StringAggregate;
import io.spine.server.test.shared.StringAggregateVBuilder;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.event.AggProjectArchived;
import io.spine.test.aggregate.event.AggProjectDeleted;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * An aggregate class that reacts only on events and does not handle commands.
 */
public class ReactingAggregate
        extends Aggregate<ProjectId, StringAggregate, StringAggregateVBuilder> {

    public static final String PROJECT_ARCHIVED = "PROJECT_ARCHIVED";
    public static final String PROJECT_DELETED = "PROJECT_DELETED";

    private ReactingAggregate(ProjectId id) {
        super(id);
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

    @Apply
    void apply(AggProjectArchived event) {
        getBuilder().setValue(PROJECT_ARCHIVED);
    }

    @Apply
    void apply(AggProjectDeleted event) {
        getBuilder().setValue(PROJECT_DELETED);
    }
}
