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

package io.spine.server;

import com.google.protobuf.Message;
import io.spine.base.Time;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Target;
import io.spine.client.Targets;
import io.spine.client.TestActorRequestFactory;
import io.spine.client.Topic;
import io.spine.core.Response;
import io.spine.server.Given.MemoizeStreamObserver;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.model.ModelTests;
import io.spine.server.stand.Stand;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.core.Versions.newVersion;
import static io.spine.test.Verify.assertInstanceOf;
import static io.spine.test.Verify.assertSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"})
// JUnit nested classes cannot be static.
@DisplayName("SubscriptionService should")
class SubscriptionServiceTest {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(SubscriptionServiceTest.class);

    /** Creates a new multi-tenant BoundedContext with the passed name. */
    private static BoundedContext ctx(String name) {
        return BoundedContext.newBuilder()
                             .setName(name)
                             .setMultitenant(true)
                             .build();
    }

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
    }

    @Nested
    @DisplayName("initialize properly with")
    class InitProperlyWith {

        @Test
        @DisplayName("one bounded context")
        void oneBc() {
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
        @DisplayName("several bounded contexts")
        void severalBcs() {
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
    }

    @Test
    @DisplayName("be able to remove bounded context from builder")
    void removeBcFromBuilder() {
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

    @Test
    @DisplayName("fail to initialize from empty builder")
    void notInitFromEmptyBuilder() {
        assertThrows(IllegalStateException.class, () -> SubscriptionService.newBuilder()
                                                                           .build());
    }

    /*
    * Subscription tests
    * ------------------
    */

    @Test
    @DisplayName("subscribe to topic")
    void subscribeToTopic() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();
        final String type = boundedContext.getStand()
                                          .getExposedTypes()
                                          .iterator()
                                          .next()
                                          .value();
        final Target target = getProjectQueryTarget();

        assertEquals(type, target.getType());

        final Topic topic = requestFactory.topic().forTarget(target);

        final MemoizeStreamObserver<Subscription> observer = new MemoizeStreamObserver<>();

        subscriptionService.subscribe(topic, observer);

        assertNotNull(observer.streamFlowValue());
        assertTrue(observer.streamFlowValue().isInitialized());
        assertEquals(observer.streamFlowValue().getTopic()
                                               .getTarget()
                                               .getType(), type);

        assertNull(observer.throwable());
        assertTrue(observer.isCompleted());
    }

    @Test
    @DisplayName("activate subscription")
    void activateSubscription() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();
        final Target target = getProjectQueryTarget();

        final Topic topic = requestFactory.topic().forTarget(target);

        // Subscribe to the topic.
        final MemoizeStreamObserver<Subscription> subscriptionObserver = new MemoizeStreamObserver<>();
        subscriptionService.subscribe(topic, subscriptionObserver);
        subscriptionObserver.verifyState();

        // Activate subscription.
        final MemoizeStreamObserver<SubscriptionUpdate> activationObserver = new MemoizeStreamObserver<>();
        subscriptionService.activate(subscriptionObserver.streamFlowValue(), activationObserver);

        // Post update to Stand directly.
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

        // `isCompleted` set to false since we don't expect activationObserver::onCompleted to be
        // called.
        activationObserver.verifyState(false);
    }

    @Test
    @DisplayName("cancel subscription")
    void cancelSubscription() {
        final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

        final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                           .add(boundedContext)
                                                                           .build();

        final Target target = getProjectQueryTarget();

        final Topic topic = requestFactory.topic().forTarget(target);

        // Subscribe.
        final MemoizeStreamObserver<Subscription> subscribeObserver = new MemoizeStreamObserver<>();
        subscriptionService.subscribe(topic, subscribeObserver);

        // Activate subscription.
        final MemoizeStreamObserver<SubscriptionUpdate> activateSubscription =
                spy(new MemoizeStreamObserver<SubscriptionUpdate>());
        subscriptionService.activate(subscribeObserver.streamFlowValue(), activateSubscription);

        // Cancel subscription.
        subscriptionService.cancel(subscribeObserver.streamFlowValue(),
                                   new MemoizeStreamObserver<>());

        // Post update to Stand.
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

        // The update must not be handled by the observer.
        verify(activateSubscription, never()).onNext(any(SubscriptionUpdate.class));
        verify(activateSubscription, never()).onCompleted();
    }

    @Nested
    @DisplayName("handle exceptions and call observer error callback for")
    class HandleExceptionsOf {

        @SuppressWarnings("ConstantConditions")
        // As `null` is intentionally passed as a method param.
        @Test
        @DisplayName("subscription process")
        void subscription() {
            final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

            final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                               .add(boundedContext)
                                                                               .build();
            final MemoizeStreamObserver<Subscription> observer = new MemoizeStreamObserver<>();
            // Causes NPE.
            subscriptionService.subscribe(null, observer);
            assertNull(observer.streamFlowValue());
            assertFalse(observer.isCompleted());
            assertNotNull(observer.throwable());
            assertInstanceOf(NullPointerException.class, observer.throwable());
        }

        @SuppressWarnings("ConstantConditions")

        // As `null` is intentionally passed as a method param.
        @Test
        @DisplayName("activation process")
        void activation() {
            final BoundedContext boundedContext = setupBoundedContextWithProjectAggregateRepo();

            final SubscriptionService subscriptionService = SubscriptionService.newBuilder()
                                                                               .add(boundedContext)
                                                                               .build();
            final MemoizeStreamObserver<SubscriptionUpdate> observer = new MemoizeStreamObserver<>();
            // Causes NPE.
            subscriptionService.activate(null, observer);
            assertNull(observer.streamFlowValue());
            assertFalse(observer.isCompleted());
            assertNotNull(observer.throwable());
            assertInstanceOf(NullPointerException.class, observer.throwable());
        }

        @Test
        @DisplayName("cancellation process")
        void cancellation() {
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
            subscriptionService.cancel(subscriptionObserver.streamFlowValue(), observer);
            assertNotNull(observer.streamFlowValue());
            assertFalse(observer.isCompleted());
            assertNotNull(observer.throwable());
            assertInstanceOf(RuntimeException.class, observer.throwable());
            assertEquals(observer.throwable().getMessage(), rejectionMessage);
        }
    }

    private static VersionableEntity mockEntity(ProjectId projectId, Message projectState,
                                                int version) {
        final VersionableEntity entity = mock(AbstractVersionableEntity.class);
        when(entity.getState()).thenReturn(projectState);
        when(entity.getId()).thenReturn(projectId);
        when(entity.getVersion()).thenReturn(newVersion(version, Time.getCurrentTime()));
        return entity;
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
}
