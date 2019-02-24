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

package io.spine.testing.server.blackbox.given;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.testing.server.blackbox.BbReport;
import io.spine.testing.server.blackbox.BbReportId;
import io.spine.testing.server.blackbox.BbReportVBuilder;
import io.spine.testing.server.blackbox.command.BbCreateReport;
import io.spine.testing.server.blackbox.event.BbReportCreated;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import io.spine.testing.server.blackbox.event.BbTaskAddedToReport;

public class BbReportAggregate extends Aggregate<BbReportId, BbReport, BbReportVBuilder> {

    protected BbReportAggregate(BbReportId id) {
        super(id);
    }

    @Assign
    BbReportCreated handle(BbCreateReport command) {
        return BbReportCreated
                .newBuilder()
                .setReportId(command.getReportId())
                .addAllProjectId(command.getProjectIdList())
                .build();
    }

    @React
    BbTaskAddedToReport on(BbTaskAdded event) {
        return BbTaskAddedToReport
                .newBuilder()
                .setReportId(id())
                .setProjectId(event.getProjectId())
                .setTask(event.getTask())
                .build();

    }

    @Apply
    void on(BbReportCreated event) {
        builder().setId(event.getReportId())
                 .addAllProjectIds(event.getProjectIdList());
    }

    @Apply
    void on(BbTaskAddedToReport event) {
        builder().addTasks(event.getTask());
    }
}
