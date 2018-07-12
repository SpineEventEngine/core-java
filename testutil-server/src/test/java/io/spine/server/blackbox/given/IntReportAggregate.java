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

package io.spine.server.blackbox.given;

import io.spine.core.React;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.testing.server.blackbox.IntCreateReport;
import io.spine.testing.server.blackbox.IntReportCreated;
import io.spine.testing.server.blackbox.IntTaskAdded;
import io.spine.testing.server.blackbox.IntTaskAddedToReport;
import io.spine.testing.server.blackbox.Report;
import io.spine.testing.server.blackbox.ReportId;
import io.spine.testing.server.blackbox.ReportVBuilder;

/**
 * @author Mykhailo Drachuk
 */
public class IntReportAggregate extends Aggregate<ReportId, Report, ReportVBuilder> {

    protected IntReportAggregate(ReportId id) {
        super(id);
    }

    @Assign
    IntReportCreated handle(IntCreateReport command) {
        return IntReportCreated
                .newBuilder()
                .setReportId(command.getReportId())
                .addAllProjectId(command.getProjectIdList())
                .build();
    }

    @React
    IntTaskAddedToReport on(IntTaskAdded event) {
        return IntTaskAddedToReport
                .newBuilder()
                .setReportId(getId())
                .setProjectId(event.getProjectId())
                .setTask(event.getTask())
                .build();

    }

    @SuppressWarnings("ReturnValueIgnored")
    @Apply
    void on(IntReportCreated event) {
        getBuilder().setId(event.getReportId())
                    .addAllProjectIds(event.getProjectIdList());
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Apply
    void on(IntTaskAddedToReport event) {
        getBuilder().addTasks(event.getTask());
    }
}
