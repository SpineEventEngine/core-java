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
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.EventValidationError;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.event.EventBus;
import io.spine.server.integration.given.IntegrationBrokerTestEnv;
import io.spine.server.integration.given.MemoizingProjectDetails1Repository;
import io.spine.server.integration.given.MemoizingProjectDetails2Repository;
import io.spine.server.integration.given.MemoizingProjection;
import io.spine.server.integration.given.ProjectCommander;
import io.spine.server.integration.given.ProjectCountAggregate;
import io.spine.server.integration.given.ProjectDetails;
import io.spine.server.integration.given.ProjectEventsSubscriber;
import io.spine.server.integration.given.ProjectStartedExtSubscriber;
import io.spine.server.integration.given.ProjectWizard;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxContext;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.pack;
import static io.spine.core.EventValidationError.UNSUPPORTED_EVENT_VALUE;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv._projectCreated;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.contextWithExternalEntitySubscribers;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.contextWithExternalSubscribers;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.contextWithProjectCreatedNeeds;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.contextWithProjectStartedNeeds;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.createBillingBcWithSubscribers;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.createEmptyBc;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.createPhotosBcWithSubscribers;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.createProjectsBc;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.createProjectsBcWithSubscribers;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.createUsersBc;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.newContext;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.projectId;
import static io.spine.server.integration.given.IntegrationBrokerTestEnv.projectStarted;
import static io.spine.testing.server.TestEventFactory.newInstance;
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
    @DisplayName("dispatch events")
    class DispatchEvents {

        @Nested
        @DisplayName("from a BC to subscribers of external events in")
        class FromOneBc {

            @Test
            @DisplayName("another BC")
            void toAnotherBc() {
                BlackBoxContext publishingUsersBc = createUsersBc();
                BlackBoxContext subscribedProjectsBc = createProjectsBc();


            }

            @Test
            @DisplayName("multiple other BCs")
            void toMultipleOtherBc() throws Exception {
                BoundedContext sourceContext = newContext();

                BoundedContext destination1 = newContext();
                destination1.internalAccess()
                            .register(new MemoizingProjectDetails1Repository());

                BoundedContext destination2 = newContext();
                destination2.internalAccess()
                            .register(new MemoizingProjectDetails2Repository());

                assertTrue(MemoizingProjection.events()
                                              .isEmpty());
                Event event = projectCreated();
                sourceContext.eventBus()
                             .post(event);
                assertEquals(2, MemoizingProjection.events()
                                                   .size());
                sourceContext.close();
                destination1.close();
                destination2.close();
            }

            @Test
            @DisplayName("multiple other BCs with different needs")
            void toMultipleOtherBcWithDifferentNeeds() throws Exception {
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
        @DisplayName("between two BCs regardless of their registration order when")
        class RegardlessBcRegistrationOrder {

            @Nested
            @DisplayName("the subscribing BC is registered")
            class WhenSubscribingBcRegistered {

                @Test
                @DisplayName("after the publishing one")
                void afterThePublishingOne() {
                    BlackBoxContext publishingBc = createEmptyBc();
                    BlackBoxContext subscribedProjectsBc = createProjectsBcWithSubscribers();

                    assertNull(ProjectDetails.externalEvent());
                    assertNull(ProjectWizard.externalEvent());
                    assertNull(ProjectCountAggregate.externalEvent());

                    ItgProjectCreated eventMessage = _projectCreated();

                    TestEventFactory eventFactory = newInstance(
                            pack(projectId()),
                            IntegrationBrokerTestEnv.class
                    );

                    Event event = eventFactory.createEvent(eventMessage);
                    publishingBc.receivesEvent(eventMessage);
//            assertEquals(eventMessage, ProjectDetails.externalEvent());
//            assertEquals(event, ProjectWizard.externalEvent());
//            assertEquals(event, ProjectCountAggregate.externalEvent());

//            subscribedProjectsBc.close();
//            publishingBc.close();
                }

                @Test
                @DisplayName("before the publishing one")
                void beforeThePublishingOne() throws Exception {
                    contextWithExternalEntitySubscribers();
                    BoundedContext sourceContext = newContext();

                    assertNull(ProjectDetails.externalEvent());
                    assertNull(ProjectWizard.externalEvent());
                    assertNull(ProjectCountAggregate.externalEvent());

                    Event event = projectCreated();
                    sourceContext.eventBus()
                                 .post(event);

                    Message expectedMessage = event.enclosedMessage();
                    assertEquals(expectedMessage, ProjectDetails.externalEvent());
                    assertEquals(expectedMessage, ProjectWizard.externalEvent());
                    assertEquals(expectedMessage, ProjectCountAggregate.externalEvent());

                    sourceContext.close();
                }

            }

            @Nested
            @DisplayName("they are subscribed to each other and registered in")
            class WhenMutuallySubscribedAndRegistered {

                @Test
                @DisplayName("straight order")
                void inStraightOrder() {
                    BlackBoxContext photosBc = createPhotosBcWithSubscribers();
                    BlackBoxContext billingBc = createBillingBcWithSubscribers();

                    photosBc.receivesCommand(UploadPhotos.generate());

                    assertDispatched(photosBc, billingBc);
                }

                @Test
                @DisplayName("reverse order")
                void inReverseOrder() {
                    BlackBoxContext billingBc = createBillingBcWithSubscribers();
                    BlackBoxContext photosBc = createPhotosBcWithSubscribers();

                    photosBc.receivesCommand(UploadPhotos.generate());

                    assertDispatched(photosBc, billingBc);
                }

                private void assertDispatched(BlackBoxContext photos, BlackBoxContext billing) {
                    photos.assertEvent(PhotosUploaded.class);
                    billing.assertEvent(CreditsHeld.class);
                    photos.assertEvent(PhotosProcessed.class);

                    photos.close();
                    billing.close();
                }

            }

        }

    }

    @Nested
    @DisplayName("avoid dispatching events from a BC to")
    class AvoidDispatchingEvents {

        @Test
        @DisplayName("subscribers of domestic events in another BC")
        void toSubscribersOfDomesticEventsInAnotherBc() throws Exception {
            BoundedContext sourceContext = newContext();
            BoundedContext destContext = contextWithExternalEntitySubscribers();

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
        @DisplayName("its own subscribers of external events")
        void toItsOwnSubscribersOfExternalEvents() throws Exception {
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
    @DisplayName("emit EventValidationError.UNSUPPORTED_EVENT_VALUE if an event type is unknown")
    void throwOnUnknownEventPassed() throws Exception {
        BoundedContext context = newContext();
        Event event = projectCreated();

        MemoizingObserver<Ack> observer = StreamObservers.memoizingObserver();
        context.internalAccess()
               .broker()
               .dispatchLocally(event, observer);

        Error error = observer.firstResponse()
                              .getStatus()
                              .getError();

        assertFalse(isDefault(error)); // what is checked here ?
        assertEquals(UNSUPPORTED_EVENT_VALUE, error.getCode());
        assertEquals(
                EventValidationError.getDescriptor().getFullName(),
                error.getType()
        );

        context.close();
    }

}
