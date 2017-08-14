/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.server.integration.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.StringValue;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Rejection;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.server.event.EventSubscriber;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.integration.TransportFactory;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.test.integration.ProjectId;
import io.spine.test.integration.command.ItgStartProject;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.event.ItgProjectStarted;
import io.spine.test.integration.rejection.IntegrationRejections.ItgCannotStartArchivedProject;
import io.spine.test.integration.rejection.IntegrationRejections.ItgProjectAlreadyExists;
import io.spine.validate.StringValueVBuilder;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.Identifier.newUuid;
import static io.spine.core.Rejections.createRejection;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.command.TestEventFactory.newInstance;

/**
 * @author Alex Tymchenko
 */
public class IntegrationBusTestEnv {

    private IntegrationBusTestEnv() {
        // Prevent instantiation of this utility class.
    }

    public static BoundedContext contextWithExtEntitySubscriber(TransportFactory transportFactory) {
        final BoundedContext boundedContext = contextWithTransport(transportFactory);
        boundedContext.register(new ProjectDetailsRepository());
        return boundedContext;
    }

    public static BoundedContext contextWithContextAwareEntitySubscriber(
            TransportFactory transportFactory) {
        final BoundedContext boundedContext = contextWithTransport(transportFactory);
        boundedContext.register(new ContextAwareProjectDetailsRepository());
        return boundedContext;
    }

    public static BoundedContext contextWithExternalSubscribers(TransportFactory transportFactory) {
        final BoundedContext boundedContext = contextWithTransport(transportFactory);
        final EventSubscriber eventSubscriber = new ProjectEventsSubscriber();
        boundedContext.getIntegrationBus()
                      .register(eventSubscriber);
        boundedContext.getEventBus()
                      .register(eventSubscriber);

        final RejectionSubscriber rejectionSubscriber = new CannotCreateProjectExtSubscriber();
        boundedContext.getRejectionBus().register(rejectionSubscriber);
        boundedContext.getIntegrationBus().register(rejectionSubscriber);
        return boundedContext;
    }

    public static BoundedContext contextWithTransport(TransportFactory transportFactory) {
        final IntegrationBus.Builder builder = IntegrationBus.newBuilder()
                                                             .setTransportFactory(transportFactory);
        final BoundedContext result = BoundedContext.newBuilder()
                                                    .setId(newUuid())
                                                    .setIntegrationBus(builder)
                                                    .build();
        return result;
    }

    public static BoundedContext contextWithProjectCreatedNeeds(TransportFactory factory) {
        final BoundedContext result = contextWithTransport(factory);
        result.getIntegrationBus()
              .register(new ProjectEventsSubscriber());
        return result;
    }

    public static BoundedContext contextWithProjectStartedNeeds(TransportFactory factory) {
        final BoundedContext result = contextWithTransport(factory);
        result.getIntegrationBus()
              .register(new ProjectStartedExtSubscriber());
        return result;
    }

    public static Event projectCreated() {
        final ProjectId projectId = projectId();
        final TestEventFactory eventFactory = newInstance(pack(projectId),
                                                          IntegrationBusTestEnv.class);
        return eventFactory.createEvent(ItgProjectCreated.newBuilder()
                                                         .setProjectId(projectId)
                                                         .build()
        );
    }

    public static Event projectStarted() {
        final ProjectId projectId = projectId();
        final TestEventFactory eventFactory = newInstance(pack(projectId),
                                                          IntegrationBusTestEnv.class);
        return eventFactory.createEvent(ItgProjectStarted.newBuilder()
                                                         .setProjectId(projectId)
                                                         .build()
        );
    }

    public static Rejection cannotStartArchivedProject() {
        final ProjectId projectId = projectId();
        final ItgStartProject cmdMessage = ItgStartProject.newBuilder()
                                                          .setProjectId(projectId)
                                                          .build();
        final Command startProjectCmd = toCommand(cmdMessage);
        final Rejection rejection = createRejection(
                ItgCannotStartArchivedProject.newBuilder()
                                             .setProjectId(projectId)
                                             .build(),
                startProjectCmd);
        return rejection;
    }

    private static Command toCommand(ItgStartProject cmdMessage) {
        return TestActorRequestFactory.newInstance(IntegrationBusTestEnv.class)
                                      .createCommand(
                                       cmdMessage
                               );
    }

    private static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
    public static class ProjectDetails
            extends Projection<ProjectId, StringValue, StringValueVBuilder> {

        private static ItgProjectCreated externalEvent = null;

        private static ItgProjectStarted domesticEvent = null;

        /**
         * Creates a new instance.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the supported types
         */
        protected ProjectDetails(ProjectId id) {
            super(id);
        }

        @Subscribe(external = true)
        public void on(ItgProjectCreated event) {
            externalEvent = event;
        }

        @Subscribe
        public void on(ItgProjectStarted event) {
            domesticEvent = event;
        }

        public static ItgProjectCreated getExternalEvent() {
            return externalEvent;
        }

        public static ItgProjectStarted getDomesticEvent() {
            return domesticEvent;
        }

        public static void clear() {
            externalEvent = null;
            domesticEvent = null;
        }
    }

    private static class ProjectDetailsRepository
            extends ProjectionRepository<ProjectId, ProjectDetails, StringValue> {
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
    public static class ContextAwareProjectDetails
            extends Projection<ProjectId, StringValue, StringValueVBuilder> {

        private static final Collection<EventContext> externalContexts = newLinkedList();
        private static final Collection<ItgProjectCreated> externalEvents = newLinkedList();

        /**
         * Creates a new instance.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the supported types
         */
        protected ContextAwareProjectDetails(ProjectId id) {
            super(id);
        }

        @Subscribe(external = true)
        public void on(ItgProjectCreated event, EventContext eventContext) {
            externalEvents.add(event);
            externalContexts.add(eventContext);
        }

        public static List<EventContext> getExternalContexts() {
            return ImmutableList.copyOf(externalContexts);
        }

        public static List<ItgProjectCreated> getExternalEvents() {
            return ImmutableList.copyOf(externalEvents);
        }

        public static void clear() {
            externalContexts.clear();
            externalEvents.clear();
        }
    }

    private static class ContextAwareProjectDetailsRepository
            extends ProjectionRepository<ProjectId, ContextAwareProjectDetails, StringValue> {
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
    public static class ProjectEventsSubscriber extends EventSubscriber {

        private static ItgProjectCreated externalEvent = null;

        private static ItgProjectStarted domesticEvent = null;

        @Subscribe(external = true)
        public void on(ItgProjectCreated msg) {
            externalEvent = msg;
        }

        @Subscribe
        public void on(ItgProjectStarted msg) {
            domesticEvent = msg;
        }

        public static ItgProjectCreated getExternalEvent() {
            return externalEvent;
        }

        public static ItgProjectStarted getDomesticEvent() {
            return domesticEvent;
        }

        public static void clear() {
            externalEvent = null;
            domesticEvent = null;
        }
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
    public static class ProjectStartedExtSubscriber extends EventSubscriber {

        private static ItgProjectStarted externalEvent = null;

        @Subscribe(external = true)
        public void on(ItgProjectStarted msg) {
            externalEvent = msg;
        }

        public static ItgProjectStarted getExternalEvent() {
            return externalEvent;
        }

        public static void clear() {
            externalEvent = null;
        }
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
    public static class CannotCreateProjectExtSubscriber extends RejectionSubscriber {

        private static ItgCannotStartArchivedProject externalRejection = null;

        private static ItgProjectAlreadyExists domesticRejection = null;

        @Subscribe(external = true)
        public void on(ItgCannotStartArchivedProject rejection) {
            externalRejection = rejection;
        }

        @Subscribe
        public void on(ItgProjectAlreadyExists rejection) {
            domesticRejection = rejection;
        }

        public static ItgCannotStartArchivedProject getExternalRejection() {
            return externalRejection;
        }

        public static ItgProjectAlreadyExists getDomesticRejection() {
            return domesticRejection;
        }

        public static void clear() {
            externalRejection = null;
            domesticRejection = null;
        }
    }
}
