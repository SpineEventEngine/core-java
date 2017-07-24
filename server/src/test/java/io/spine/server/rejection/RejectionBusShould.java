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
package io.spine.server.rejection;

import io.spine.base.Error;
import io.spine.change.StringChange;
import io.spine.client.CommandFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Commands;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.core.Subscribe;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.commandbus.Given;
import io.spine.test.rejection.ProjectId;
import io.spine.test.rejection.command.RemoveOwner;
import io.spine.test.rejection.command.UpdateProjectName;
import io.spine.testdata.Sample;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.collect.Maps.newHashMap;
import static io.spine.Identifier.newUuid;
import static io.spine.core.Rejections.getMessage;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.test.rejection.ProjectRejections.InvalidProjectName;
import static io.spine.test.rejection.ProjectRejections.MissingOwner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
    // OK as for the test class for one of the primary framework features
public class RejectionBusShould {

    private RejectionBus rejectionBus;
    private PostponedDispatcherRejectionDelivery postponedDelivery;
    private Executor delegateDispatcherExecutor;
    private RejectionBus rejectionBusWithPostponedExecution;

    @Before
    public void setUp() {
        this.rejectionBus = RejectionBus.newBuilder()
                                        .build();
        this.delegateDispatcherExecutor = spy(directExecutor());
        this.postponedDelivery =
                new PostponedDispatcherRejectionDelivery(delegateDispatcherExecutor);
        this.rejectionBusWithPostponedExecution =
                RejectionBus.newBuilder()
                            .setDispatcherRejectionDelivery(
                                  postponedDelivery)
                            .build();
    }

    @SuppressWarnings("MethodMayBeStatic")   /* it cannot, as its result is used in {@code org.mockito.Mockito.spy() */
    private Executor directExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    @Test
    public void have_builder() {
        assertNotNull(RejectionBus.newBuilder());
    }

    @Test
    public void return_associated_DispatcherDelivery() {
        final DispatcherRejectionDelivery delivery = mock(DispatcherRejectionDelivery.class);
        final RejectionBus result = RejectionBus.newBuilder()
                                                .setDispatcherRejectionDelivery(delivery)
                                                .build();
        assertEquals(delivery, result.delivery());
    }

    @Test
    public void return_direct_DispatcherDelivery_if_none_customized() {
        final DispatcherRejectionDelivery actual = rejectionBus.delivery();
        assertTrue(actual instanceof DispatcherRejectionDelivery.DirectDelivery);
    }

    @Test   // as the FailureBus instances do not support enrichment yet.
    public void not_enrich_failure_messages() {
        final Rejection original = Rejection.getDefaultInstance();
        final Rejection enriched = rejectionBus.enrich(original);
        assertEquals(original, enriched);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_object_with_no_subscriber_methods() {
        rejectionBus.register(new RejectionSubscriber());
    }

    @Test
    public void register_failure_subscriber() {
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

        // Check that the 2nd subscriber with the same failure subscriber method remains
        // after the 1st subscriber unregisters.
        final Collection<RejectionDispatcher<?>> subscribers = rejectionBus.getDispatchers(
                rejectionClass);
        assertFalse(subscribers.contains(subscriberOne));
        assertTrue(subscribers.contains(subscriberTwo));

        // Check that after 2nd subscriber us unregisters he's no longer in
        rejectionBus.unregister(subscriberTwo);

        assertFalse(rejectionBus.getDispatchers(rejectionClass)
                                .contains(subscriberTwo));
    }

    @Test
    public void call_subscriber_when_failure_posted() {
        final InvalidProjectNameSubscriber subscriber = new InvalidProjectNameSubscriber();
        final Rejection rejection = invalidProjectNameFailure();
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

        rejectionBus.post(invalidProjectNameFailure());

        assertTrue(dispatcher.isDispatchCalled());
    }

    @Test
    public void not_call_dispatchers_if_dispatcher_failure_execution_postponed() {
        final BareDispatcher dispatcher = new BareDispatcher();

        rejectionBusWithPostponedExecution.register(dispatcher);

        final Rejection rejection = invalidProjectNameFailure();
        rejectionBusWithPostponedExecution.post(rejection);
        assertFalse(dispatcher.isDispatchCalled());

        final boolean failurePostponed = postponedDelivery.isPostponed(rejection, dispatcher);
        assertTrue(failurePostponed);
    }

    @Test
    public void deliver_postponed_failure_to_dispatcher_using_configured_executor() {
        final BareDispatcher dispatcher = new BareDispatcher();

        rejectionBusWithPostponedExecution.register(dispatcher);

        final Rejection rejection = invalidProjectNameFailure();
        rejectionBusWithPostponedExecution.post(rejection);
        final Set<RejectionEnvelope> postponedFailures = postponedDelivery.getPostponedFailures();
        final RejectionEnvelope postponedFailure = postponedFailures.iterator()
                                                                    .next();
        verify(delegateDispatcherExecutor, never()).execute(any(Runnable.class));
        postponedDelivery.deliverNow(postponedFailure, dispatcher.getClass());
        assertTrue(dispatcher.isDispatchCalled());
        verify(delegateDispatcherExecutor).execute(any(Runnable.class));
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
        rejectionBus.post(invalidProjectNameFailure());

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
        checkFailure(subscriber);
    }

    @Test
    public void support_context_aware_subscriber_methods() {
        final ContextAwareSubscriber subscriber = new ContextAwareSubscriber();
        checkFailure(subscriber);
    }

    @Test
    public void support_command_msg_aware_subscriber_methods() {
        final CommandMessageAwareSubscriber subscriber = new CommandMessageAwareSubscriber();
        checkFailure(subscriber);
    }

    @Test
    public void support_command_aware_subscriber_methods() {
        final CommandAwareSubscriber subscriber = new CommandAwareSubscriber();
        checkFailure(subscriber);
    }

    @Test(
            expected = IllegalArgumentException.class
                // In Bus ->  No message types are forwarded by this dispatcher.
    )
    public void not_support_subscriber_methods_with_wrong_parameter_sequence() {
        final RejectionDispatcher<?> subscriber = new InvalidOrderSubscriber();

        rejectionBus.register(subscriber);
        rejectionBus.post(missingOwnerFailure());
    }

    @Test
    public void report_dead_messages() {
        final MemoizingObserver<Ack> observer = memoizingObserver();
        rejectionBus.post(missingOwnerFailure(), observer);
        assertTrue(observer.isCompleted());
        final Ack result = observer.firstResponse();
        assertNotNull(result);
        assertEquals(ERROR, result.getStatus().getStatusCase());
        final Error error = result.getStatus().getError();
        assertEquals(UnhandledFailureException.class.getCanonicalName(), error.getType());
    }

    @Test
    public void have_log() {
        assertNotNull(RejectionBus.log());
    }

    private void checkFailure(VerifiableSubscriber subscriber) {
        final Rejection rejection = missingOwnerFailure();
        rejectionBus.register(subscriber);
        rejectionBus.post(rejection);

        assertTrue(subscriber.isMethodCalled());
        subscriber.verifyGot(rejection);
    }

    private static Rejection invalidProjectNameFailure() {
        final ProjectId projectId = newProjectId();
        final InvalidProjectName invalidProjectName =
                InvalidProjectName.newBuilder()
                                  .setProjectId(projectId)
                                  .build();
        final StringChange nameChange = StringChange.newBuilder()
                                                    .setNewValue("Too short")
                                                    .build();
        final UpdateProjectName updateProjectName = UpdateProjectName.newBuilder()
                                                                     .setId(projectId)
                                                                     .setNameUpdate(nameChange)
                                                                     .build();

        final TenantId generatedTenantId = TenantId.newBuilder()
                                                   .setValue(newUuid())
                                                   .build();
        final TestActorRequestFactory factory =
                TestActorRequestFactory.newInstance(RejectionBusShould.class, generatedTenantId);
        final Command command = factory.createCommand(updateProjectName);
        return Rejections.createRejection(invalidProjectName, command);
    }

    private static Rejection missingOwnerFailure() {
        final ProjectId projectId = newProjectId();
        final MissingOwner msg = MissingOwner.newBuilder()
                                             .setProjectId(projectId)
                                             .build();
        final Command command = Given.ACommand.withMessage(Sample.messageOfType(RemoveOwner.class));
        return Rejections.createRejection(msg, command);
    }

    private static ProjectId newProjectId() {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(newUuid())
                                             .build();
        return projectId;
    }

    /**
     * A simple dispatcher class, which only dispatch and does not have own failure
     * subscribing methods.
     */
    private static class BareDispatcher implements RejectionDispatcher<String> {

        private boolean dispatchCalled = false;

        @Override
        public Set<RejectionClass> getMessageClasses() {
            return RejectionClass.setOf(InvalidProjectName.class);
        }

        @Override
        public Set<String> dispatch(RejectionEnvelope failure) {
            dispatchCalled = true;
            return Identity.of(this);
        }

        private boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }

    private static class InvalidProjectNameSubscriber extends RejectionSubscriber {

        private Rejection rejectionHandled;

        @Subscribe
        public void on(InvalidProjectName failure,
                       UpdateProjectName commandMessage,
                       CommandContext context) {
            final CommandFactory commandFactory =
                    TestActorRequestFactory.newInstance(InvalidProjectNameSubscriber.class)
                                           .command();
            final Command command = commandFactory.createWithContext(commandMessage, context);
            this.rejectionHandled = Rejections.createRejection(failure, command);
        }

        private Rejection getRejectionHandled() {
            return rejectionHandled;
        }
    }

    private static class PostponedDispatcherRejectionDelivery extends DispatcherRejectionDelivery {

        private final Map<RejectionEnvelope,
                Class<? extends RejectionDispatcher>> postponedExecutions = newHashMap();

        private PostponedDispatcherRejectionDelivery(Executor delegate) {
            super(delegate);
        }

        @Override
        public boolean shouldPostponeDelivery(RejectionEnvelope failure,
                                              RejectionDispatcher<?> consumer) {
            postponedExecutions.put(failure, consumer.getClass());
            return true;
        }

        private boolean isPostponed(Rejection rejection, RejectionDispatcher<?> dispatcher) {
            final RejectionEnvelope envelope = RejectionEnvelope.of(rejection);
            final Class<? extends RejectionDispatcher> actualClass = postponedExecutions.get(
                    envelope);
            final boolean failurePostponed = actualClass != null;
            final boolean dispatcherMatches = failurePostponed && dispatcher.getClass()
                                                                            .equals(actualClass);
            return dispatcherMatches;
        }

        private Set<RejectionEnvelope> getPostponedFailures() {
            final Set<RejectionEnvelope> envelopes = postponedExecutions.keySet();
            return envelopes;
        }
    }

    private abstract static class VerifiableSubscriber extends RejectionSubscriber {

        private boolean methodCalled = false;

        void triggerCall() {
            methodCalled = true;
        }

        private boolean isMethodCalled() {
            return methodCalled;
        }

        abstract void verifyGot(Rejection rejection);
    }

    /** The subscriber which throws exception from the subscriber method. */
    private static class FaultySubscriber extends VerifiableSubscriber {

        @SuppressWarnings("unused") // It's fine for a faulty subscriber.
        @Subscribe
        public void on(InvalidProjectName failure, CommandContext context) {
            triggerCall();
            throw new UnsupportedOperationException(
                    "Faulty subscriber should have failed: " +
                            FaultySubscriber.class.getSimpleName());
        }

        @Override
        void verifyGot(Rejection ignored) {
            fail("FaultySubscriber");
        }
    }

    private static class RejectionMessageSubscriber extends VerifiableSubscriber {

        private MissingOwner failure;

        @Subscribe
        public void on(MissingOwner failure) {
            triggerCall();
            this.failure = failure;
        }

        @Override
        void verifyGot(Rejection rejection) {
            assertEquals(getMessage(rejection), this.failure);
        }
    }

    private static class ContextAwareSubscriber extends VerifiableSubscriber {

        private MissingOwner failure;
        private CommandContext context;

        @Subscribe
        public void on(MissingOwner failure, CommandContext context) {
            triggerCall();
            this.failure = failure;
            this.context = context;
        }

        @Override
        void verifyGot(Rejection rejection) {
            assertEquals(getMessage(rejection), this.failure);
            assertEquals(rejection.getContext().getCommand().getContext(), context);
        }
    }

    private static class CommandMessageAwareSubscriber extends VerifiableSubscriber {

        private MissingOwner failure;
        private RemoveOwner command;

        @Subscribe
        public void on(MissingOwner failure, RemoveOwner command) {
            triggerCall();
            this.failure = failure;
            this.command = command;
        }

        @Override
        void verifyGot(Rejection rejection) {
            assertEquals(getMessage(rejection), this.failure);
            assertEquals(Commands.getMessage(rejection.getContext().getCommand()), command);
        }
    }

    private static class CommandAwareSubscriber extends VerifiableSubscriber {

        private MissingOwner failure;
        private RemoveOwner command;
        private CommandContext context;

        @Subscribe
        public void on(MissingOwner failure,
                       RemoveOwner command,
                       CommandContext context) {
            triggerCall();
            this.failure = failure;
            this.command = command;
            this.context = context;
        }

        @Override
        void verifyGot(Rejection rejection) {
            assertEquals(getMessage(rejection), this.failure);
            assertEquals(Commands.getMessage(rejection.getContext().getCommand()), command);
            assertEquals(rejection.getContext().getCommand().getContext(), context);
        }
    }

    private static class InvalidOrderSubscriber extends VerifiableSubscriber {

        @SuppressWarnings("unused") // The method should never be invoked, so the params are unused.
        @Subscribe
        public void on(MissingOwner failure,
                       CommandContext context,
                       RemoveOwner command) {
            triggerCall();
            fail("InvalidOrderSubscriber invoked the handler method");
        }

        @Override
        void verifyGot(Rejection rejection) {
            fail("InvalidOrderSubscriber");
        }
    }
}
