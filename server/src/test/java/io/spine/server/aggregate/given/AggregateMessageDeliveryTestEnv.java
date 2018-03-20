/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.React;
import io.spine.core.Rejection;
import io.spine.core.Rejections;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.command.TestEventFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.rejection.Rejections.AggCannotStartArchivedProject;
import io.spine.validate.StringValueVBuilder;

import java.util.List;

import static io.spine.protobuf.AnyPacker.pack;
import static java.util.Collections.emptyList;

/**
 * @author Alex Tymchenko
 */
public class AggregateMessageDeliveryTestEnv {

    /** Prevents instantiation of this utility class. */
    private AggregateMessageDeliveryTestEnv() {}

    public static Command startProject() {
        final ProjectId projectId = projectId();
        final Command command = createCommand(AggStartProject.newBuilder()
                                                             .setProjectId(projectId)
                                                             .build());
        return command;
    }

    public static Event projectStarted() {
        final ProjectId projectId = projectId();
        final TestEventFactory eventFactory =
                TestEventFactory.newInstance(
                        pack(projectId),
                        AggregateMessageDeliveryTestEnv.class
                );

        final AggProjectStarted msg = AggProjectStarted.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build();

        final Event result = eventFactory.createEvent(msg);
        return result;
    }

    private static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(Identifier.newUuid())
                        .build();
    }

    public static Rejection cannotStartProject() {
        final ProjectId projectId = projectId();


        final AggStartProject cmdMessage = AggStartProject.newBuilder()
                                                          .setProjectId(projectId)
                                                          .build();
        final Command command = createCommand(cmdMessage);

        final Rejection result = Rejections.toRejection(
                new io.spine.test.aggregate.rejection.AggCannotStartArchivedProject(
                        projectId, Lists.<ProjectId>newArrayList()),
                command);
        return result;
    }

    private static Command createCommand(Message cmdMessage) {
        final Command result =
                TestActorRequestFactory.newInstance(AggregateMessageDeliveryTestEnv.class)
                                       .createCommand(cmdMessage);
        return result;
    }

    /**
     * An aggregate class, which declares all kinds of message dispatching methods and remembers
     * the latest values submitted to each of them.
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    public static class ReactingProject
            extends Aggregate<ProjectId, StringValue, StringValueVBuilder> {


        private static final Multimap<Long, ProjectId> threadToId =
                Multimaps.synchronizedMultimap(HashMultimap.<Long, ProjectId>create());


        protected ReactingProject(ProjectId id) {
            super(id);
        }

        @Assign
        AggProjectStarted on(AggStartProject cmd) {
            final ProjectId projectId = getId();
            final long currentThreadId = Thread.currentThread()
                                               .getId();
            threadToId.put(currentThreadId, projectId);
            final AggProjectStarted event = AggProjectStarted.newBuilder()
                                                             .setProjectId(projectId)
                                                             .build();
            return event;
        }

        @SuppressWarnings("unused")     // an applier is required by the framework.
        @Apply
        void the(AggProjectStarted event) {
            // do nothing.
        }

        @React
        List<Message> onEvent(AggProjectStarted event) {
            return emptyList();
        }

        @React
        List<Message> onRejection(AggCannotStartArchivedProject rejection) {
            return emptyList();
        }

        public static Multimap<Long, ProjectId> getThreadToId() {
            return Multimaps.unmodifiableMultimap(threadToId);
        }

        public static void clearStats() {
            threadToId.clear();
        }
    }
}
