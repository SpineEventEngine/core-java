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
package io.spine.server.rejection;

import com.google.protobuf.StringValue;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.rejection.given.BareDispatcher;
import io.spine.server.rejection.given.CommandAwareSubscriber;
import io.spine.server.rejection.given.CommandMessageAwareSubscriber;
import io.spine.server.rejection.given.ContextAwareSubscriber;
import io.spine.server.rejection.given.FaultySubscriber;
import io.spine.server.rejection.given.InvalidOrderSubscriber;
import io.spine.server.rejection.given.InvalidProjectNameSubscriber;
import io.spine.server.rejection.given.MultipleRejectionSubscriber;
import io.spine.server.rejection.given.RejectionMessageSubscriber;
import io.spine.server.rejection.given.VerifiableSubscriber;
import io.spine.test.rejection.command.RjStartProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.rejection.given.Given.cannotModifyDeletedEntity;
import static io.spine.server.rejection.given.Given.invalidProjectNameRejection;
import static io.spine.server.rejection.given.Given.missingOwnerRejection;
import static io.spine.test.rejection.ProjectRejections.InvalidProjectName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("RejectionBus should")
public class RejectionBusTest {

    private RejectionBus rejectionBus;

    @BeforeEach
    void setUp() {
        this.rejectionBus = RejectionBus.newBuilder()
                                        .build();
    }

    @Test
    @DisplayName("have builder")
    void haveBuilder() {
        assertNotNull(RejectionBus.newBuilder());
    }

    @Test   // As the RejectionBus instances do not support enrichment yet.
    @DisplayName("not enrich rejection messages")
    void notEnrichRejectionMessages() {
        Rejection original = invalidProjectNameRejection();
        RejectionEnvelope enriched = rejectionBus.enrich(RejectionEnvelope.of(original));
        assertEquals(original, enriched.getOuterObject());
    }

    @Test
    @DisplayName("reject object with no subscriber methods")
    void rejectIfNoSubscribers() {
        assertThrows(IllegalArgumentException.class,
                     () -> rejectionBus.register(new RejectionSubscriber()));
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("subscriber")
        void subscriber() {
            RejectionSubscriber subscriberOne = new InvalidProjectNameSubscriber();
            RejectionSubscriber subscriberTwo = new InvalidProjectNameSubscriber();

            rejectionBus.register(subscriberOne);
            rejectionBus.register(subscriberTwo);

            RejectionClass rejectionClass = RejectionClass.of(InvalidProjectName.class);
            assertTrue(rejectionBus.hasDispatchers(rejectionClass));

            Collection<RejectionDispatcher<?>> dispatchers = rejectionBus.getDispatchers(
                    rejectionClass);
            assertTrue(dispatchers.contains(subscriberOne));
            assertTrue(dispatchers.contains(subscriberTwo));
        }

        @Test
        @DisplayName("dispatcher")
        void dispatcher() {
            RejectionDispatcher<?> dispatcher = new BareDispatcher();

            rejectionBus.register(dispatcher);

            RejectionClass rejectionClass = RejectionClass.of(InvalidProjectName.class);
            assertTrue(rejectionBus.getDispatchers(rejectionClass)
                                   .contains(dispatcher));
        }
    }

    @Nested
    @DisplayName("unregister")
    class Unregister {

        @Test
        @DisplayName("subscriber")
        void subscriber() {
            RejectionSubscriber subscriberOne = new InvalidProjectNameSubscriber();
            RejectionSubscriber subscriberTwo = new InvalidProjectNameSubscriber();
            rejectionBus.register(subscriberOne);
            rejectionBus.register(subscriberTwo);
            RejectionClass rejectionClass = RejectionClass.of(
                    InvalidProjectName.class);

            rejectionBus.unregister(subscriberOne);

            // Check that the 2nd subscriber with the same rejection subscriber method remains
            // after the 1st subscriber unregisters.
            Collection<RejectionDispatcher<?>> subscribers =
                    rejectionBus.getDispatchers(rejectionClass);
            assertFalse(subscribers.contains(subscriberOne));
            assertTrue(subscribers.contains(subscriberTwo));

            // Check that after 2nd subscriber is unregistered, he's no longer in.
            rejectionBus.unregister(subscriberTwo);

            assertFalse(rejectionBus.getDispatchers(rejectionClass)
                                    .contains(subscriberTwo));
        }

        @Test
        @DisplayName("dispatcher")
        void dispatcher() {
            RejectionDispatcher<?> dispatcherOne = new BareDispatcher();
            RejectionDispatcher<?> dispatcherTwo = new BareDispatcher();
            RejectionClass rejectionClass = RejectionClass.of(InvalidProjectName.class);
            rejectionBus.register(dispatcherOne);
            rejectionBus.register(dispatcherTwo);

            rejectionBus.unregister(dispatcherOne);
            Set<RejectionDispatcher<?>> dispatchers =
                    rejectionBus.getDispatchers(rejectionClass);

            // Check we don't have 1st dispatcher, but have 2nd.
            assertFalse(dispatchers.contains(dispatcherOne));
            assertTrue(dispatchers.contains(dispatcherTwo));

            rejectionBus.unregister(dispatcherTwo);
            assertFalse(rejectionBus.getDispatchers(rejectionClass)
                                    .contains(dispatcherTwo));
        }
    }

    @Test
    @DisplayName("unregister registries on close")
    void unregisterAllOnClose() throws Exception {
        RejectionBus rejectionBus = RejectionBus.newBuilder()
                                                .build();
        rejectionBus.register(new BareDispatcher());
        rejectionBus.register(new InvalidProjectNameSubscriber());
        RejectionClass rejectionClass = RejectionClass.of(InvalidProjectName.class);

        rejectionBus.close();

        assertTrue(rejectionBus.getDispatchers(rejectionClass)
                               .isEmpty());
    }

    @Nested
    @DisplayName("call")
    class Call {

        @Test
        @DisplayName("subscriber")
        void subscriber() {
            InvalidProjectNameSubscriber subscriber = new InvalidProjectNameSubscriber();
            Rejection rejection = invalidProjectNameRejection();
            rejectionBus.register(subscriber);

            rejectionBus.post(rejection);

            Rejection handled = subscriber.getRejectionHandled();
            // Compare the content without command ID, which is different in the remembered.
            assertEquals(rejection.getMessage(), handled.getMessage());
            assertEquals(rejection.getContext()
                                  .getCommand()
                                  .getMessage(),
                         handled.getContext()
                                .getCommand()
                                .getMessage());
            assertEquals(rejection.getContext()
                                  .getCommand()
                                  .getContext(),
                         handled.getContext()
                                .getCommand()
                                .getContext());
        }

        @Test
        @DisplayName("dispatcher")
        void dispatcher() {
            BareDispatcher dispatcher = new BareDispatcher();

            rejectionBus.register(dispatcher);

            rejectionBus.post(invalidProjectNameRejection());

            assertTrue(dispatcher.isDispatchCalled());
        }
    }

    @Nested
    @DisplayName("call subscriber by")
    class CallSubscriberBy {

        @Test
        @DisplayName("rejection message only")
        void rejectionOnly() {
            MultipleRejectionSubscriber subscriber = new MultipleRejectionSubscriber();
            rejectionBus.register(subscriber);

            Rejection rejection = cannotModifyDeletedEntity(StringValue.class);
            rejectionBus.post(rejection);

            assertEquals(1, subscriber.numberOfSubscriberCalls());
            assertNull(subscriber.commandMessageClass());
        }

        @Test
        @DisplayName("rejection and command message")
        void rejectionAndCommandMessage() {
            MultipleRejectionSubscriber subscriber = new MultipleRejectionSubscriber();
            rejectionBus.register(subscriber);

            Class<RjStartProject> commandMessageCls = RjStartProject.class;
            Rejection rejection = cannotModifyDeletedEntity(commandMessageCls);
            rejectionBus.post(rejection);

            assertEquals(1, subscriber.numberOfSubscriberCalls());
            assertEquals(commandMessageCls, subscriber.commandMessageClass());
        }
    }

    @Test
    @DisplayName("catch exceptions caused by subscribers")
    void catchSubscriberExceptions() {
        VerifiableSubscriber faultySubscriber = new FaultySubscriber();

        rejectionBus.register(faultySubscriber);
        rejectionBus.post(invalidProjectNameRejection());

        assertTrue(faultySubscriber.isMethodCalled());
    }

    @Nested
    @DisplayName("support subscriber methods which are")
    class SupportSubscriberMethods {

        @Test
        @DisplayName("short form")
        void shortForm() {
            RejectionMessageSubscriber subscriber = new RejectionMessageSubscriber();
            checkRejection(subscriber);
        }

        @Test
        @DisplayName("context aware")
        void contextAware() {
            ContextAwareSubscriber subscriber = new ContextAwareSubscriber();
            checkRejection(subscriber);
        }

        @Test
        @DisplayName("command message aware")
        void commandMessageAware() {
            CommandMessageAwareSubscriber subscriber = new CommandMessageAwareSubscriber();
            checkRejection(subscriber);
        }

        @Test
        @DisplayName("command aware")
        void commandAware() {
            CommandAwareSubscriber subscriber = new CommandAwareSubscriber();
            checkRejection(subscriber);
        }

        private void checkRejection(VerifiableSubscriber subscriber) {
            Rejection rejection = missingOwnerRejection();
            rejectionBus.register(subscriber);
            rejectionBus.post(rejection);

            assertTrue(subscriber.isMethodCalled());
            subscriber.verifyGot(rejection);
        }
    }

    @Test
    @DisplayName("not support subscriber methods with wrong parameter sequence")
    void rejectWrongArgSequence() {
        RejectionDispatcher<?> subscriber = new InvalidOrderSubscriber();

        // In Bus ->  No message types are forwarded by this dispatcher.
        assertThrows(IllegalArgumentException.class,
                     () -> rejectionBus.register(subscriber));
    }

    @Test
    @DisplayName("report dead messages")
    void reportDeadMessages() {
        MemoizingObserver<Ack> observer = memoizingObserver();
        rejectionBus.post(missingOwnerRejection(), observer);
        assertTrue(observer.isCompleted());
        Ack result = observer.firstResponse();
        assertNotNull(result);
        assertEquals(ERROR, result.getStatus().getStatusCase());
        Error error = result.getStatus()
                            .getError();
        assertEquals(UnhandledRejectionException.class.getCanonicalName(), error.getType());
    }

    @Test
    @DisplayName("have log")
    void haveLog() {
        assertNotNull(RejectionBus.log());
    }
}
