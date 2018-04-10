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
import io.spine.server.rejection.given.PostponedDispatcherRejectionDelivery;
import io.spine.server.rejection.given.RejectionMessageSubscriber;
import io.spine.server.rejection.given.VerifiableSubscriber;
import io.spine.test.rejection.command.RjStartProject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Set;

import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.rejection.given.Given.cannotModifyDeletedEntity;
import static io.spine.server.rejection.given.Given.invalidProjectNameRejection;
import static io.spine.server.rejection.given.Given.missingOwnerRejection;
import static io.spine.test.rejection.ProjectRejections.InvalidProjectName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"ClassWithTooManyMethods",
        "OverlyCoupledClass",
        "InstanceVariableNamingConvention"})
// OK as for the test class for one of the primary framework features
public class RejectionBusShould {

    private RejectionBus rejectionBus;

    @Before
    public void setUp() {
        this.rejectionBus = RejectionBus.newBuilder()
                                        .build();
    }

    @Test
    public void have_builder() {
        assertNotNull(RejectionBus.newBuilder());
    }

    @Test   // as the RejectionBus instances do not support enrichment yet.
    public void not_enrich_rejection_messages() {
        final Rejection original = invalidProjectNameRejection();
        final RejectionEnvelope enriched = rejectionBus.enrich(RejectionEnvelope.of(original));
        assertEquals(original, enriched.getOuterObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_object_with_no_subscriber_methods() {
        rejectionBus.register(new RejectionSubscriber());
    }

    @Test
    public void register_rejection_subscriber() {
        final RejectionSubscriber subscriberOne = new InvalidProjectNameSubscriber();
        final RejectionSubscriber subscriberTwo = new InvalidProjectNameSubscriber();

        rejectionBus.register(subscriberOne);
        rejectionBus.register(subscriberTwo);

        final RejectionClass rejectionClass = RejectionClass.of(InvalidProjectName.class);
        assertTrue(rejectionBus.hasDispatchers(rejectionClass));

        final Collection<RejectionDispatcher<?>> dispatchers = rejectionBus.getDispatchers(
                rejectionClass);
        assertTrue(dispatchers.contains(subscriberOne));
        assertTrue(dispatchers.contains(subscriberTwo));
    }

    @Test
    public void unregister_subscribers() {
        final RejectionSubscriber subscriberOne = new InvalidProjectNameSubscriber();
        final RejectionSubscriber subscriberTwo = new InvalidProjectNameSubscriber();
        rejectionBus.register(subscriberOne);
        rejectionBus.register(subscriberTwo);
        final RejectionClass rejectionClass = RejectionClass.of(
                InvalidProjectName.class);

        rejectionBus.unregister(subscriberOne);

        // Check that the 2nd subscriber with the same rejection subscriber method remains
        // after the 1st subscriber unregisters.
        final Collection<RejectionDispatcher<?>> subscribers =
                rejectionBus.getDispatchers(rejectionClass);
        assertFalse(subscribers.contains(subscriberOne));
        assertTrue(subscribers.contains(subscriberTwo));

        // Check that after 2nd subscriber us unregisters he's no longer in
        rejectionBus.unregister(subscriberTwo);

        assertFalse(rejectionBus.getDispatchers(rejectionClass)
                                .contains(subscriberTwo));
    }

    @Test
    public void call_subscriber_when_rejection_posted() {
        final InvalidProjectNameSubscriber subscriber = new InvalidProjectNameSubscriber();
        final Rejection rejection = invalidProjectNameRejection();
        rejectionBus.register(subscriber);

        rejectionBus.post(rejection);

        final Rejection handled = subscriber.getRejectionHandled();
        // Compare the content without command ID, which is different in the remembered
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
    public void call_subscriber_by_rejection_and_command_message_when_rejection_posted() {
        final MultipleRejectionSubscriber subscriber = new MultipleRejectionSubscriber();
        rejectionBus.register(subscriber);

        final Class<RjStartProject> commandMessageCls = RjStartProject.class;
        final Rejection rejection = cannotModifyDeletedEntity(commandMessageCls);
        rejectionBus.post(rejection);

        assertEquals(1, subscriber.numberOfSubscriberCalls());
        assertEquals(commandMessageCls, subscriber.commandMessageClass());
    }

    @Test
    public void call_subscriber_by_rejection_message_only() {
        final MultipleRejectionSubscriber subscriber = new MultipleRejectionSubscriber();
        rejectionBus.register(subscriber);

        final Rejection rejection = cannotModifyDeletedEntity(StringValue.class);
        rejectionBus.post(rejection);

        assertEquals(1, subscriber.numberOfSubscriberCalls());
        assertNull(subscriber.commandMessageClass());
    }

    @Test
    public void register_dispatchers() {
        final RejectionDispatcher<?> dispatcher = new BareDispatcher();

        rejectionBus.register(dispatcher);

        final RejectionClass rejectionClass = RejectionClass.of(InvalidProjectName.class);
        assertTrue(rejectionBus.getDispatchers(rejectionClass)
                               .contains(dispatcher));
    }

    @Test
    public void call_dispatchers() {
        final BareDispatcher dispatcher = new BareDispatcher();

        rejectionBus.register(dispatcher);

        rejectionBus.post(invalidProjectNameRejection());

        assertTrue(dispatcher.isDispatchCalled());
    }

    @Test
    public void unregister_dispatchers() {
        final RejectionDispatcher<?> dispatcherOne = new BareDispatcher();
        final RejectionDispatcher<?> dispatcherTwo = new BareDispatcher();
        final RejectionClass rejectionClass = RejectionClass.of(InvalidProjectName.class);
        rejectionBus.register(dispatcherOne);
        rejectionBus.register(dispatcherTwo);

        rejectionBus.unregister(dispatcherOne);
        final Set<RejectionDispatcher<?>> dispatchers = rejectionBus.getDispatchers(rejectionClass);

        // Check we don't have 1st dispatcher, but have 2nd.
        assertFalse(dispatchers.contains(dispatcherOne));
        assertTrue(dispatchers.contains(dispatcherTwo));

        rejectionBus.unregister(dispatcherTwo);
        assertFalse(rejectionBus.getDispatchers(rejectionClass)
                                .contains(dispatcherTwo));
    }

    @Test
    public void catch_exceptions_caused_by_subscribers() {
        final VerifiableSubscriber faultySubscriber = new FaultySubscriber();

        rejectionBus.register(faultySubscriber);
        rejectionBus.post(invalidProjectNameRejection());

        assertTrue(faultySubscriber.isMethodCalled());
    }

    @Test
    public void unregister_registries_on_close() throws Exception {
        final RejectionBus rejectionBus = RejectionBus.newBuilder()
                                                      .build();
        rejectionBus.register(new BareDispatcher());
        rejectionBus.register(new InvalidProjectNameSubscriber());
        final RejectionClass rejectionClass = RejectionClass.of(InvalidProjectName.class);

        rejectionBus.close();

        assertTrue(rejectionBus.getDispatchers(rejectionClass)
                               .isEmpty());
    }

    @Test
    public void support_short_form_subscriber_methods() {
        final RejectionMessageSubscriber subscriber = new RejectionMessageSubscriber();
        checkRejection(subscriber);
    }

    @Test
    public void support_context_aware_subscriber_methods() {
        final ContextAwareSubscriber subscriber = new ContextAwareSubscriber();
        checkRejection(subscriber);
    }

    @Test
    public void support_command_msg_aware_subscriber_methods() {
        final CommandMessageAwareSubscriber subscriber = new CommandMessageAwareSubscriber();
        checkRejection(subscriber);
    }

    @Test
    public void support_command_aware_subscriber_methods() {
        final CommandAwareSubscriber subscriber = new CommandAwareSubscriber();
        checkRejection(subscriber);
    }

    @Test(
            expected = IllegalArgumentException.class
                // In Bus ->  No message types are forwarded by this dispatcher.
    )
    public void not_support_subscriber_methods_with_wrong_parameter_sequence() {
        final RejectionDispatcher<?> subscriber = new InvalidOrderSubscriber();

        rejectionBus.register(subscriber);
        rejectionBus.post(missingOwnerRejection());
    }

    @Test
    public void report_dead_messages() {
        final MemoizingObserver<Ack> observer = memoizingObserver();
        rejectionBus.post(missingOwnerRejection(), observer);
        assertTrue(observer.isCompleted());
        final Ack result = observer.firstResponse();
        assertNotNull(result);
        assertEquals(ERROR, result.getStatus().getStatusCase());
        final Error error = result.getStatus().getError();
        assertEquals(UnhandledRejectionException.class.getCanonicalName(), error.getType());
    }

    @Test
    public void have_log() {
        assertNotNull(RejectionBus.log());
    }

    private void checkRejection(VerifiableSubscriber subscriber) {
        final Rejection rejection = missingOwnerRejection();
        rejectionBus.register(subscriber);
        rejectionBus.post(rejection);

        assertTrue(subscriber.isMethodCalled());
        subscriber.verifyGot(rejection);
    }

}
