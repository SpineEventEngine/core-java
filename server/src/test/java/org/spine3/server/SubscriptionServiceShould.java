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

package org.spine3.server;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.spine3.base.Queries;
import org.spine3.base.Response;
import org.spine3.client.Subscription;
import org.spine3.client.SubscriptionUpdate;
import org.spine3.client.Target;
import org.spine3.client.Topic;
import org.spine3.server.entity.Entity;
import org.spine3.server.stand.Stand;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;

import java.util.List;

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
import static org.spine3.test.Verify.assertInstanceOf;
import static org.spine3.test.Verify.assertSize;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

/**
 * @author Dmytro Dashenkov
 */
public class SubscriptionServiceShould {

    /*
     * Creation tests
     * --------------
     */

    @Test
    public void initialize_properly_with_one_bounded_context() {
        final BoundedContext singleBoundedContext = newBoundedContext("Single", newSimpleStand());

        final SubscriptionService.Builder builder = SubscriptionService.newBuilder()
                                                                       .add(singleBoundedContext);

        final SubscriptionService subscriptionService = builder.build();
        assertNotNull(subscriptionService);

        final List<BoundedContext> boundedContexs = builder.getBoundedContexts();
        assertSize(1, boundedContexs);
        assertTrue(boundedContexs.contains(singleBoundedContext));
    }

    @Test
    public void initialize_properly_with_several_bounded_contexts() {
        final BoundedContext firstBoundedContext = newBoundedContext("First", newSimpleStand());
        final BoundedContext secondBoundedContext = newBoundedContext("Second", newSimpleStand());
        final BoundedContext thirdBoundedContext = newBoundedContext("Third", newSimpleStand());

        final SubscriptionService.Builder builder = SubscriptionService.newBuilder()
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
        final BoundedContext firstBoundedContext = newBoundedContext("Removed", newSimpleStand());
        final BoundedContext secondBoundedContext = newBoundedContext("Also removed", newSimpleStand());
        final BoundedContext thirdBoundedContext = newBoundedContext("The one to stay", newSimpleStand());

        final SubscriptionService.Builder builder = SubscriptionService.newBuilder()
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

        final Topic topic = Topic.newBuilder()
                                 .setTarget(target)
                                 .build();

        final MemoizeStreamObserver<Subscription> observer = new MemoizeStreamObserver<>();

        subscriptionService.subscribe(topic, observer);

        assertNotNull(observer.streamFlowValue);
        assertTrue(observer.streamFlowValue.isInitialized());
        assertEquals(observer.streamFlowValue.getType(), type);

        assertNull(observer.throwable);
        assertTrue(observer.isCompleted);
    }

    @SuppressWarnings("ConstantConditions")     // as `null` is intentionally passed as a method param.
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

        final Topic topic = Topic.newBuilder()
                                 .setTarget(target)
                                 .build();
        // Subscribe on the topic
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

        final Entity entity = mockEntity(projectId, projectState, version);
        boundedContext.getStandFunnel()
                      .post(entity);

        // isCompleted set to false since we don't expect activationObserver::onCompleted to be called.
        activationObserver.verifyState(false);
    }

    private static Entity mockEntity(ProjectId projectId, Message projectState, int version) {
        final Entity entity = mock(Entity.class);
        when(entity.getState()).thenReturn(projectState);
        when(entity.getId()).thenReturn(projectId);
        when(entity.getVersion()).thenReturn(version);
        return entity;
    }

    @SuppressWarnings("ConstantConditions")     // as `null` is intentionally passed as a method param.
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

        final Topic topic = Topic.newBuilder()
                                 .setTarget(target)
                                 .build();

        // Subscribe
        final MemoizeStreamObserver<Subscription> subscribeObserver = new MemoizeStreamObserver<>();
        subscriptionService.subscribe(topic, subscribeObserver);

        // Activate subscription
        final MemoizeStreamObserver<SubscriptionUpdate> activateSubscription = spy(new MemoizeStreamObserver<SubscriptionUpdate>());
        subscriptionService.activate(subscribeObserver.streamFlowValue, activateSubscription);

        // Cancel subscription
        subscriptionService.cancel(subscribeObserver.streamFlowValue, new MemoizeStreamObserver<Response>());

        // Post update to Stand
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId("some-other-id")
                                             .build();
        final Message projectState = Project.newBuilder()
                                            .setId(projectId)
                                            .build();
        final int version = 1;
        final Entity entity = mockEntity(projectId, projectState, version);
        boundedContext.getStandFunnel()
                      .post(entity);

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
        final Topic topic = Topic.newBuilder()
                                 .setTarget(target)
                                 .build();
        final MemoizeStreamObserver<Subscription> subscriptionObserver = new MemoizeStreamObserver<>();
        subscriptionService.subscribe(topic, subscriptionObserver);

        final String failureMessage = "Execution breaking exception";
        final MemoizeStreamObserver<Response> observer = new MemoizeStreamObserver<Response>() {
            @Override
            public void onNext(Response value) {
                super.onNext(value);
                throw new RuntimeException(failureMessage);
            }
        };
        subscriptionService.cancel(subscriptionObserver.streamFlowValue, observer);
        assertNotNull(observer.streamFlowValue);
        assertFalse(observer.isCompleted);
        assertNotNull(observer.throwable);
        assertInstanceOf(RuntimeException.class, observer.throwable);
        assertEquals(observer.throwable.getMessage(), failureMessage);
    }

    private static BoundedContext setupBoundedContextWithProjectAggregateRepo() {
        final Stand stand = Stand.newBuilder()
                                 .build();
        final BoundedContext boundedContext = newBoundedContext(stand);
        stand.registerTypeSupplier(new Given.ProjectAggregatePartRepository(boundedContext));

        return boundedContext;
    }

    private static Target getProjectQueryTarget() {
        return Queries.Targets.allOf(Project.class);
    }

    private static Stand newSimpleStand() {
        return Stand.newBuilder()
                    .build();
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
