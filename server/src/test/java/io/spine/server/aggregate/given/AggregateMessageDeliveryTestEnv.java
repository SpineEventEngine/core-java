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
package io.spine.server.aggregate.given;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.React;
import io.spine.core.Rejection;
import io.spine.core.RejectionContext;
import io.spine.core.Rejections;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.delivery.ShardingStrategy;
import io.spine.server.delivery.UniformAcrossTargets;
import io.spine.server.delivery.given.ThreadStats;
import io.spine.server.route.RejectionRoute;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectCancelled;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.rejection.Rejections.AggCannotStartArchivedProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import io.spine.validate.StringValueVBuilder;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

/**
 * @author Alex Tymchenko
 */
public class AggregateMessageDeliveryTestEnv {

    /** Prevents instantiation of this test environment class. */
    private AggregateMessageDeliveryTestEnv() {
    }

    public static Command startProject() {
        ProjectId projectId = projectId();
        Command command = createCommand(AggStartProject.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build());
        return command;
    }

    public static Event projectStarted() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory =
                TestEventFactory.newInstance(
                        pack(projectId),
                        AggregateMessageDeliveryTestEnv.class
                );

        AggProjectStarted msg = AggProjectStarted.newBuilder()
                                                 .setProjectId(projectId)
                                                 .build();

        Event result = eventFactory.createEvent(msg);
        return result;
    }

    public static Event projectCancelled() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory =
                TestEventFactory.newInstance(
                        pack(projectId),
                        AggregateMessageDeliveryTestEnv.class
                );

        AggProjectCancelled msg = AggProjectCancelled.newBuilder()
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

    public static Rejection cannotStartProject() {
        ProjectId projectId = projectId();

        AggStartProject cmdMessage = AggStartProject.newBuilder()
                                                    .setProjectId(projectId)
                                                    .build();
        Command command = createCommand(cmdMessage);

        Rejection result = Rejections.toRejection(
                new io.spine.test.aggregate.rejection.AggCannotStartArchivedProject(
                        projectId, Lists.newArrayList()),
                command);
        return result;
    }

    private static Command createCommand(Message cmdMessage) {
        Command result = TestActorRequestFactory.newInstance(AggregateMessageDeliveryTestEnv.class)
                                                .createCommand(cmdMessage);
        return result;
    }

    public static RejectionRoute<ProjectId, Message> routeByProjectId() {
        return new RejectionRoute<ProjectId, Message>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Set<ProjectId> apply(Message raw, RejectionContext context) {
                AggCannotStartArchivedProject msg = (AggCannotStartArchivedProject) raw;
                return ImmutableSet.of(msg.getProjectId());
            }
        };
    }

    /**
     * An aggregate class, which remembers the threads in which its handler methods were invoked.
     *
     * <p>Message handlers are invoked via reflection, so some of them are considered unused.
     *
     * <p>Some handler parameters are not used, as they aren't needed for tests. They are still
     * present, as long as they are required according to the handler declaration rules.
     */
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "unused"})
    public static class DeliveryProject
            extends Aggregate<ProjectId, StringValue, StringValueVBuilder> {

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
        List<Message> onRejection(AggCannotStartArchivedProject rejection) {
            stats.recordCallingThread(getId());
            return emptyList();
        }

        public static ThreadStats<ProjectId> getStats() {
            return stats;
        }
    }

    public static class SingleShardProjectRepository
            extends AggregateRepository<ProjectId, DeliveryProject> {
        public SingleShardProjectRepository() {
            super();
            getRejectionRouting().replaceDefault(routeByProjectId());
        }

    }

    public static class TripleShardProjectRepository
            extends AggregateRepository<ProjectId, DeliveryProject> {

        public TripleShardProjectRepository() {
            super();
            getRejectionRouting().replaceDefault(routeByProjectId());
        }

        @Override
        public ShardingStrategy getShardingStrategy() {
            return UniformAcrossTargets.forNumber(3);
        }
    }
}
