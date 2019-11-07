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
import io.spine.base.EntityState;
import io.spine.client.EntityStateUpdate;
import io.spine.client.EntityUpdates;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Target;
import io.spine.client.Targets;
import io.spine.client.Topic;
import io.spine.client.TopicFactory;
import io.spine.core.Command;
import io.spine.core.Response;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.server.Given.ProjectAggregateRepository;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.commandservice.customer.Customer;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.LoggingTest;
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
import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SubscriptionService should")
class SubscriptionServiceTest {

    private final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(SubscriptionServiceTest.class);

    private BoundedContext context;
    private SubscriptionService subscriptionService;
    /** The observer for creating a subscription. */
    private MemoizingObserver<Subscription> observer;
    /** The observer for activating a subscription. */
    private MemoizingObserver<SubscriptionUpdate> activationObserver;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        ServerEnvironment.instance()
                         .reset();
        context = BoundedContextBuilder
                .assumingTests()
                .add(new ProjectAggregateRepository())
                .build();
        subscriptionService = SubscriptionService
                .newBuilder()
                .add(context)
                .build();
        observer = new MemoizingObserver<>();
        activationObserver = new MemoizingObserver<>();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    /*
     * Tests of work with multiple Bounded Contexts
     * ---------------------------------------------
     */

    /** Creates a new multi-tenant BoundedContext with the passed name. */
    private static BoundedContext ctx(String name) {
        return BoundedContext.multitenant(name)
                             .build();
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

            List<BoundedContext> contexts = builder.contexts();
            assertThat(contexts).hasSize(1);
            assertTrue(contexts.contains(oneContext));
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

            List<BoundedContext> contexts = builder.contexts();
            assertThat(contexts).containsExactly(bc1, bc2, bc3);
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

        List<BoundedContext> contexts = builder.contexts();
        assertThat(contexts).containsExactly(bc3);
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

    private TopicFactory topic() {
        return requestFactory.topic();
    }

    private Topic newTopic() {
        return topic().forTarget(Targets.allOf(Project.class));
    }

    /**
     * Creates a topic on the type which is not available in the Bounded Context used
     * in these tests.
     */
    private Topic invalidTopic() {
        return topic().allOf(Customer.class);
    }

    /**
     * Creates a subscription with the entity state {@code Customer}, which is not present
     * in the context under the test.
     */
    private Subscription invalidSubscription() {
        Topic invalidTopic = invalidTopic();
        return Subscription
                .newBuilder()
                .setTopic(invalidTopic)
                .build();
    }

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
            Topic topic = topic().forTarget(target);
            subscriptionService.subscribe(topic, observer);

            Subscription response = observer.firstResponse();
            ProtoSubject assertResponse = ProtoTruth.assertThat(response);
            assertResponse.isNotNull();
            assertResponse.hasAllRequiredFields();
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
        Topic topic = topic().allOf(EntityStateChanged.class);
        subscriptionService.subscribe(topic, observer);

        assertThat(observer.responses()).isEmpty();
        assertThat(observer.isCompleted()).isFalse();
        assertThat(observer.getError()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("activate subscription")
    void activateSubscription() {
        Topic topic = newTopic();
        // Subscribe to the topic.
        subscriptionService.subscribe(topic, observer);
        verifyState(observer, true);

        // Activate subscription.
        subscriptionService.activate(observer.firstResponse(), activationObserver);

        // Post update to Stand directly.
        ProjectId projectId = ProjectId
                .newBuilder()
                .setId("some-id")
                .build();

        createProject(context, projectId);

        // `isCompleted` set to false since we don't expect
        // activationObserver::onCompleted to be called.
        verifyState(activationObserver, false);

        Project actual = memoizedEntity(activationObserver, Project.class);
        Project expected = Project.newBuilder()
                                  .setId(projectId)
                                  .build();
        ProtoTruth.assertThat(actual)
                  .comparingExpectedFieldsOnly()
                  .isEqualTo(expected);
    }

    private void createProject(BoundedContext context, ProjectId projectId) {
        AggCreateProject cmd = Given.CommandMessage.createProject(projectId);
        Command command = requestFactory.createCommand(cmd);
        context.commandBus()
               .post(command, StreamObservers.noOpObserver());
    }

    private static <T extends EntityState>
    T memoizedEntity(MemoizingObserver<SubscriptionUpdate> observer, Class<T> entityCls) {
        SubscriptionUpdate update = observer.firstResponse();
        EntityUpdates entityUpdates = update.getEntityUpdates();
        assertThat(entityUpdates.getUpdateCount()).isEqualTo(1);

        EntityStateUpdate projectUpdate = entityUpdates.getUpdate(0);
        return unpack(projectUpdate.getState(), entityCls);
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
        Subscription invalidSubscription = invalidSubscription();
        subscriptionService.activate(invalidSubscription, activationObserver);

        // Check observer is not completed and contains an error.
        assertThat(activationObserver.responses()).isEmpty();
        assertThat(activationObserver.isCompleted()).isFalse();
        assertThat(activationObserver.getError()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("cancel subscription")
    void cancelSubscription() {
        Topic topic = newTopic();

        // Subscribe.
        subscriptionService.subscribe(topic, observer);

        // Activate subscription.
        subscriptionService.activate(observer.firstResponse(), activationObserver);

        // Cancel subscription.
        subscriptionService.cancel(observer.firstResponse(), new MemoizingObserver<>());

        // Post update to Stand.
        ProjectId projectId = ProjectId
                .newBuilder()
                .setId("some-other-id")
                .build();
        createProject(context, projectId);

        // The update must not be handled by the observer.
        assertThat(activationObserver.responses()).isEmpty();
        assertThat(activationObserver.isCompleted()).isFalse();
    }

    @Nested
    @DisplayName("when cancelling non-existent subscription")
    class WarnOnCancelling extends LoggingTest {

        private MemoizingObserver<Response> cancellationObserver;

        WarnOnCancelling() {
            super(SubscriptionService.class, Level.WARNING);
        }

        @BeforeEach
        void cancelSubscription() {
            Subscription invalidSubscription = invalidSubscription();

            // Hook the log here to minimize the trapped output.
            interceptLogging();

            cancellationObserver = new MemoizingObserver<>();
            subscriptionService.cancel(invalidSubscription, cancellationObserver);
        }

        @AfterEach
        void tearDown() {
            restoreLogging();
        }

        @Test
        @DisplayName("do not return the error to the observer")
        void observerIsEmpty() {
            assertThat(cancellationObserver.isCompleted()).isTrue();
            assertThat(cancellationObserver.responses()).isEmpty();
            assertThat(cancellationObserver.getError()).isNull();
        }

        @Test
        @DisplayName("log warning")
        void nonExistingSubscription() {
            assertLog().record()
                       .hasLevelThat()
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
            // Causes NPE.
            subscriptionService.activate(null, activationObserver);
            assertThat(activationObserver.responses()).isEmpty();
            assertThat(activationObserver.isCompleted()).isFalse();
            assertThat(activationObserver.getError()).isInstanceOf(NullPointerException.class);
        }

        @Test
        @MuteLogging
        @DisplayName("cancellation process")
        void cancellation() {
            Topic topic = newTopic();
            subscriptionService.subscribe(topic, observer);

            String rejectionMessage = "Execution breaking exception";
            MemoizingObserver<Response> faultyObserver = new MemoizingObserver<Response>() {
                @Override
                public void onNext(Response value) {
                    super.onNext(value);
                    throw new RuntimeException(rejectionMessage);
                }
            };
            subscriptionService.cancel(observer.firstResponse(), faultyObserver);
            assertThat(faultyObserver.firstResponse()).isNotNull();
            assertThat(faultyObserver.isCompleted()).isFalse();
            assertThat(faultyObserver.getError()).isInstanceOf(RuntimeException.class);
            assertThat(faultyObserver.getError().getMessage()).isEqualTo(rejectionMessage);
        }
    }
}
