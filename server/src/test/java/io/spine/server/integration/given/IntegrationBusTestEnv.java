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
package io.spine.server.integration.given;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.React;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.command.Rejection;
import io.spine.server.event.EventSubscriber;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.transport.TransportFactory;
import io.spine.test.integration.Project;
import io.spine.test.integration.ProjectId;
import io.spine.test.integration.ProjectVBuilder;
import io.spine.test.integration.command.ItgStartProject;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.event.ItgProjectStarted;
import io.spine.test.integration.rejection.IntegrationRejections.ItgCannotStartArchivedProject;
import io.spine.test.integration.rejection.IntegrationRejections.ItgProjectAlreadyExists;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import io.spine.validate.Int32ValueVBuilder;
import io.spine.validate.StringValueVBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;
import static io.spine.testing.server.TestEventFactory.newInstance;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * @author Alex Tymchenko
 */
public class IntegrationBusTestEnv {

    /** Prevents instantiation of this utility class. */
    private IntegrationBusTestEnv() {
    }

    @CanIgnoreReturnValue
    public static BoundedContext
    contextWithExtEntitySubscribers(TransportFactory transportFactory) {
        BoundedContext boundedContext = contextWithTransport(transportFactory);
        boundedContext.register(new ProjectDetailsRepository());
        boundedContext.register(new ProjectWizardRepository());
        boundedContext.register(new ProjectCountAggregateRepository());
        return boundedContext;
    }

    public static BoundedContext contextWithContextAwareEntitySubscriber(
            TransportFactory transportFactory) {
        BoundedContext boundedContext = contextWithTransport(transportFactory);
        boundedContext.register(new ContextAwareProjectDetailsRepository());
        return boundedContext;
    }

    @CanIgnoreReturnValue
    public static BoundedContext contextWithExternalSubscribers(TransportFactory transportFactory) {
        BoundedContext boundedContext = contextWithTransport(transportFactory);
        EventSubscriber eventSubscriber = new ProjectEventsSubscriber();
        boundedContext.getIntegrationBus()
                      .register(eventSubscriber);
        boundedContext.getEventBus()
                      .register(eventSubscriber);
        boundedContext.register(new ProjectCountAggregateRepository());
        boundedContext.register(new ProjectWizardRepository());
        return boundedContext;
    }

    public static BoundedContext contextWithTransport(TransportFactory transportFactory) {
        IntegrationBus.Builder builder = IntegrationBus.newBuilder()
                                                       .setTransportFactory(transportFactory);
        BoundedContext result = BoundedContext
                .newBuilder()
                .setName(newUuid())
                .setIntegrationBus(builder)
                .build();
        return result;
    }

    public static BoundedContext contextWithProjectCreatedNeeds(TransportFactory factory) {
        BoundedContext result = contextWithTransport(factory);
        result.getIntegrationBus()
              .register(new ProjectEventsSubscriber());
        return result;
    }

    public static BoundedContext contextWithProjectStartedNeeds(TransportFactory factory) {
        BoundedContext result = contextWithTransport(factory);
        result.getIntegrationBus()
              .register(new ProjectStartedExtSubscriber());
        return result;
    }

    public static Event projectCreated() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory = newInstance(pack(projectId), IntegrationBusTestEnv.class);
        return eventFactory.createEvent(
                ItgProjectCreated.newBuilder()
                                 .setProjectId(projectId)
                                 .build()
        );
    }

    public static Event projectStarted() {
        ProjectId projectId = projectId();
        TestEventFactory eventFactory =
                newInstance(pack(projectId), IntegrationBusTestEnv.class);
        return eventFactory.createEvent(
                ItgProjectStarted.newBuilder()
                                 .setProjectId(projectId)
                                 .build()
        );
    }

    @SuppressWarnings("ThrowableNotThrown")     // used to create a rejection
    public static Event cannotStartArchivedProject() {
        ProjectId projectId = projectId();
        ItgStartProject cmdMessage = ItgStartProject
                .newBuilder()
                .setProjectId(projectId)
                .build();
        Command startProjectCmd = toCommand(cmdMessage);
        io.spine.test.integration.rejection.ItgCannotStartArchivedProject throwable =
                new io.spine.test.integration.rejection.ItgCannotStartArchivedProject(projectId);
        throwable.initProducer(pack(projectId));
        Rejection rejection = Rejection.from(CommandEnvelope.of(startProjectCmd), throwable);
        return rejection.asEvent();
    }

    private static Command toCommand(ItgStartProject cmdMessage) {
        return TestActorRequestFactory.newInstance(IntegrationBusTestEnv.class)
                                      .createCommand(cmdMessage);
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

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
        }
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
    public static class ProjectWizard
            extends ProcessManager<ProjectId, Project, ProjectVBuilder> {

        protected ProjectWizard(ProjectId id) {
            super(id);
        }

        private static Message externalEvent = null;

        @React(external = true)
        List<Message> on(ItgProjectCreated event) {
            externalEvent = event;
            return Collections.emptyList();
        }

        @React(external = true)
        List<Message> on(ItgCannotStartArchivedProject rejection) {
            externalEvent = rejection;
            return Collections.emptyList();
        }

        public static <M extends Message> M getExternalEvent() {
            @SuppressWarnings("unchecked") // OK for tests.
            M event = (M) externalEvent;
            return event;
        }

        public static void clear() {
            externalEvent = null;
        }
    }

    private static class ProjectWizardRepository
            extends ProcessManagerRepository<ProjectId, ProjectWizard, Project> {

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
        }
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
    public static class ProjectCountAggregate
            extends Aggregate<ProjectId, Int32Value, Int32ValueVBuilder> {

        private static ItgProjectCreated externalEvent = null;

        private static ItgCannotStartArchivedProject externalRejection = null;

        protected ProjectCountAggregate(ProjectId id) {
            super(id);
        }

        @React(external = true)
        List<Message> on(ItgProjectCreated event) {
            externalEvent = event;
            return Collections.emptyList();
        }

        @React(external = true)
        List<Message> on(ItgCannotStartArchivedProject rejection) {
            externalRejection = rejection;
            return Collections.emptyList();
        }

        public static ItgProjectCreated getExternalEvent() {
            return externalEvent;
        }

        public static ItgCannotStartArchivedProject getExternalRejection() {
            return externalRejection;
        }

        public static void clear() {
            externalEvent = null;
            externalRejection = null;
        }
    }

    private static class ProjectCountAggregateRepository
            extends AggregateRepository<ProjectId, ProjectCountAggregate> {

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
        }
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

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
        }
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

        /**
         * Rethrow all the issues, so that they are visible to tests.
         */
        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
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

        /**
         * Rethrow all the issues, so that they are visible to tests.
         */
        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
        }
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
    public static class ProjectRejectionsExtSubscriber extends EventSubscriber {

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

        public static void clear() {
            externalRejection = null;
            domesticRejection = null;
        }

        /**
         * Rethrow all the issues, so that they are visible to tests.
         */
        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
        }
    }

    /**
     * A subscriber for testing of external attribute mismatch check.
     */
    @SuppressWarnings("unused") // OK to have unused params in this test env. class
    public static final class ExternalMismatchSubscriber extends EventSubscriber {

        @Subscribe(external = true)
        public void on(ItgCannotStartArchivedProject rejection, ItgStartProject command) {
            // do nothing.
        }

        @Subscribe
        public void on(ItgCannotStartArchivedProject rejection) {
            // do nothing.
        }

        /**
         * Rethrow all the issues, so that they are visible to tests.
         */
        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            throw illegalStateWithCauseOf(exception);
        }
    }
}
