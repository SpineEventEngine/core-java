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
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv;
import io.spine.server.command.Assign;
import io.spine.server.delivery.given.ThreadStats;
import io.spine.server.event.React;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.rejection.Rejections.PmCannotStartArchivedProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author Alex Tymchenko
 */
public class GivenMessage {

    /** Prevents instantiation of this test environment class. */
    private GivenMessage() {
    }

    public static Command createProject() {
        ProjectId projectId = projectId();
        Command command = createCommand(PmCreateProject.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build());
        return command;
    }

    public static Event projectStarted() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory =
                TestEventFactory.newInstance(AnyPacker.pack(projectId),
                                             GivenMessage.class
                );

        PmProjectStarted msg = PmProjectStarted.newBuilder()
                                               .setProjectId(projectId)
                                               .build();

        Event result = eventFactory.createEvent(msg);
        return result;
    }

    private static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(Identifier.newUuid())
                        .build();
    }

    private static Command createCommand(Message cmdMessage) {
        Command result = TestActorRequestFactory.newInstance(AggregateMessageDeliveryTestEnv.class)
                                                .createCommand(cmdMessage);
        return result;
    }

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
    @SuppressWarnings("unused")
    public static class DeliveryPm
            extends ProcessManager<ProjectId, StringValue, StringValueVBuilder> {

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
        List<Message> on(PmCannotStartArchivedProject rejection) {
            stats.recordCallingThread(getId());
            return emptyList();
        }

        public static ThreadStats<ProjectId> getStats() {
            return stats;
        }
    }

    public static class SingleShardPmRepository
            extends ProcessManagerRepository<ProjectId, DeliveryPm, StringValue> {
    }

    public static class QuadrupleShardPmRepository
            extends ProcessManagerRepository<ProjectId, DeliveryPm, StringValue> {
    }
}
