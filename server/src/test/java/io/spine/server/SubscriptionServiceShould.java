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

package io.spine.server;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Target;
import io.spine.client.Targets;
import io.spine.client.TestActorRequestFactory;
import io.spine.client.Topic;
import io.spine.core.Response;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.stand.Stand;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.time.Time;
import org.junit.Test;

import java.util.List;

import static io.spine.core.Versions.newVersion;
import static io.spine.test.Verify.assertInstanceOf;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
public class SubscriptionServiceShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(SubscriptionServiceShould.class);

    /*
     * Creation tests
     * --------------
     */

    /** Creates a new multi-tenant BoundedContext with the passed name. */
    private static BoundedContext ctx(String name) {
        return BoundedContext.newBuilder()
                             .setName(name)
                             .setMultitenant(true)
                             .build();
    }

    @Test
    public void initialize_properly_with_one_bounded_context() {
        final BoundedContext oneContext = ctx("One");

        final SubscriptionService.Builder builder = SubscriptionService.newBuilder()
                                                                       .add(oneContext);

        final SubscriptionService subscriptionService = builder.build();
        assertNotNull(subscriptionService);

        final List<BoundedContext> boundedContexts = builder.getBoundedContexts();
        assertSize(1, boundedContexts);
        assertTrue(boundedContexts.contains(oneContext));
    }

    @Test
    public void initialize_properly_with_several_bounded_contexts() {
        final BoundedContext firstBoundedContext = ctx("First");
        final BoundedContext secondBoundedContext = ctx("Second");
        final BoundedContext thirdBoundedContext = ctx("Third");

        final SubscriptionService.Builder builder =
                SubscriptionService.newBuilder()
                                   .add(firstBoundedContext)
                                   .add(secondBoundedContext)
                                   .add(thirdBoundedContext);
        final SubscriptionService service = builder.build();
        assertNotNull(service);

        final List<BoundedContext> boundedContexts = builder.getBoundedContexts();
        assertSize(3, boundedContexts);
        assertTrue(boundedContexts.contains(firstBoundedContext));
        assertTrue(boundedContexts.contains(secondBoundedContext));
        assertTrue(boundedContexts.contains(thirdBoundedContext));
    }

    @Test
    public void be_able_to_remove_bounded_context_from_builder() {
        final BoundedContext firstBoundedContext = ctx("Removed");
        final BoundedContext secondBoundedContext = ctx("Also removed");
        final BoundedContext thirdBoundedContext = ctx("The one to stay");

        final SubscriptionService.Builder builder =
                SubscriptionService.newBuilder()
                                   .add(firstBoundedContext)
                                   .add(secondBoundedContext)
                                   .add(thirdBoundedContext)
                                   .remove(secondBoundedContext)
                                   .remove(firstBoundedContext);
        final SubscriptionService subscriptionService = builder.build();
        assertNotNull(subscriptionService);

        final List<BoundedContext> boundedContexts = builder.getBoundedContexts();
        assertSize(1, boundedContexts);
        assertFalse(boundedContexts.contains(firstBoundedContext));
        assertFalse(boundedContexts.contains(secondBoundedContext));
        assertTrue(boundedContexts.contains(thirdBoundedContext));
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_initialize_from_empty_builder() {
        SubscriptionService.newBuilder()
                           .build();
    }

    /*
    * Subscription tests
    * ------------------
    */

    @Test
    public void subscribe_to_topic() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();
        final String type = boundedContext.getStand()
                                          .getExposedTypes()
                                          .iterator()
                                          .next()
                                          .getTypeName();
        final Target target = getProjectQueryTarget();

        assertEquals(type, target.getType());

        final Topic topic = requestFactory.topic().forTarget(target);

        final MemoizeStreamObserver<Subscription> observer = new MemoizeStreamObserver<>();

        subscriptionService.subscribe(topic, observer);

        assertNotNull(observer.streamFlowValue);
        assertTrue(observer.streamFlowValue.isInitialized());
        assertEquals(observer.streamFlowValue.getTopic()
                                             .getTarget()
                                             .getType(), type);

        assertNull(observer.throwable);
        assertTrue(observer.isCompleted);
    }

    @SuppressWarnings("ConstantConditions")
    // as `null` is intentionally passed as a method param.
    @Test
    public void handle_subscription_process_exceptions_and_call_observer_error_callback() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();
        final MemoizeStreamObserver<Subscription> observer = new MemoizeStreamObserver<>();
        // Causes NPE
        subscriptionService.subscribe(null, observer);
        assertNull(observer.streamFlowValue);
        assertFalse(observer.isCompleted);
        assertNotNull(observer.throwable);
        assertInstanceOf(NullPointerException.class, observer.throwable);
    }

    @Test
    public void activate_subscription() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();
        final Target target = getProjectQueryTarget();

        final Topic topic = requestFactory.topic().forTarget(target);

        // Subscribe to the topic
        final MemoizeStreamObserver<Subscription> subscriptionObserver = new MemoizeStreamObserver<>();
        subscriptionService.subscribe(topic, subscriptionObserver);
        subscriptionObserver.verifyState();

        // Activate subscription
        final MemoizeStreamObserver<SubscriptionUpdate> activationObserver = new MemoizeStreamObserver<>();
        subscriptionService.activate(subscriptionObserver.streamFlowValue, activationObserver);

        // Post update to Stand directly
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId("some-id")
                                             .build();
        final Message projectState = Project.newBuilder()
                                            .setId(projectId)
                                            .build();
        final int version = 1;

        final VersionableEntity entity = mockEntity(projectId, projectState, version);
        boundedContext.getStand()
                      .post(requestFactory.createCommandContext()
                                          .getActorContext()
                                          .getTenantId(), entity);

        // isCompleted set to false since we don't expect activationObserver::onCompleted to be called.
        activationObserver.verifyState(false);
    }

    private static VersionableEntity mockEntity(ProjectId projectId, Message projectState,
                                                int version) {
        final VersionableEntity entity = mock(AbstractVersionableEntity.class);
        when(entity.getState()).thenReturn(projectState);
        when(entity.getId()).thenReturn(projectId);
        when(entity.getVersion()).thenReturn(newVersion(version, Time.getCurrentTime()));
        return entity;
    }

    @SuppressWarnings("ConstantConditions")
    // as `null` is intentionally passed as a method param.
    @Test
    public void handle_activation_process_exceptions_and_call_observer_error_callback() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();
        final MemoizeStreamObserver<SubscriptionUpdate> observer = new MemoizeStreamObserver<>();
        // Causes NPE
        subscriptionService.activate(null, observer);
        assertNull(observer.streamFlowValue);
        assertFalse(observer.isCompleted);
        assertNotNull(observer.throwable);
        assertInstanceOf(NullPointerException.class, observer.throwable);
    }

    @Test
    public void cancel_subscription_on_topic() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();

        final Target target = getProjectQueryTarget();

        final Topic topic = requestFactory.topic().forTarget(target);

        // Subscribe
        final MemoizeStreamObserver<Subscription> subscribeObserver = new MemoizeStreamObserver<>();
        subscriptionService.subscribe(topic, subscribeObserver);

        // Activate subscription
        final MemoizeStreamObserver<SubscriptionUpdate> activateSubscription =
                spy(new MemoizeStreamObserver<SubscriptionUpdate>());
        subscriptionService.activate(subscribeObserver.streamFlowValue, activateSubscription);

        // Cancel subscription
        subscriptionService.cancel(subscribeObserver.streamFlowValue,
                                   new MemoizeStreamObserver<Response>());

        // Post update to Stand
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId("some-other-id")
                                             .build();
        final Message projectState = Project.newBuilder()
                                            .setId(projectId)
                                            .build();
        final int version = 1;
        final VersionableEntity entity = mockEntity(projectId, projectState, version);
        boundedContext.getStand()
                      .post(requestFactory.createCommandContext()
                                          .getActorContext()
                                          .getTenantId(), entity);

        // The update must not be handled by the observer
        verify(activateSubscription, never()).onNext(any(SubscriptionUpdate.class));
        verify(activateSubscription, never()).onCompleted();
    }

    @Test
    public void handle_cancellation_process_exceptions_and_call_observer_error_callback() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();
        final Target target = getProjectQueryTarget();

        final Topic topic = requestFactory.topic().forTarget(target);

        final MemoizeStreamObserver<Subscription> subscriptionObserver =
                new MemoizeStreamObserver<>();
        subscriptionService.subscribe(topic, subscriptionObserver);

        final String rejectionMessage = "Execution breaking exception";
        final MemoizeStreamObserver<Response> observer = new MemoizeStreamObserver<Response>() {
            @Override
            public void onNext(Response value) {
                super.onNext(value);
                throw new RuntimeException(rejectionMessage);
            }
        };
        subscriptionService.cancel(subscriptionObserver.streamFlowValue, observer);
        assertNotNull(observer.streamFlowValue);
        assertFalse(observer.isCompleted);
        assertNotNull(observer.throwable);
        assertInstanceOf(RuntimeException.class, observer.throwable);
        assertEquals(observer.throwable.getMessage(), rejectionMessage);
    }

    private static BoundedContext setupBoundedContextWithProjectAggregateRepo() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setStand(Stand.newBuilder())
                                                            .build();
        final Stand stand = boundedContext.getStand();

        stand.registerTypeSupplier(new Given.ProjectAggregateRepository());

        return boundedContext;
    }

    private static Target getProjectQueryTarget() {
        return Targets.allOf(Project.class);
    }

    private static class MemoizeStreamObserver<T> implements StreamObserver<T> {

        private T streamFlowValue;
        private Throwable throwable;
        private boolean isCompleted;

        @Override
        public void onNext(T value) {
            this.streamFlowValue = value;
        }

        @Override
        public void onError(Throwable t) {
            this.throwable = t;
        }

        @Override
        public void onCompleted() {
            this.isCompleted = true;
        }

        private void verifyState() {
            verifyState(true);
        }

        private void verifyState(boolean isCompleted) {
            assertNotNull(streamFlowValue);
            assertNull(throwable);
            assertEquals(this.isCompleted, isCompleted);
        }
    }
}
