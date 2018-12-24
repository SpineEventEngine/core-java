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

package io.spine.server.procman.given.delivery;

import com.google.protobuf.Message;
import io.spine.server.command.Assign;
import io.spine.server.delivery.given.ThreadStats;
import io.spine.server.event.React;
import io.spine.server.procman.ProcessManager;
import io.spine.server.test.shared.EmptyProcess;
import io.spine.server.test.shared.EmptyProcessVBuilder;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.rejection.Rejections;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A process manager class, which remembers the threads in which its handler methods
 * were invoked.
 *
 * <p>Message handlers are invoked via reflection, so some of them are considered unused.
 *
 * <p>The handler method parameters are not used, as they aren't needed for tests.
 * They are still present, as long as they are required according to the handler
 * declaration rules.
 */
public class DeliveryPm
        extends ProcessManager<ProjectId, EmptyProcess, EmptyProcessVBuilder> {

    private static final ThreadStats<ProjectId> stats = new ThreadStats<>();

    protected DeliveryPm(ProjectId id) {
        super(id);
    }

    @Assign
    PmProjectCreated on(PmCreateProject command) {
        stats.recordCallingThread(command.getProjectId());
        return PmProjectCreated.newBuilder()
                               .setProjectId(command.getProjectId())
                               .build();
    }

    @React
    List<Message> on(PmProjectStarted event) {
        stats.recordCallingThread(getId());
        return emptyList();
    }

    @React
    List<Message> on(Rejections.PmCannotStartArchivedProject rejection) {
        stats.recordCallingThread(getId());
        return emptyList();
    }

    public static ThreadStats<ProjectId> getStats() {
        return stats;
    }
}
