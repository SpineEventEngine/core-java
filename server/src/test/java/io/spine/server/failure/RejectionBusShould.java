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
package io.spine.server.failure;

import io.spine.base.Error;
import io.spine.change.StringChange;
import io.spine.client.CommandFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Commands;
import io.spine.core.FailureClass;
import io.spine.core.FailureEnvelope;
import io.spine.core.Rejection;
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

    private FailureBus failureBus;
    private PostponedDispatcherFailureDelivery postponedDelivery;
    private Executor delegateDispatcherExecutor;
    private FailureBus failureBusWithPostponedExecution;

    @Before
    public void setUp() {
        this.failureBus = FailureBus.newBuilder()
                                    .build();
        this.delegateDispatcherExecutor = spy(directExecutor());
        this.postponedDelivery =
                new PostponedDispatcherFailureDelivery(delegateDispatcherExecutor);
        this.failureBusWithPostponedExecution =
                FailureBus.newBuilder()
                          .setDispatcherFailureDelivery(
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
        assertNotNull(FailureBus.newBuilder());
    }

    @Test
    public void return_associated_DispatcherDelivery() {
        final DispatcherFailureDelivery delivery = mock(DispatcherFailureDelivery.class);
        final FailureBus result = FailureBus.newBuilder()
                                            .setDispatcherFailureDelivery(delivery)
                                            .build();
        assertEquals(delivery, result.delivery());
    }

    @Test
    public void return_direct_DispatcherDelivery_if_none_customized() {
        final DispatcherFailureDelivery actual = failureBus.delivery();
        assertTrue(actual instanceof DispatcherFailureDelivery.DirectDelivery);
    }

    @Test   // as the FailureBus instances do not support enrichment yet.
    public void not_enrich_failure_messages() {
        final Rejection original = Rejection.getDefaultInstance();
        final Rejection enriched = failureBus.enrich(original);
        assertEquals(original, enriched);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_object_with_no_subscriber_methods() {
        failureBus.register(new FailureSubscriber());
    }

    @Test
    public void register_failure_subscriber() {
        final FailureSubscriber subscriberOne = new InvalidProjectNameSubscriber();
        final FailureSubscriber subscriberTwo = new InvalidProjectNameSubscriber();

        failureBus.register(subscriberOne);
        failureBus.register(subscriberTwo);

        final FailureClass failureClass = FailureClass.of(InvalidProjectName.class);
        assertTrue(failureBus.hasDispatchers(failureClass));

        final Collection<FailureDispatcher<?>> dispatchers = failureBus.getDispatchers(failureClass);
        assertTrue(dispatchers.contains(subscriberOne));
        assertTrue(dispatchers.contains(subscriberTwo));
    }

    @Test
    public void unregister_subscribers() {
        final FailureSubscriber subscriberOne = new InvalidProjectNameSubscriber();
        final FailureSubscriber subscriberTwo = new InvalidProjectNameSubscriber();
        failureBus.register(subscriberOne);
        failureBus.register(subscriberTwo);
        final FailureClass failureClass = FailureClass.of(
                InvalidProjectName.class);

        failureBus.unregister(subscriberOne);

        // Check that the 2nd subscriber with the same failure subscriber method remains
        // after the 1st subscriber unregisters.
        final Collection<FailureDispatcher<?>> subscribers = failureBus.getDispatchers(failureClass);
        assertFalse(subscribers.contains(subscriberOne));
        assertTrue(subscribers.contains(subscriberTwo));

        // Check that after 2nd subscriber us unregisters he's no longer in
        failureBus.unregister(subscriberTwo);

        assertFalse(failureBus.getDispatchers(failureClass)
                              .contains(subscriberTwo));
    }

    @Test
    public void call_subscriber_when_failure_posted() {
        final InvalidProjectNameSubscriber subscriber = new InvalidProjectNameSubscriber();
        final Rejection rejection = invalidProjectNameFailure();
        failureBus.register(subscriber);

        failureBus.post(rejection);

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
        final FailureDispatcher<?> dispatcher = new BareDispatcher();

        failureBus.register(dispatcher);

        final FailureClass failureClass = FailureClass.of(InvalidProjectName.class);
        assertTrue(failureBus.getDispatchers(failureClass)
                             .contains(dispatcher));
    }

    @Test
    public void call_dispatchers() {
        final BareDispatcher dispatcher = new BareDispatcher();

        failureBus.register(dispatcher);

        failureBus.post(invalidProjectNameFailure());

        assertTrue(dispatcher.isDispatchCalled());
    }

    @Test
    public void not_call_dispatchers_if_dispatcher_failure_execution_postponed() {
        final BareDispatcher dispatcher = new BareDispatcher();

        failureBusWithPostponedExecution.register(dispatcher);

        final Rejection rejection = invalidProjectNameFailure();
        failureBusWithPostponedExecution.post(rejection);
        assertFalse(dispatcher.isDispatchCalled());

        final boolean failurePostponed = postponedDelivery.isPostponed(rejection, dispatcher);
        assertTrue(failurePostponed);
    }

    @Test
    public void deliver_postponed_failure_to_dispatcher_using_configured_executor() {
        final BareDispatcher dispatcher = new BareDispatcher();

        failureBusWithPostponedExecution.register(dispatcher);

        final Rejection rejection = invalidProjectNameFailure();
        failureBusWithPostponedExecution.post(rejection);
        final Set<FailureEnvelope> postponedFailures = postponedDelivery.getPostponedFailures();
        final FailureEnvelope postponedFailure = postponedFailures.iterator()
                                                                  .next();
        verify(delegateDispatcherExecutor, never()).execute(any(Runnable.class));
        postponedDelivery.deliverNow(postponedFailure, dispatcher.getClass());
        assertTrue(dispatcher.isDispatchCalled());
        verify(delegateDispatcherExecutor).execute(any(Runnable.class));
    }

    @Test
    public void unregister_dispatchers() {
        final FailureDispatcher<?> dispatcherOne = new BareDispatcher();
        final FailureDispatcher<?> dispatcherTwo = new BareDispatcher();
        final FailureClass failureClass = FailureClass.of(InvalidProjectName.class);
        failureBus.register(dispatcherOne);
        failureBus.register(dispatcherTwo);

        failureBus.unregister(dispatcherOne);
        final Set<FailureDispatcher<?>> dispatchers = failureBus.getDispatchers(failureClass);

        // Check we don't have 1st dispatcher, but have 2nd.
        assertFalse(dispatchers.contains(dispatcherOne));
        assertTrue(dispatchers.contains(dispatcherTwo));

        failureBus.unregister(dispatcherTwo);
        assertFalse(failureBus.getDispatchers(failureClass)
                              .contains(dispatcherTwo));
    }

    @Test
    public void catch_exceptions_caused_by_subscribers() {
        final VerifiableSubscriber faultySubscriber = new FaultySubscriber();

        failureBus.register(faultySubscriber);
        failureBus.post(invalidProjectNameFailure());

        assertTrue(faultySubscriber.isMethodCalled());
    }

    @Test
    public void unregister_registries_on_close() throws Exception {
        final FailureBus failureBus = FailureBus.newBuilder()
                                                .build();
        failureBus.register(new BareDispatcher());
        failureBus.register(new InvalidProjectNameSubscriber());
        final FailureClass failureClass = FailureClass.of(InvalidProjectName.class);

        failureBus.close();

        assertTrue(failureBus.getDispatchers(failureClass)
                             .isEmpty());
    }

    @Test
    public void support_short_form_subscriber_methods() {
        final FailureMessageSubscriber subscriber = new FailureMessageSubscriber();
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
        final FailureDispatcher<?> subscriber = new InvalidOrderSubscriber();

        failureBus.register(subscriber);
        failureBus.post(missingOwnerFailure());
    }

    @Test
    public void report_dead_messages() {
        final MemoizingObserver<Ack> observer = memoizingObserver();
        failureBus.post(missingOwnerFailure(), observer);
        assertTrue(observer.isCompleted());
        final Ack result = observer.firstResponse();
        assertNotNull(result);
        assertEquals(ERROR, result.getStatus().getStatusCase());
        final Error error = result.getStatus().getError();
        assertEquals(UnhandledFailureException.class.getCanonicalName(), error.getType());
    }

    @Test
    public void have_log() {
        assertNotNull(FailureBus.log());
    }

    private void checkFailure(VerifiableSubscriber subscriber) {
        final Rejection rejection = missingOwnerFailure();
        failureBus.register(subscriber);
        failureBus.post(rejection);

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
    private static class BareDispatcher implements FailureDispatcher<String> {

        private boolean dispatchCalled = false;

        @Override
        public Set<FailureClass> getMessageClasses() {
            return FailureClass.setOf(InvalidProjectName.class);
        }

        @Override
        public Set<String> dispatch(FailureEnvelope failure) {
            dispatchCalled = true;
            return Identity.of(this);
        }

        private boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }

    private static class InvalidProjectNameSubscriber extends FailureSubscriber {

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

    private static class PostponedDispatcherFailureDelivery extends DispatcherFailureDelivery {

        private final Map<FailureEnvelope,
                Class<? extends FailureDispatcher>> postponedExecutions = newHashMap();

        private PostponedDispatcherFailureDelivery(Executor delegate) {
            super(delegate);
        }

        @Override
        public boolean shouldPostponeDelivery(FailureEnvelope failure,
                                              FailureDispatcher<?> consumer) {
            postponedExecutions.put(failure, consumer.getClass());
            return true;
        }

        private boolean isPostponed(Rejection rejection, FailureDispatcher<?> dispatcher) {
            final FailureEnvelope envelope = FailureEnvelope.of(rejection);
            final Class<? extends FailureDispatcher> actualClass = postponedExecutions.get(
                    envelope);
            final boolean failurePostponed = actualClass != null;
            final boolean dispatcherMatches = failurePostponed && dispatcher.getClass()
                                                                            .equals(actualClass);
            return dispatcherMatches;
        }

        private Set<FailureEnvelope> getPostponedFailures() {
            final Set<FailureEnvelope> envelopes = postponedExecutions.keySet();
            return envelopes;
        }
    }

    private abstract static class VerifiableSubscriber extends FailureSubscriber {

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

    private static class FailureMessageSubscriber extends VerifiableSubscriber {

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
