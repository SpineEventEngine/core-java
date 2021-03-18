/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.base.EventMessage;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.EventValidationError;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.event.EventBus;
import io.spine.server.integration.given.AnotherMemoizingProjectDetailsRepo;
import io.spine.server.integration.given.BillingAggregate;
import io.spine.server.integration.given.MemoizingProjectDetailsRepo;
import io.spine.server.integration.given.MemoizingProjection;
import io.spine.server.integration.given.PhotosProcMan;
import io.spine.server.integration.given.ProjectCommander;
import io.spine.server.integration.given.ProjectCountAggregate;
import io.spine.server.integration.given.ProjectDetails;
import io.spine.server.integration.given.ProjectEventsSubscriber;
import io.spine.server.integration.given.ProjectStartedExtSubscriber;
import io.spine.server.integration.given.ProjectWizard;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.blackbox.BlackBox;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.core.EventValidationError.UNSUPPORTED_EVENT_VALUE;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.contextWithExtEntitySubscribers;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.contextWithExternalSubscribers;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.contextWithProjectCreatedNeeds;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.contextWithProjectStartedNeeds;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.newContext;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.projectStarted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("IntegrationBroker should")
class IntegrationBrokerTest {

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        ServerEnvironment.instance()
                         .reset();
        ProjectDetails.clear();
        ProjectWizard.clear();
        ProjectCountAggregate.clear();
        MemoizingProjection.clear();
        ProjectEventsSubscriber.clear();
        ProjectStartedExtSubscriber.clear();
    }

    @AfterEach
    void tearDown() {
        ServerEnvironment.instance()
                         .reset();
        ModelTests.dropAllModels();
    }

    @Nested
    @DisplayName("dispatch events from one BC")
    class DispatchEvents {

        @Test
        @DisplayName("to entities with external subscribers of another BC")
        void toEntitiesOfBc() throws Exception {
            BoundedContext sourceContext = newContext();
            contextWithExtEntitySubscribers();

            assertNull(ProjectDetails.externalEvent());
            assertNull(ProjectWizard.externalEvent());
            assertNull(ProjectCountAggregate.externalEvent());

            Event event = projectCreated();
            sourceContext.eventBus()
                         .post(event);

            Message expectedMessage = unpack(event.getMessage());
            assertEquals(expectedMessage, ProjectDetails.externalEvent());
            assertEquals(expectedMessage, ProjectWizard.externalEvent());
            assertEquals(expectedMessage, ProjectCountAggregate.externalEvent());

            sourceContext.close();
        }

        @Test
        @DisplayName("to external subscribers of another BC")
        void toBcSubscribers() throws Exception {
            BoundedContext sourceContext = newContext();
            contextWithExternalSubscribers();

            assertNull(ProjectEventsSubscriber.externalEvent());
            assertNull(ProjectCommander.externalEvent());

            Event event = projectCreated();
            Message expectedMsg = unpack(event.getMessage());

            sourceContext.eventBus()
                         .post(event);
            assertThat(ProjectEventsSubscriber.externalEvent()).isEqualTo(expectedMsg);
            assertThat(ProjectCommander.externalEvent()).isEqualTo(expectedMsg);

            sourceContext.close();
        }

        @Test
        @DisplayName("to entities with external subscribers of multiple BCs")
        void toEntitiesOfMultipleBcs() throws Exception {
            BoundedContext sourceContext = newContext();

            BoundedContext destination1 = newContext();
            destination1.internalAccess()
                        .register(new MemoizingProjectDetailsRepo());

            BoundedContext destination2 = newContext();
            destination2.internalAccess()
                        .register(new AnotherMemoizingProjectDetailsRepo());

            assertTrue(MemoizingProjection.events()
                                          .isEmpty());
            Event event = projectCreated();
            sourceContext.eventBus()
                         .post(event);
            assertEquals(2, MemoizingProjection.events().size());
            sourceContext.close();
            destination1.close();
            destination2.close();
        }

        @Test
        @DisplayName("to two BCs with different needs")
        void twoBcSubscribers() throws Exception {
            BoundedContext sourceContext = newContext();
            BoundedContext destA = contextWithProjectCreatedNeeds();
            BoundedContext destB = contextWithProjectStartedNeeds();

            assertNull(ProjectStartedExtSubscriber.externalEvent());
            assertNull(ProjectEventsSubscriber.externalEvent());

            EventBus sourceEventBus = sourceContext.eventBus();
            Event created = projectCreated();
            sourceEventBus.post(created);
            Event started = projectStarted();
            sourceEventBus.post(started);
            assertThat(ProjectEventsSubscriber.externalEvent())
                    .isEqualTo(created.enclosedMessage());
            assertThat(ProjectStartedExtSubscriber.externalEvent())
                    .isEqualTo(started.enclosedMessage());
            sourceContext.close();
            destA.close();
            destB.close();
        }
    }

    @Nested
    @DisplayName("avoid dispatching events from a BC")
    class AvoidDispatching {

        @Test
        @DisplayName("to domestic entities subscribers of another BC")
        void toDomesticEntitySubscribers() throws Exception {
            BoundedContext sourceContext = newContext();
            BoundedContext destContext = contextWithExtEntitySubscribers();

            assertNull(ProjectDetails.domesticEvent());

            Event event = projectStarted();
            sourceContext.eventBus()
                         .post(event);
            assertThat(ProjectDetails.domesticEvent()).isNull();

            destContext.eventBus()
                       .post(event);
            assertThat(ProjectDetails.domesticEvent()).isEqualTo(unpack(event.getMessage()));

            sourceContext.close();
            destContext.close();
        }

        @Test
        @DisplayName("to domestic standalone subscribers of another BC")
        void toDomesticStandaloneSubscribers() throws Exception {
            BoundedContext sourceContext = newContext();
            BoundedContext destContext = contextWithExternalSubscribers();

            assertNull(ProjectEventsSubscriber.domesticEvent());
            assertNull(ProjectCommander.domesticEvent());

            Event projectStarted = projectStarted();
            sourceContext.eventBus()
                         .post(projectStarted);

            Message expectedEventMsg = unpack(projectStarted.getMessage());

            assertThat(ProjectEventsSubscriber.domesticEvent()).isNull();
            assertThat(ProjectCommander.domesticEvent()).isNull();

            destContext.eventBus()
                       .post(projectStarted);

            assertThat(ProjectEventsSubscriber.domesticEvent()).isEqualTo(expectedEventMsg);
            assertThat(ProjectCommander.domesticEvent()).isEqualTo(expectedEventMsg);

            sourceContext.close();
            destContext.close();
        }

        @Test
        @DisplayName("to own standalone subscribers if they expect external events")
        void toOwnExternalStandaloneSubscribers() throws Exception {
            BoundedContext destContext = contextWithExternalSubscribers();

            assertThat(ProjectEventsSubscriber.externalEvent()).isNull();
            assertThat(ProjectCommander.externalEvent()).isNull();

            Event projectCreated = projectCreated();
            destContext.eventBus()
                       .post(projectCreated);

            assertThat(ProjectEventsSubscriber.externalEvent()).isNull();
            assertThat(ProjectCommander.externalEvent()).isNull();

            destContext.close();
        }
    }

    @Test
    @DisplayName("send messages between two contexts regardless of registration order")
    void mutual() {
        String suffix = IntegrationBrokerTest.class.getSimpleName();
        BlackBox photos = BlackBox.from(
                BoundedContext.singleTenant("Photos-" + suffix)
                              .add(PhotosProcMan.class)
        );
        BlackBox billing = BlackBox.from(
                BoundedContext.singleTenant("Billing-" + suffix)
                              .add(BillingAggregate.class)
        );
        photos.receivesCommand(UploadPhotos.generate());
        assertReceived(photos, PhotosUploaded.class);
        assertReceived(billing, CreditsHeld.class);
        assertReceived(photos, PhotosProcessed.class);

        photos.close();
        billing.close();
    }

    private static void assertReceived(BlackBox context,
                                       Class<? extends EventMessage> eventClass) {
        context.assertEvents()
               .withType(eventClass)
               .hasSize(1);
    }

    @MuteLogging
    @Test
    @DisplayName("not dispatch to domestic subscribers if they requested external events")
    void notDispatchDomestic() throws Exception {
        BoundedContext context = contextWithExtEntitySubscribers();
        ProjectEventsSubscriber eventSubscriber = new ProjectEventsSubscriber();
        EventBus eventBus = context.eventBus();
        eventBus.register(eventSubscriber);

        assertNull(ProjectEventsSubscriber.externalEvent());
        assertNull(ProjectDetails.externalEvent());
        assertNull(ProjectWizard.externalEvent());
        assertNull(ProjectCountAggregate.externalEvent());

        Event projectCreated = projectCreated();
        eventBus.post(projectCreated);

        assertNull(ProjectEventsSubscriber.externalEvent());
        assertNull(ProjectDetails.externalEvent());
        assertNull(ProjectWizard.externalEvent());
        assertNull(ProjectCountAggregate.externalEvent());

        context.close();
    }

    @Test
    @DisplayName("emit unsupported external message exception if message type is unknown")
    void throwOnUnknownMessage() throws Exception {
        BoundedContext context = newContext();

        Event event = projectCreated();
        MemoizingObserver<Ack> observer = StreamObservers.memoizingObserver();
        context.internalAccess()
               .broker()
               .dispatchLocally(event, observer);
        Error error = observer.firstResponse()
                              .getStatus()
                              .getError();
        assertFalse(isDefault(error));
        assertEquals(EventValidationError.getDescriptor().getFullName(),
                     error.getType());
        assertEquals(UNSUPPORTED_EVENT_VALUE, error.getCode());

        context.close();
    }
}
