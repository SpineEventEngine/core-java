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
package io.spine.server.procman.given;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.React;
import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv;
import io.spine.server.command.Assign;
import io.spine.server.command.TestEventFactory;
import io.spine.server.procman.PmCommandDelivery;
import io.spine.server.procman.PmEventDelivery;
import io.spine.server.procman.PmRejectionDelivery;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.route.RejectionProducers;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.rejection.Rejections.PmCannotStartArchivedProject;
import io.spine.validate.StringValueVBuilder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static io.spine.core.Rejections.toRejection;
import static java.util.Collections.emptyList;

/**
 * @author Alex Tymchenko
 */
public class PmMessageDeliveryTestEnv {

    /** Prevents instantiation of this utility class. */
    private PmMessageDeliveryTestEnv() {}

    public static Command createProject() {
        final ProjectId projectId = projectId();
        final Command command = createCommand(PmCreateProject.newBuilder()
                                                             .setProjectId(projectId)
                                                             .build());
        return command;
    }

    public static Event projectStarted() {
        final ProjectId projectId = projectId();
        final TestEventFactory eventFactory =
                TestEventFactory.newInstance(
                        AnyPacker.pack(projectId),
                        PmMessageDeliveryTestEnv.class
                );

        final PmProjectStarted msg = PmProjectStarted.newBuilder()
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

        final PmStartProject cmdMessage = PmStartProject.newBuilder()
                                                        .setProjectId(projectId)
                                                        .build();
        final Command command = createCommand(cmdMessage);
        final Rejection result = toRejection(throwableWith(projectId), command);
        return result;
    }

    private static io.spine.test.procman.rejection.PmCannotStartArchivedProject
    throwableWith(ProjectId projectId) {
        return new io.spine.test.procman.rejection.PmCannotStartArchivedProject(projectId);
    }

    private static Command createCommand(Message cmdMessage) {
        final Command result =
                TestActorRequestFactory.newInstance(AggregateMessageDeliveryTestEnv.class)
                                       .createCommand(cmdMessage);
        return result;
    }

    /**
     * A process manager class, which declares all kinds of dispatching methods and remembers
     * the latest values submitted to each of them.
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    public static class ReactingProjectWizard
            extends ProcessManager<ProjectId, StringValue, StringValueVBuilder> {

        private static PmProjectStarted eventReceived = null;
        private static PmCreateProject commandReceived = null;
        private static PmCannotStartArchivedProject rejectionReceived = null;

        protected ReactingProjectWizard(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("unused")     // Accessed by the framework via reflection.
        @Assign
        PmProjectCreated on(PmCreateProject command) {
            commandReceived = command;
            getBuilder().setValue(command.getProjectId().getId());
            return PmProjectCreated.newBuilder()
                                   .setProjectId(command.getProjectId())
                                   .build();
        }

        @SuppressWarnings("unused")     // Accessed by the framework via reflection.
        @React
        List<Message> on(PmProjectStarted event) {
            eventReceived = event;
            return emptyList();
        }

        @SuppressWarnings("unused")     // Accessed by the framework via reflection.
        @React
        List<Message> on(PmCannotStartArchivedProject rejection) {
            rejectionReceived = rejection;
            return emptyList();
        }

        @Nullable
        public static PmCreateProject getCommandReceived() {
            return commandReceived;
        }

        @Nullable
        public static PmProjectStarted getEventReceived() {
            return eventReceived;
        }

        @Nullable
        public static PmCannotStartArchivedProject getRejectionReceived() {
            return rejectionReceived;
        }

    }

    /**
     * A repository which overrides the delivery strategy for events, rejections and commands,
     * postponing the delivery of each of these objects to process manager instances.
     */
    public static class PostponingRepository
            extends ProcessManagerRepository<ProjectId, ReactingProjectWizard, StringValue> {

        private PostponingEventDelivery eventDelivery = null;
        private PostponingRejectionDelivery rejectionDelivery = null;
        private PostponingCommandDelivery commandDelivery = null;

        public PostponingRepository() {
            // Override the routing to use the first field; it's more obvious for tests.
            getRejectionRouting().replaceDefault(
                    RejectionProducers.<ProjectId>fromFirstMessageField());
        }

        @Override
        public PostponingEventDelivery getEventEndpointDelivery() {
            if (eventDelivery == null) {
                eventDelivery = new PostponingEventDelivery(this);
            }
            return eventDelivery;
        }

        @Override
        public PostponingRejectionDelivery getRejectionEndpointDelivery() {
            if (rejectionDelivery == null) {
                rejectionDelivery = new PostponingRejectionDelivery(this);
            }
            return rejectionDelivery;
        }

        @Override
        public PostponingCommandDelivery getCommandEndpointDelivery() {
            if (commandDelivery == null) {
                commandDelivery = new PostponingCommandDelivery(this);
            }
            return commandDelivery;
        }
    }

    /**
     * Event delivery strategy which always postpones the delivery, but remembers the event
     * along with the target entity ID.
     */
    public static class PostponingEventDelivery
            extends PmEventDelivery<ProjectId, ReactingProjectWizard> {

        private final Map<ProjectId, EventEnvelope> postponedEvents = newHashMap();

        PostponingEventDelivery(PostponingRepository repo) {
            super(repo);
        }

        @Override
        public boolean shouldPostpone(ProjectId id, EventEnvelope envelope) {
            postponedEvents.put(id, envelope);
            return true;
        }

        public Map<ProjectId, EventEnvelope> getPostponedEvents() {
            return ImmutableMap.copyOf(postponedEvents);
        }
    }

    /**
     * Rejection delivery strategy which always postpones the delivery, but remembers the rejection
     * along with the target entity ID.
     */
    public static class PostponingRejectionDelivery
            extends PmRejectionDelivery<ProjectId, ReactingProjectWizard> {

        private final Map<ProjectId, RejectionEnvelope> postponedRejections = newHashMap();

        PostponingRejectionDelivery(PostponingRepository repository) {
            super(repository);
        }

        @Override
        public boolean shouldPostpone(ProjectId id, RejectionEnvelope envelope) {
            postponedRejections.put(id, envelope);
            return true;
        }

        public Map<ProjectId, RejectionEnvelope> getPostponedRejections() {
            return ImmutableMap.copyOf(postponedRejections);
        }
    }

    /**
     * Command delivery strategy which always postpones the delivery, but remembers the command
     * along with the target entity ID.
     */
    public static class PostponingCommandDelivery
            extends PmCommandDelivery<ProjectId, ReactingProjectWizard> {

        private final Map<ProjectId, CommandEnvelope> postponedCommands = newHashMap();

        PostponingCommandDelivery(PostponingRepository repository) {
            super(repository);
        }

        @Override
        public boolean shouldPostpone(ProjectId id, CommandEnvelope envelope) {
            postponedCommands.put(id, envelope);
            return true;
        }

        public Map<ProjectId, CommandEnvelope> getPostponedCommands() {
            return ImmutableMap.copyOf(postponedCommands);
        }
    }
}
