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

package io.spine.server.aggregate.given.delivery;

import com.google.protobuf.Message;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.delivery.given.ThreadStats;
import io.spine.server.event.React;
import io.spine.server.test.shared.StringAggregate;
import io.spine.server.test.shared.StringAggregateVBuilder;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectCancelled;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.rejection.Rejections;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * An aggregate class, which remembers the threads in which its handler methods were invoked.
 *
 * <p>Message handlers are invoked via reflection, so some of them are considered unused.
 *
 * <p>Some handler parameters are not used, as they aren't needed for tests. They are still
 * present, as long as they are required according to the handler declaration rules.
 */
public class DeliveryProject
        extends Aggregate<ProjectId, StringAggregate, StringAggregateVBuilder> {

    private static final ThreadStats<ProjectId> stats = new ThreadStats<>();

    protected DeliveryProject(ProjectId id) {
        super(id);
    }

    @Assign
    AggProjectStarted on(AggStartProject cmd) {
        stats.recordCallingThread(getId());
        AggProjectStarted event = AggProjectStarted.newBuilder()
                                                   .setProjectId(cmd.getProjectId())
                                                   .build();
        return event;
    }

    @SuppressWarnings("unused")     // an applier is required by the framework.
    @Apply
    void the(AggProjectStarted event) {
        // do nothing.
    }

    @React
    List<Message> onEvent(AggProjectCancelled event) {
        stats.recordCallingThread(getId());
        return emptyList();
    }

    @React
    List<Message> onRejection(Rejections.AggCannotStartArchivedProject rejection) {
        stats.recordCallingThread(getId());
        return emptyList();
    }

    public static ThreadStats<ProjectId> getStats() {
        return stats;
    }
}
