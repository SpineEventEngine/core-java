/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Message;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Target;
import io.spine.client.Targets;
import io.spine.client.Topic;
import io.spine.core.Response;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.Given.ProjectAggregateRepository;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.entity.Entity;
import io.spine.server.stand.Stand;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.commandservice.customer.Customer;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.LogRecordSubject;
import io.spine.testing.logging.LoggerTest;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.server.entity.given.Given.aggregateOfClass;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings("deprecation")
// The deprecated `Stand.post()` method will become test-only in the future.
@DisplayName("SubscriptionService should")
class SubscriptionServiceTest {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(SubscriptionServiceTest.class);

    /** Creates a new multi-tenant BoundedContext with the passed name. */
    private static BoundedContext ctx(String name) {
        return BoundedContext.multitenant(name).build();
    }

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
    }

    @Nested
    @DisplayName("initialize properly with")
    class InitProperlyWith {

        @Test
        @DisplayName("one bounded context")
        void oneBc() {
            BoundedContext oneContext = ctx("One");

            SubscriptionService.Builder builder = SubscriptionService
                    .newBuilder()
                    .add(oneContext);

            SubscriptionService subscriptionService = builder.build();
            assertNotNull(subscriptionService);

            List<BoundedContext> boundedContexts = builder.contexts();
            assertThat(boundedContexts).hasSize(1);
            assertTrue(boundedContexts.contains(oneContext));
        }

        @Test
        @DisplayName("several bounded contexts")
        void severalBcs() {
            BoundedContext bc1 = ctx("First");
            BoundedContext bc2 = ctx("Second");
            BoundedContext bc3 = ctx("Third");

            SubscriptionService.Builder builder = SubscriptionService
                    .newBuilder()
                    .add(bc1)
                    .add(bc2)
                    .add(bc3);
            SubscriptionService service = builder.build();
            assertNotNull(service);

            List<BoundedContext> boundedContexts = builder.contexts();
            assertThat(boundedContexts).containsExactly(bc1, bc2, bc3);
        }
    }

    @Test
    @DisplayName("be able to remove bounded context from builder")
    void removeBcFromBuilder() {
        BoundedContext bc1 = ctx("Removed");
        BoundedContext bc2 = ctx("Also removed");
        BoundedContext bc3 = ctx("The one to stay");

        SubscriptionService.Builder builder = SubscriptionService
                .newBuilder()
                .add(bc1)
                .add(bc2)
                .add(bc3)
                .remove(bc2)
                .remove(bc1);
        SubscriptionService subscriptionService = builder.build();
        assertNotNull(subscriptionService);

        List<BoundedContext> boundedContexts = builder.contexts();
        assertThat(boundedContexts).containsExactly(bc3);
    }

    @Test
    @DisplayName("fail to initialize from empty builder")
    void notInitFromEmptyBuilder() {
        assertThrows(IllegalStateException.class,
                     () -> SubscriptionService.newBuilder()
                                              .build());
    }

    /*
     * Subscription tests
     * ------------------
     */

    @Nested
    @DisplayName("subscribe to")
    class SubscribeTo {

        @Test
        @DisplayName("entity updates")
        void entityTopic() {
            checkSubscribesTo(Project.class);
        }

        @Test
        @DisplayName("events")
        void eventTopic() {
            checkSubscribesTo(AggProjectCreated.class);
        }

        private void checkSubscribesTo(Class<? extends Message> aClass) {
            Target target = Targets.allOf(aClass);
            BoundedContext boundedContext = boundedContextWith(new ProjectAggregateRepository());

            SubscriptionService subscriptionService = SubscriptionService
                    .newBuilder()
                    .add(boundedContext)
                    .build();

            Topic topic = requestFactory.topic()
                                        .forTarget(target);

            MemoizingObserver<Subscription> observer = new MemoizingObserver<>();

            subscriptionService.subscribe(topic, observer);

            Subscription response = observer.firstResponse();
            ProtoSubject<?, Message> assertResponse = ProtoTruth.assertThat(response);
            assertResponse
                    .isNotNull();
            assertResponse
                    .hasAllRequiredFields();
            assertThat(response.getTopic()
                               .getTarget()
                               .getType())
                    .isEqualTo(target.getType());
            assertThat(observer.getError())
                    .isNull();
            assertThat(observer.isCompleted())
                    .isTrue();
        }
    }

    @Test
    @MuteLogging
    @DisplayName("receive IAE in observer error callback on subscribing to system event")
    void failOnSystemEventSubscribe() {
        BoundedContext boundedContext = boundedContextWith(new ProjectAggregateRepository());
        SubscriptionService subscriptionService = SubscriptionService
                .newBuilder()
                .add(boundedContext)
                .build();
        Topic topic = requestFactory.topic()
                                    .allOf(EntityStateChanged.class);
        MemoizingObserver<Subscription> observer = new MemoizingObserver<>();

        subscriptionService.subscribe(topic, observer);

        assertThat(observer.responses()).isEmpty();
        assertThat(observer.isCompleted()).isFalse();
        assertThat(observer.getError()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("activate subscription")
    void activateSubscription() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        BoundedContext boundedContext = boundedContextWith(repository);

        SubscriptionService subscriptionService = SubscriptionService
                .newBuilder()
                .add(boundedContext)
                .build();
        Target target = getProjectQueryTarget();

        Topic topic = requestFactory.topic()
                                    .forTarget(target);
        // Subscribe to the topic.
        MemoizingObserver<Subscription> subscriptionObserver = new MemoizingObserver<>();
        subscriptionService.subscribe(topic, subscriptionObserver);
        verifyState(subscriptionObserver, true);

        // Activate subscription.
        MemoizingObserver<SubscriptionUpdate> activationObserver = new MemoizingObserver<>();
        subscriptionService.activate(subscriptionObserver.firstResponse(), activationObserver);

        // Post update to Stand directly.
        ProjectId projectId = ProjectId
                .newBuilder()
                .setId("some-id")
                .build();
        Message projectState = Project
                .newBuilder()
                .setId(projectId)
                .build();
        int version = 1;

        Entity entity = newEntity(projectId, projectState, version);
        boundedContext.stand()
                      .post(entity, repository.lifecycleOf(projectId));

        // `isCompleted` set to false since we don't expect
        // activationObserver::onCompleted to be called.
        verifyState(activationObserver, false);
    }

    private static <T> void verifyState(MemoizingObserver<T> observer, boolean completed) {
        assertThat(observer.firstResponse()).isNotNull();
        assertThat(observer.getError()).isNull();
        assertThat(observer.isCompleted()).isEqualTo(completed);
    }

    @Test
    @MuteLogging
    @DisplayName("receive IAE in error callback when activating non-existent subscription")
    void failOnActivatingNonExistent() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        BoundedContext boundedContext = boundedContextWith(repository);

        SubscriptionService subscriptionService = SubscriptionService
                .newBuilder()
                .add(boundedContext)
                .build();

        // Try activating a subscription to `Customer` entity which is not present in BC.
        Topic invalidTopic = requestFactory.topic()
                                           .allOf(Customer.class);
        Subscription invalidSubscription = Subscription
                .newBuilder()
                .setTopic(invalidTopic)
                .build();

        MemoizingObserver<SubscriptionUpdate> activationObserver = new MemoizingObserver<>();
        subscriptionService.activate(invalidSubscription, activationObserver);

        // Check observer is not completed and contains an error.
        assertThat(activationObserver.responses()).isEmpty();
        assertThat(activationObserver.isCompleted()).isFalse();
        assertThat(activationObserver.getError()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("cancel subscription")
    void cancelSubscription() {
        ProjectAggregateRepository repository = new ProjectAggregateRepository();
        BoundedContext boundedContext = boundedContextWith(repository);

        SubscriptionService subscriptionService = SubscriptionService
                .newBuilder()
                .add(boundedContext)
                .build();

        Target target = getProjectQueryTarget();

        Topic topic = requestFactory.topic().forTarget(target);

        // Subscribe.
        MemoizingObserver<Subscription> subscribeObserver = new MemoizingObserver<>();
        subscriptionService.subscribe(topic, subscribeObserver);

        // Activate subscription.
        MemoizingObserver<SubscriptionUpdate> activateSubscription = spy(new MemoizingObserver<>());
        subscriptionService.activate(subscribeObserver.firstResponse(), activateSubscription);

        // Cancel subscription.
        subscriptionService.cancel(subscribeObserver.firstResponse(), new MemoizingObserver<>());

        // Post update to Stand.
        ProjectId projectId = ProjectId
                .newBuilder()
                .setId("some-other-id")
                .build();
        Message projectState = Project
                .newBuilder()
                .setId(projectId)
                .build();
        int version = 1;
        Entity entity = newEntity(projectId, projectState, version);
        boundedContext.stand()
                      .post(entity, repository.lifecycleOf(projectId));

        // The update must not be handled by the observer.
        verify(activateSubscription, never()).onNext(any(SubscriptionUpdate.class));
        verify(activateSubscription, never()).onCompleted();
    }

    @Nested
    @DisplayName("when cancelling non-existent subscription")
    class WarnOnCancelling extends LoggerTest {

        private BoundedContext context;
        private MemoizingObserver<Response> cancellationObserver;

        WarnOnCancelling() {
            super(SubscriptionService.class, Level.WARNING);
        }

        @BeforeEach
        void cancelSubscription() {
            SubscriptionService subscriptionService = createService();
            Subscription invalidSubscription = createSubscription();

            // Hook the log here to minimize the trapped output.
            addHandler();

            cancellationObserver = new MemoizingObserver<>();
            subscriptionService.cancel(invalidSubscription, cancellationObserver);
        }

        private SubscriptionService createService() {
            ProjectAggregateRepository repository = new ProjectAggregateRepository();
            context = boundedContextWith(repository);
            return SubscriptionService
                    .newBuilder()
                    .add(context)
                    .build();
        }

        /**
         * Creates a subscription with the entity state {@code Customer}, which is not present
         * in the context under the test.
         */
        private Subscription createSubscription() {
            Topic invalidTopic = requestFactory.topic()
                                               .allOf(Customer.class);
            return Subscription
                    .newBuilder()
                    .setTopic(invalidTopic)
                    .build();
        }

        @AfterEach
        void tearDown() throws Exception {
            removeHandler();
            context.close();
        }

        @Test
        @DisplayName("do not return the error to the observer")
        void observerIsEmpty() {
            assertThat(cancellationObserver.isCompleted())
                    .isTrue();
            assertThat(cancellationObserver.responses())
                    .isEmpty();
            assertThat(cancellationObserver.getError())
                    .isNull();
        }

        @Test
        @DisplayName("log warning")
        void nonExistingSubscription() {
            LogRecordSubject assertRecord = handler().assertRecord();
            assertRecord.hasLevelThat()
                        .isEqualTo(level());
        }
    }

    @Nested
    @DisplayName("handle exceptions and call observer error callback for")
    class HandleExceptionsOf {

        @Test
        @MuteLogging
        @DisplayName("subscription process")
        void subscription() {
            BoundedContext boundedContext = boundedContextWith(new ProjectAggregateRepository());

            SubscriptionService subscriptionService = SubscriptionService
                    .newBuilder()
                    .add(boundedContext)
                    .build();
            MemoizingObserver<Subscription> observer = new MemoizingObserver<>();
            // Causes NPE.
            subscriptionService.subscribe(null, observer);
            assertThat(observer.responses()).isEmpty();
            assertThat(observer.isCompleted()).isFalse();
            assertThat(observer.getError()).isInstanceOf(NullPointerException.class);
        }

        @Test
        @MuteLogging
        @DisplayName("activation process")
        void activation() {
            BoundedContext boundedContext = boundedContextWith(new ProjectAggregateRepository());

            SubscriptionService subscriptionService = SubscriptionService
                    .newBuilder()
                    .add(boundedContext)
                    .build();
            MemoizingObserver<SubscriptionUpdate> observer = new MemoizingObserver<>();
            // Causes NPE.
            subscriptionService.activate(null, observer);
            assertThat(observer.responses()).isEmpty();
            assertThat(observer.isCompleted()).isFalse();
            assertThat(observer.getError()).isInstanceOf(NullPointerException.class);
        }

        @Test
        @MuteLogging
        @DisplayName("cancellation process")
        void cancellation() {
            BoundedContext boundedContext = boundedContextWith(new ProjectAggregateRepository());

            SubscriptionService subscriptionService = SubscriptionService
                    .newBuilder()
                    .add(boundedContext)
                    .build();
            Target target = getProjectQueryTarget();

            Topic topic = requestFactory.topic()
                                        .forTarget(target);

            MemoizingObserver<Subscription> subscriptionObserver = new MemoizingObserver<>();
            subscriptionService.subscribe(topic, subscriptionObserver);

            String rejectionMessage = "Execution breaking exception";
            MemoizingObserver<Response> observer = new MemoizingObserver<Response>() {
                @Override
                public void onNext(Response value) {
                    super.onNext(value);
                    throw new RuntimeException(rejectionMessage);
                }
            };
            subscriptionService.cancel(subscriptionObserver.firstResponse(), observer);
            assertNotNull(observer.firstResponse());
            assertFalse(observer.isCompleted());
            assertThat(observer.getError())
                    .isInstanceOf(RuntimeException.class);
            assertThat(observer.getError().getMessage())
                    .isEqualTo(rejectionMessage);
        }
    }

    private static Entity newEntity(ProjectId projectId, Message projectState, int version) {
        Entity entity = aggregateOfClass(PAggregate.class)
                .withId(projectId)
                .withState((Project) projectState)
                .withVersion(version)
                .build();
        return entity;
    }

    private static BoundedContext boundedContextWith(ProjectAggregateRepository repository) {
        BoundedContext boundedContext = BoundedContextBuilder
                .assumingTests()
                .setStand(Stand.newBuilder())
                .build();
        boundedContext.register(repository);
        return boundedContext;
    }

    private static Target getProjectQueryTarget() {
        return Targets.allOf(Project.class);
    }

    private static class PAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        protected PAggregate(ProjectId id) {
            super(id);
        }
    }
}
