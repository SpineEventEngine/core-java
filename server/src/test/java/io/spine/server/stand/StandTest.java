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
package io.spine.server.stand;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.ActorRequestFactory;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityStateUpdate;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionValidationError;
import io.spine.client.Subscriptions;
import io.spine.client.Target;
import io.spine.client.Targets;
import io.spine.client.Topic;
import io.spine.core.Response;
import io.spine.core.Responses;
import io.spine.core.TenantId;
import io.spine.core.Version;
import io.spine.grpc.MemoizingObserver;
import io.spine.people.PersonName;
import io.spine.server.BoundedContext;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.entity.EntityStateEnvelope;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.stand.given.Given.StandTestProjectionRepository;
import io.spine.server.stand.given.StandTestEnv.MemoizeEntityUpdateCallback;
import io.spine.server.stand.given.StandTestEnv.MemoizeQueryResponseObserver;
import io.spine.system.server.MemoizingGateway;
import io.spine.system.server.NoOpSystemGateway;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.testing.core.given.GivenVersion;
import io.spine.testing.server.tenant.TenantAwareTest;
import io.spine.type.TypeUrl;
import io.spine.validate.Validate;
import io.spine.validate.ValidationError;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.client.QueryValidationError.INVALID_QUERY;
import static io.spine.client.QueryValidationError.UNSUPPORTED_QUERY_TARGET;
import static io.spine.client.TopicValidationError.INVALID_TOPIC;
import static io.spine.client.TopicValidationError.UNSUPPORTED_TOPIC_TARGET;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.stand.given.Given.StandTestProjection;
import static io.spine.server.stand.given.StandTestEnv.newStand;
import static io.spine.test.projection.Project.Status.CANCELLED;
import static io.spine.test.projection.Project.Status.STARTED;
import static io.spine.test.projection.Project.Status.UNDEFINED;
import static io.spine.testing.Tests.assertMatchesMask;
import static io.spine.testing.core.given.GivenUserId.of;
import static io.spine.testing.server.entity.given.Given.projectionOfClass;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//It's OK for this test.
@SuppressWarnings({
        "OverlyCoupledClass",
        "ClassWithTooManyMethods",
        "UnsecureRandomNumberGeneration"
})
@DisplayName("Stand should")
class StandTest extends TenantAwareTest {

    private static final int TOTAL_PROJECTS_FOR_BATCH_READING = 10;

    private boolean multitenant = false;

    private ActorRequestFactory requestFactory;

    protected void setMultitenant(boolean multitenant) {
        this.multitenant = multitenant;
    }

    public ActorRequestFactory getRequestFactory() {
        return requestFactory;
    }

    public void setRequestFactory(ActorRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    protected boolean isMultitenant() {
        return multitenant;
    }

    @BeforeEach
    protected void setUp() {
        setMultitenant(false);
        requestFactory = createRequestFactory(null);
    }

    protected static ActorRequestFactory createRequestFactory(@Nullable TenantId tenant) {
        ActorRequestFactory.Builder builder = ActorRequestFactory
                .newBuilder()
                .setActor(of(newUuid()));
        if (tenant != null) {
            builder.setTenantId(tenant);
        }
        return builder.build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("projection repositories")
        void projectionRepositories() {
            boolean multitenant = isMultitenant();
            BoundedContext boundedContext = BoundedContext.newBuilder()
                                                          .setMultitenant(multitenant)
                                                          .build();
            Stand stand = boundedContext.getStand();

            checkTypesEmpty(stand);

            StandTestProjectionRepository standTestProjectionRepo =
                    new StandTestProjectionRepository();
            stand.registerTypeSupplier(standTestProjectionRepo);
            checkHasExactlyOne(stand.getExposedTypes(), Project.getDescriptor());

            ImmutableSet<TypeUrl> knownAggregateTypes = stand.getExposedAggregateTypes();
            // As we registered a projection repo, known aggregate types should be still empty.
            assertTrue(knownAggregateTypes.isEmpty(),
                       "For some reason an aggregate type was registered");

            StandTestProjectionRepository anotherTestProjectionRepo =
                    new StandTestProjectionRepository();
            stand.registerTypeSupplier(anotherTestProjectionRepo);
            checkHasExactlyOne(stand.getExposedTypes(), Project.getDescriptor());
        }

        @Test
        @DisplayName("aggregate repositories")
        void aggregateRepositories() {
            BoundedContext boundedContext = BoundedContext.newBuilder()
                                                          .build();
            Stand stand = boundedContext.getStand();

            checkTypesEmpty(stand);

            CustomerAggregateRepository customerAggregateRepo = new CustomerAggregateRepository();
            stand.registerTypeSupplier(customerAggregateRepo);

            Descriptors.Descriptor customerEntityDescriptor = Customer.getDescriptor();
            checkHasExactlyOne(stand.getExposedTypes(), customerEntityDescriptor);
            checkHasExactlyOne(stand.getExposedAggregateTypes(), customerEntityDescriptor);

            @SuppressWarnings("LocalVariableNamingConvention")
            CustomerAggregateRepository anotherCustomerAggregateRepo =
                    new CustomerAggregateRepository();
            stand.registerTypeSupplier(anotherCustomerAggregateRepo);
            checkHasExactlyOne(stand.getExposedTypes(), customerEntityDescriptor);
            checkHasExactlyOne(stand.getExposedAggregateTypes(), customerEntityDescriptor);
        }
    }

    @Test
    @DisplayName("use provided executor upon update of watched type")
    void useProvidedExecutor() {
        Executor executor = mock(Executor.class);
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .setStand(Stand.newBuilder()
                                                                     .setCallbackExecutor(executor))
                                                      .build();
        Stand stand = boundedContext.getStand();

        StandTestProjectionRepository standTestProjectionRepo = new StandTestProjectionRepository();
        stand.registerTypeSupplier(standTestProjectionRepo);

        Topic projectProjections = requestFactory.topic()
                                                 .allOf(Project.class);

        MemoizingObserver<Subscription> observer = memoizingObserver();
        stand.subscribe(projectProjections, observer);
        Subscription subscription = observer.firstResponse();

        StreamObserver<Response> noopObserver = noOpObserver();
        stand.activate(subscription, emptyUpdateCallback(), noopObserver);
        assertNotNull(subscription);

        verify(executor, never()).execute(any(Runnable.class));

        ProjectId someId = ProjectId.getDefaultInstance();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(someId, Project.getDefaultInstance(), stateVersion));

        verify(executor, times(1)).execute(any(Runnable.class));
    }

    @Nested
    @DisplayName("return empty list")
    class ReturnEmptyList {

        @Test
        @DisplayName("on read all when empty")
        void onReadAllWhenEmpty() {
            Query readAllCustomers = requestFactory.query()
                                                   .all(Customer.class);
            checkEmptyResultForTargetOnEmptyStorage(readAllCustomers);
        }

        @Test
        @DisplayName("on read by IDs when empty")
        void onReadByIdWhenEmpty() {

            Query readCustomersById =
                    requestFactory.query()
                                  .byIds(Customer.class,
                                         newHashSet(customerIdFor(1), customerIdFor(2)));

            checkEmptyResultForTargetOnEmptyStorage(readCustomersById);
        }

        private void checkEmptyResultForTargetOnEmptyStorage(Query readCustomersQuery) {
            Stand stand = createStand();

            MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
            stand.execute(readCustomersQuery, responseObserver);

            List<Any> messageList = checkAndGetMessageList(responseObserver);
            assertTrue(
                messageList.isEmpty(),
                "Query returned a non-empty response message list though the target had been empty"
            );
        }
    }

    @Nested
    @DisplayName("return single result")
    class ReturnSingleResult {

        @Test
        @DisplayName("for projection read by ID")
        void forProjectionReadById() {
            doCheckReadingProjectsById(1);
        }
    }

    @Nested
    @DisplayName("return multiple results")
    class ReturnMultipleResults {

        @Test
        @DisplayName("for projection batch read by IDs")
        void forProjectionBatchRead() {
            doCheckReadingProjectsById(TOTAL_PROJECTS_FOR_BATCH_READING);
        }

        @Test
        @DisplayName("for projection batch read by IDs with field mask")
        void forProjectionReadWithMask() {
            List<FieldDescriptor> projectFields = Project.getDescriptor()
                                                         .getFields();
            doCheckReadingProjectByIdAndFieldMask(
                    projectFields.get(0)
                                 .getName(), // ID
                    projectFields.get(1)
                                 .getName()  // Name
            );
        }

        private void doCheckReadingProjectByIdAndFieldMask(String... paths) {
            StandTestProjectionRepository repository = new StandTestProjectionRepository();
            Stand stand = newStand(isMultitenant(), repository);

            int querySize = 2;

            Set<ProjectId> ids = new HashSet<>();
            for (int i = 0; i < querySize; i++) {
                Project project = Project
                        .newBuilder()
                        .setId(projectIdFor(i))
                        .setName(valueOf(i))
                        .setStatus(STARTED)
                        .build();
                repository.store(projectionOfClass(StandTestProjection.class)
                                         .withId(project.getId())
                                         .withState(project)
                                         .withVersion(1)
                                         .build());
                ids.add(project.getId());
            }

            Query query = requestFactory.query().byIdsWithMask(Project.class, ids, paths);

            FieldMask fieldMask = FieldMask.newBuilder()
                                           .addAllPaths(asList(paths))
                                           .build();
            MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
                @Override
                public void onNext(QueryResponse value) {
                    super.onNext(value);
                    List<Any> messages = value.getMessagesList();
                    assertThat(messages).hasSize(ids.size());
                    for (Any message : messages) {
                        Project project = (Project) unpack(message);
                        assertNotEquals(project, null);
                        assertMatchesMask(project, fieldMask);
                    }
                }
            };

            stand.execute(query, observer);

            verifyObserver(observer);
        }
    }

    @Nested
    @DisplayName("trigger subscription callback")
    class TriggerSubscriptionCallback {

        @Test
        @DisplayName("upon update of aggregate")
        void uponUpdateOfAggregate() {
            Stand stand = createStand();
            Topic allCustomers = requestFactory.topic()
                                               .allOf(Customer.class);

            MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();

            subscribeAndActivate(stand, allCustomers, memoizeCallback);
            assertNull(memoizeCallback.newEntityState());

            Customer customer = fillSampleCustomers(1)
                    .iterator()
                    .next();
            CustomerId customerId = customer.getId();
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customerId, customer, stateVersion));

            Any packedState = pack(customer);
            assertEquals(packedState, memoizeCallback.newEntityState());
        }

        @Test
        @DisplayName("upon update of projection")
        void uponUpdateOfProjection() {
            Stand stand = createStand();
            Topic allProjects = requestFactory.topic()
                                              .allOf(Project.class);

            MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
            subscribeAndActivate(stand, allProjects, memoizeCallback);
            assertNull(memoizeCallback.newEntityState());

            Project project = fillSampleProjects(1)
                    .iterator()
                    .next();
            ProjectId projectId = project.getId();
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(projectId, project, stateVersion));

            Any packedState = pack(project);
            assertEquals(packedState, memoizeCallback.newEntityState());
        }
    }

    @Test
    @DisplayName("trigger subscription callbacks matching by ID")
    void triggerSubscriptionsMatchingById() {
        Stand stand = createStand();

        Collection<Customer> sampleCustomers = fillSampleCustomers(10);

        Topic someCustomers = requestFactory.topic()
                                            .select(Customer.class)
                                            .byId(ids(sampleCustomers))
                                            .build();
        Collection<Customer> callbackStates = newHashSet();
        MemoizeEntityUpdateCallback callback = new MemoizeEntityUpdateCallback() {
            @Override
            public void onStateChanged(EntityStateUpdate update) {
                super.onStateChanged(update);
                Customer customerInCallback = (Customer) unpack(update.getState());
                callbackStates.add(customerInCallback);
            }
        };
        subscribeAndActivate(stand, someCustomers, callback);

        for (Customer customer : sampleCustomers) {
            CustomerId customerId = customer.getId();
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customerId, customer, stateVersion));
        }

        assertEquals(newHashSet(sampleCustomers), callbackStates);
    }

    @Test
    @DisplayName("allow cancelling subscriptions")
    void cancelSubscriptions() {
        Stand stand = createStand();
        Topic allCustomers = requestFactory.topic()
                                           .allOf(Customer.class);

        MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
        Subscription subscription =
                subscribeAndActivate(stand, allCustomers, memoizeCallback);

        stand.cancel(subscription, noOpObserver());

        Customer customer = fillSampleCustomers(1)
                .iterator()
                .next();
        CustomerId customerId = customer.getId();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        assertNull(memoizeCallback.newEntityState());
    }

    @Test
    @DisplayName("fail if cancelling non-existent subscription")
    void notCancelNonExistent() {
        Stand stand = createStand();
        Subscription nonExistingSubscription = Subscription.newBuilder()
                                                           .setId(Subscriptions.generateId())
                                                           .build();
        assertThrows(IllegalArgumentException.class,
                     () -> stand.cancel(nonExistingSubscription, noOpObserver()));
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    @DisplayName("trigger each subscription callback once for multiple subscriptions")
    void triggerSubscriptionCallbackOnce() {
        Stand stand = createStand();
        Target allCustomers = Targets.allOf(Customer.class);

        Set<MemoizeEntityUpdateCallback> callbacks = newHashSet();
        int totalCallbacks = 100;

        for (int callbackIndex = 0; callbackIndex < totalCallbacks; callbackIndex++) {
            MemoizeEntityUpdateCallback callback = subscribeWithCallback(stand, allCustomers);
            callbacks.add(callback);
        }

        Customer customer = fillSampleCustomers(1)
                .iterator()
                .next();
        CustomerId customerId = customer.getId();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        Any packedState = pack(customer);
        for (MemoizeEntityUpdateCallback callback : callbacks) {
            assertEquals(packedState, callback.newEntityState());
            verify(callback, times(1)).onStateChanged(any(EntityStateUpdate.class));
        }
    }

    @Test
    @DisplayName("not trigger subscription callbacks in case of another type criterion mismatch")
    void notTriggerOnTypeMismatch() {
        Stand stand = createStand();
        Target allProjects = Targets.allOf(Project.class);
        MemoizeEntityUpdateCallback callback = subscribeWithCallback(stand, allProjects);
        Customer customer = fillSampleCustomers(1)
                .iterator()
                .next();
        CustomerId customerId = customer.getId();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        verify(callback, never()).onStateChanged(any(EntityStateUpdate.class));
    }

    private MemoizeEntityUpdateCallback
    subscribeWithCallback(Stand stand, Target subscriptionTarget) {
        MemoizeEntityUpdateCallback callback = spy(new MemoizeEntityUpdateCallback());
        Topic topic = requestFactory.topic()
                                    .forTarget(subscriptionTarget);
        subscribeAndActivate(stand, topic, callback);

        assertNull(callback.newEntityState());
        return callback;
    }

    private static CustomerId customerIdFor(int numericId) {
        return CustomerId.newBuilder()
                         .setNumber(numericId)
                         .build();
    }

    private static ProjectId projectIdFor(int numericId) {
        return ProjectId.newBuilder()
                        .setId(valueOf(numericId))
                        .build();
    }

    private static final ImmutableList<String> FIRST_NAMES = ImmutableList.of(
            "Emma", "Liam", "Mary", "John"
    );

    private static final ImmutableList<String> LAST_NAMES = ImmutableList.of(
            "Smith", "Doe", "Steward", "Lee"
    );

    private static PersonName personName() {
        String givenName = selectOne(FIRST_NAMES);
        String familyName = selectOne(LAST_NAMES);
        return PersonName
                .newBuilder()
                .setGivenName(givenName)
                .setFamilyName(familyName)
                .build();
    }

    private static <T> T selectOne(List<T> choices) {
        checkArgument(!choices.isEmpty());
        Random random = new Random();
        int index = random.nextInt(choices.size());
        return choices.get(index);
    }

    @Test
    @DisplayName("query system BC for aggregate states")
    void readAggregates() {
        boolean multitenant = isMultitenant();
        MemoizingGateway gateway = multitenant
                                   ? MemoizingGateway.multitenant()
                                   : MemoizingGateway.singleTenant();
        Stand stand = Stand
                .newBuilder()
                .setMultitenant(multitenant)
                .setSystemGateway(gateway)
                .build();
        stand.registerTypeSupplier(new CustomerAggregateRepository());
        Query query = getRequestFactory().query()
                                         .all(Customer.class);
        stand.execute(query, noOpObserver());

        Message actualQuery = gateway.lastSeenQuery()
                                     .message();
        assertNotNull(actualQuery);
        assertEquals(query, actualQuery);
    }

    @Test
    @DisplayName("handle mistakes in query silently")
    void handleMistakesInQuery() {
        StandTestProjectionRepository repository = new StandTestProjectionRepository();
        Stand stand = newStand(isMultitenant(), repository);
        Project sampleProject = Project
                .newBuilder()
                .setId(projectIdFor(42))
                .setName("Test Project")
                .setStatus(CANCELLED)
                .build();
        repository.store(projectionOfClass(StandTestProjection.class)
                                 .withId(sampleProject.getId())
                                 .withState(sampleProject)
                                 .withVersion(42)
                                 .build());
        // FieldMask with invalid field paths.
        String[] paths = {"invalid_field_path_example", Project.getDescriptor()
                                                               .getFields()
                                                               .get(2).getFullName()};
        Query query = requestFactory.query().allWithMask(Project.class, paths);
        MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                Project project = (Project) unpack(messages.get(0));

                assertNotNull(project);

                assertFalse(project.hasId());
                assertTrue(project.getName().isEmpty());
                assertEquals(UNDEFINED, project.getStatus());
                assertTrue(project.getTaskList().isEmpty());
            }
        };
        stand.execute(query, observer);
        verifyObserver(observer);
    }

    @Nested
    @DisplayName("throw invalid query exception packed as IAE")
    class ThrowInvalidQueryEx {

        @Test
        @DisplayName("if querying unknown type")
        void ifQueryingUnknownType() {
            Stand stand = Stand.newBuilder()
                               .setSystemGateway(NoOpSystemGateway.INSTANCE)
                               .setMultitenant(isMultitenant())
                               .build();
            checkTypesEmpty(stand);

            // Customer type was NOT registered.
            // So create a query for an unknown type.
            Query readAllCustomers = requestFactory.query()
                                                   .all(Customer.class);

            try {
                stand.execute(readAllCustomers, noOpObserver());
                fail("Expected IllegalArgumentException upon executing query with unknown target," +
                             " but got nothing");
            } catch (IllegalArgumentException e) {
                verifyUnsupportedQueryTargetException(readAllCustomers, e);
            }
        }

        @Test
        @DisplayName("if invalid query message is passed")
        void ifInvalidQueryMessagePassed() {
            Stand stand = createStand();
            Query invalidQuery = Query.getDefaultInstance();

            try {
                stand.execute(invalidQuery, noOpObserver());
                fail("Expected IllegalArgumentException due to invalid query message passed," +
                             " but got nothing");
            } catch (IllegalArgumentException e) {
                verifyInvalidQueryException(invalidQuery, e);
            }
        }

        private void verifyUnsupportedQueryTargetException(Query query,
                                                           IllegalArgumentException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidQueryException);

            InvalidQueryException queryException = (InvalidQueryException) cause;
            assertEquals(query, queryException.getRequest());

            assertEquals(UNSUPPORTED_QUERY_TARGET.getNumber(),
                         queryException.asError()
                                       .getCode());
        }

        private void verifyInvalidQueryException(Query invalidQuery,
                                                 IllegalArgumentException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidQueryException);

            InvalidQueryException queryException = (InvalidQueryException) cause;
            assertEquals(invalidQuery, queryException.getRequest());

            assertEquals(INVALID_QUERY.getNumber(),
                         queryException.asError()
                                       .getCode());

            ValidationError validationError = queryException.asError()
                                                            .getValidationError();
            assertTrue(Validate.isNotDefault(validationError));
        }
    }

    @Nested
    @DisplayName("throw invalid topic exception packed as IAE")
    class ThrowInvalidTopicEx {

        @Test
        @DisplayName("if subscribing to unknown type changes")
        void ifSubscribingToUnknownType() {
            Stand stand = Stand.newBuilder()
                               .setMultitenant(isMultitenant())
                               .setSystemGateway(NoOpSystemGateway.INSTANCE)
                               .build();
            checkTypesEmpty(stand);

            // Project type was NOT registered.
            // So create a topic for an unknown type.
            Topic allProjectsTopic = requestFactory.topic()
                                                    .allOf(Project.class);

            try {
                stand.subscribe(allProjectsTopic, noOpObserver());
                fail("Expected IllegalArgumentException upon subscribing to a topic " +
                             "with unknown target, but got nothing");
            } catch (IllegalArgumentException e) {
                verifyUnsupportedTopicException(allProjectsTopic, e);
            }
        }

        @Test
        @DisplayName("if invalid topic message is passed")
        void ifInvalidTopicMessagePassed() {
            Stand stand = createStand();
            Topic invalidTopic = Topic.getDefaultInstance();
            try {
                stand.subscribe(invalidTopic, noOpObserver());
                fail("Expected IllegalArgumentException due to an invalid topic message, " +
                             "but got nothing");
            } catch (IllegalArgumentException e) {
                verifyInvalidTopicException(invalidTopic, e);
            }
        }

        private void verifyUnsupportedTopicException(Topic topic, IllegalArgumentException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidTopicException);

            InvalidTopicException topicException = (InvalidTopicException) cause;
            assertEquals(topic, topicException.getRequest());

            assertEquals(UNSUPPORTED_TOPIC_TARGET.getNumber(),
                         topicException.asError()
                                       .getCode());
        }

        private void verifyInvalidTopicException(Topic invalidTopic,
                                                 IllegalArgumentException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidTopicException);

            InvalidTopicException topicException = (InvalidTopicException) cause;
            assertEquals(invalidTopic, topicException.getRequest());

            assertEquals(INVALID_TOPIC.getNumber(),
                         topicException.asError()
                                       .getCode());

            ValidationError validationError = topicException.asError()
                                                            .getValidationError();
            assertTrue(Validate.isNotDefault(validationError));
        }
    }

    @Nested
    @DisplayName("throw invalid subscription exception packed as IAE")
    class ThrowInvalidSubscriptionEx {

        @Test
        @DisplayName("if activating subscription with unsupported target")
        void ifActivateWithUnsupportedTarget() {
            Stand stand = createStand();
            Subscription subscription = subscriptionWithUnknownTopic();

            try {
                stand.activate(subscription, emptyUpdateCallback(), noOpObserver());
                fail("Expected IllegalArgumentException upon activating an unknown subscription, " +
                             "but got nothing");
            } catch (IllegalArgumentException e) {
                verifyUnknownSubscriptionException(e, subscription);
            }
        }

        @Test
        @DisplayName("if cancelling subscription with unsupported target")
        void ifCancelWithUnsupportedTarget() {
            Stand stand = createStand();
            Subscription subscription = subscriptionWithUnknownTopic();

            try {
                stand.cancel(subscription, noOpObserver());
                fail("Expected IllegalArgumentException upon cancelling an unknown subscription, " +
                             "but got nothing");
            } catch (IllegalArgumentException e) {
                verifyUnknownSubscriptionException(e, subscription);
            }
        }

        @Test
        @DisplayName("if invalid subscription message is passed on activation")
        void ifActivateWithInvalidMessage() {
            Stand stand = createStand();
            Subscription invalidSubscription = Subscription.getDefaultInstance();

            try {
                stand.activate(invalidSubscription, emptyUpdateCallback(), noOpObserver());
                fail("Expected IllegalArgumentException due to an invalid subscription message " +
                             "passed to `activate`, but got nothing");
            } catch (IllegalArgumentException e) {
                verifyInvalidSubscriptionException(invalidSubscription, e);
            }
        }

        @Test
        @DisplayName("if invalid subscription message is passed on cancel")
        void ifCancelWithInvalidMessage() {
            Stand stand = createStand();
            Subscription invalidSubscription = Subscription.getDefaultInstance();

            try {
                stand.cancel(invalidSubscription, noOpObserver());
                fail("Expected IllegalArgumentException due to an invalid subscription message " +
                             "passed to `cancel`, but got nothing");
            } catch (IllegalArgumentException e) {
                verifyInvalidSubscriptionException(invalidSubscription, e);
            }
        }

        private void verifyInvalidSubscriptionException(Subscription invalidSubscription,
                                                        IllegalArgumentException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidSubscriptionException);

            InvalidSubscriptionException exception = (InvalidSubscriptionException) cause;
            assertEquals(invalidSubscription, exception.getRequest());

            assertEquals(SubscriptionValidationError.INVALID_SUBSCRIPTION.getNumber(),
                         exception.asError()
                                  .getCode());

            ValidationError validationError = exception.asError()
                                                       .getValidationError();
            assertTrue(Validate.isNotDefault(validationError));
        }

        private void verifyUnknownSubscriptionException(IllegalArgumentException e,
                                                        Subscription subscription) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidSubscriptionException);

            InvalidSubscriptionException exception = (InvalidSubscriptionException) cause;
            assertEquals(subscription, exception.getRequest());

            assertEquals(SubscriptionValidationError.UNKNOWN_SUBSCRIPTION.getNumber(),
                         exception.asError()
                                  .getCode());
        }

        private Subscription subscriptionWithUnknownTopic() {
            Topic allCustomersTopic = requestFactory.topic()
                                                    .allOf(Customer.class);
            return Subscription.newBuilder()
                               .setId(Subscriptions.generateId())
                               .setTopic(allCustomersTopic)
                               .build();
        }
    }

    private Stand createStand() {
        return newStand(isMultitenant());
    }

    @CanIgnoreReturnValue
    protected static Subscription subscribeAndActivate(Stand stand,
                                                       Topic topic,
                                                       Stand.EntityUpdateCallback callback) {
        MemoizingObserver<Subscription> observer = memoizingObserver();
        stand.subscribe(topic, observer);
        Subscription subscription = observer.firstResponse();
        stand.activate(subscription, callback, noOpObserver());

        assertNotNull(subscription);
        return subscription;
    }

    private static void verifyObserver(MemoizeQueryResponseObserver observer) {
        assertNotNull(observer.responseHandled());
        assertTrue(observer.isCompleted());
        assertNull(observer.throwable());
    }

    private void doCheckReadingProjectsById(int numberOfProjects) {
        // Define the types and values used as a test data.
        Map<ProjectId, Project> sampleProjects = newHashMap();
        TypeUrl projectType = TypeUrl.of(Project.class);
        fillSampleProjects(sampleProjects, numberOfProjects);

        StandTestProjectionRepository projectionRepository =
                mock(StandTestProjectionRepository.class);
        when(projectionRepository.getEntityStateType()).thenReturn(projectType);
        setupExpectedFindAllBehaviour(sampleProjects, projectionRepository);

        Stand stand = prepareStandWithProjectionRepo(projectionRepository);

        Query readMultipleProjects = requestFactory.query()
                                                   .byIds(Project.class, sampleProjects.keySet());

        MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readMultipleProjects, responseObserver);

        List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertEquals(sampleProjects.size(), messageList.size());
        Collection<Project> allCustomers = sampleProjects.values();
        for (Any singleRecord : messageList) {
            Project unpackedSingleResult = (Project) unpack(singleRecord);
            assertTrue(allCustomers.contains(unpackedSingleResult));
        }
    }

    private Stand
    prepareStandWithProjectionRepo(ProjectionRepository<?, ?, ?> projectionRepository) {
        Stand stand = createStand();
        assertNotNull(stand);
        stand.registerTypeSupplier(projectionRepository);
        return stand;
    }


    private static void setupExpectedFindAllBehaviour(
            Map<ProjectId, Project> sampleProjects,
            StandTestProjectionRepository projectionRepository) {

        Set<ProjectId> projectIds = sampleProjects.keySet();
        ImmutableCollection<StandTestProjection> allResults =
                toProjectionCollection(projectIds);

        for (ProjectId projectId : projectIds) {
            when(projectionRepository.find(eq(projectId)))
                    .thenReturn(Optional.of(new StandTestProjection(projectId)));
        }

        Iterable<ProjectId> matchingIds = argThat(projectionIdsIterableMatcher(projectIds));

        when(projectionRepository.loadAll(matchingIds, any(FieldMask.class)))
                .thenReturn(allResults.iterator());
        when(projectionRepository.loadAll())
                .thenReturn(allResults.iterator());

        EntityFilters matchingFilter = argThat(entityFilterMatcher(projectIds));
        when(projectionRepository.find(matchingFilter, any(FieldMask.class)))
                .thenReturn(allResults.iterator());
    }

    private static
    ArgumentMatcher<EntityFilters> entityFilterMatcher(Collection<ProjectId> projectIds) {
        // This argument matcher does NOT mimic the exact repository behavior.
        // Instead, it only matches the EntityFilters instance in case it has EntityIdFilter with
        // ALL the expected IDs.
        return argument -> {
            boolean everyElementPresent = true;
            for (EntityId entityId : argument.getIdFilter()
                                             .getIdsList()) {
                Any idAsAny = entityId.getId();
                Message rawId = unpack(idAsAny);
                if (rawId instanceof ProjectId) {
                    ProjectId convertedProjectId = (ProjectId) rawId;
                    everyElementPresent = everyElementPresent
                            && projectIds.contains(convertedProjectId);
                } else {
                    everyElementPresent = false;
                }
            }
            return everyElementPresent;
        };
    }

    private static
    ImmutableCollection<StandTestProjection> toProjectionCollection(Collection<ProjectId> values) {
        Collection<StandTestProjection> transformed = Collections2.transform(
                values,
                input -> {
                    checkNotNull(input);
                    return new StandTestProjection(input);
                });
        ImmutableList<StandTestProjection> result = ImmutableList.copyOf(transformed);
        return result;
    }

    private static ArgumentMatcher<Iterable<ProjectId>> projectionIdsIterableMatcher(
            Set<ProjectId> projectIds) {
        return argument -> {
            boolean everyElementPresent = true;
            for (ProjectId projectId : argument) {
                everyElementPresent = everyElementPresent && projectIds.contains(projectId);
            }
            return everyElementPresent;
        };
    }

    protected static Collection<Customer> fillSampleCustomers(int numberOfCustomers) {
        return generate(numberOfCustomers,
                        numericId -> Customer.newBuilder()
                                             .setId(customerIdFor(numericId))
                                             .setName(personName())
                                             .build());
    }

    private static Collection<Project> fillSampleProjects(int numberOfProjects) {
        return generate(numberOfProjects,
                        numericId -> Project.newBuilder()
                                            .setId(projectIdFor(numericId))
                                            .setName(valueOf(numericId))
                                            .build());
    }

    private static <T extends Message> Collection<T> generate(int count, IntFunction<T> idMapper) {
        Random random = new Random();
        List<T> result = IntStream.generate(random::nextInt)
                                  .limit(count)
                                  .map(Math::abs)
                                  .mapToObj(idMapper)
                                  .collect(toList());
        return result;
    }

    private static void fillSampleProjects(Map<ProjectId, Project> sampleProjects,
                                           int numberOfProjects) {
        for (int projectIndex = 0; projectIndex < numberOfProjects; projectIndex++) {
            Project project = Project.getDefaultInstance();
            ProjectId projectId = ProjectId.newBuilder()
                                           .setId(UUID.randomUUID()
                                                      .toString())
                                           .build();
            sampleProjects.put(projectId, project);
        }
    }

    private static List<Any> checkAndGetMessageList(MemoizeQueryResponseObserver responseObserver) {
        assertTrue(responseObserver.isCompleted(), "Query has not completed successfully");
        assertNull(responseObserver.throwable(), "Throwable has been caught upon query execution");

        QueryResponse response = responseObserver.responseHandled();
        assertEquals(Responses.ok(), response.getResponse(), "Query response is not OK");
        assertNotNull(response, "Query response must not be null");

        List<Any> messageList = response.getMessagesList();
        assertNotNull(messageList, "Query response has null message list");
        return messageList;
    }

    private static void checkTypesEmpty(Stand stand) {
        assertTrue(stand.getExposedTypes()
                        .isEmpty());
        assertTrue(stand.getExposedAggregateTypes()
                        .isEmpty());
    }

    private static void checkHasExactlyOne(Set<TypeUrl> availableTypes,
                                           Descriptors.Descriptor expectedType) {
        assertEquals(1, availableTypes.size());

        TypeUrl actualTypeUrl = availableTypes.iterator()
                                              .next();
        TypeUrl expectedTypeUrl = TypeUrl.from(expectedType);
        assertEquals(expectedTypeUrl, actualTypeUrl, "Type was registered incorrectly");
    }

    private static Stand.EntityUpdateCallback emptyUpdateCallback() {
        return newEntityState -> {
            //do nothing
        };
    }

    /**
     * Packs the parameters as {@code EntityStateEnvelope}
     * bounded by the current {@linkplain #tenantId() tenant ID}.
     */
    protected EntityStateEnvelope asEnvelope(Object entityId,
                                             Message entityState,
                                             Version entityVersion) {
        TenantId tenantId = isMultitenant() ? tenantId() : TenantId.getDefaultInstance();
        return EntityStateEnvelope.of(entityId, entityState, entityVersion, tenantId);
    }

    private static Set<CustomerId> ids(Collection<Customer> customers) {
        return customers.stream()
                        .map(Customer::getId)
                        .collect(toSet());
    }
}
