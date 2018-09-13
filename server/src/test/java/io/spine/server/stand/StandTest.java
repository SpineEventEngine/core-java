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
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.ActorRequestFactory;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityStateUpdate;
import io.spine.client.Order;
import io.spine.client.Pagination;
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
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.Given.CustomerAggregate;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityStateEnvelope;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.stand.given.Given;
import io.spine.server.stand.given.Given.StandTestProjectionRepository;
import io.spine.server.stand.given.StandTestEnv.MemoizeEntityUpdateCallback;
import io.spine.server.stand.given.StandTestEnv.MemoizeQueryResponseObserver;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.testing.Verify;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.base.Identifier.newUuid;
import static io.spine.client.QueryValidationError.INVALID_QUERY;
import static io.spine.client.QueryValidationError.UNSUPPORTED_QUERY_TARGET;
import static io.spine.client.TopicValidationError.INVALID_TOPIC;
import static io.spine.client.TopicValidationError.UNSUPPORTED_TOPIC_TARGET;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.stand.given.Given.StandTestProjection;
import static io.spine.testing.core.given.GivenUserId.of;
import static java.util.Collections.emptyIterator;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
//It's OK for a test.
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyMethods", "OverlyComplexClass",
        "DuplicateStringLiteralInspection" /* Common test display names */})
@DisplayName("Stand should")
class StandTest extends TenantAwareTest {
    private static final int TOTAL_CUSTOMERS_FOR_BATCH_READING = 10;
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

    @Test
    @DisplayName("initialize with empty builder")
    void initializeWithEmptyBuilder() {
        Stand.Builder builder = Stand.newBuilder()
                                     .setMultitenant(isMultitenant());
        Stand stand = builder.build();

        assertNotNull(stand);
        assertTrue(stand.getExposedTypes()
                        .isEmpty(),
                   "Exposed types must be empty after the initialization.");
        assertTrue(stand.getExposedAggregateTypes()
                        .isEmpty(),
                   "Exposed aggregate types must be empty after the initialization");
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

    @SuppressWarnings("OverlyCoupledMethod")
    @Test
    @DisplayName("operate with storage provided through builder")
    void operateWithStorage() {
        StandStorage standStorageMock = mock(StandStorage.class);
        BoundedContext boundedContext =
                BoundedContext.newBuilder()
                              .setStand(Stand.newBuilder()
                                             .setStorage(standStorageMock))
                              .build();
        Stand stand = boundedContext.getStand();

        assertNotNull(stand);

        CustomerAggregateRepository customerAggregateRepo = new CustomerAggregateRepository();
        boundedContext.register(customerAggregateRepo);

        int numericIdValue = 17;
        CustomerId customerId = customerIdFor(numericIdValue);
        CustomerAggregate customerAggregate = customerAggregateRepo.create(customerId);
        Customer customerState = customerAggregate.getState();
        TypeUrl customerType = TypeUrl.of(Customer.class);
        Version stateVersion = GivenVersion.withNumber(1);

        verify(standStorageMock, never()).write(any(AggregateStateId.class),
                                                any(EntityRecordWithColumns.class));

        stand.update(asEnvelope(customerId, customerState, stateVersion));

        AggregateStateId expectedAggregateStateId = AggregateStateId.of(customerId, customerType);
        Any packedState = AnyPacker.pack(customerState);
        EntityRecord expectedRecord = EntityRecord.newBuilder()
                                                  .setState(packedState)
                                                  .build();
        verify(standStorageMock, times(1))
                .write(eq(expectedAggregateStateId), recordStateMatcher(expectedRecord));
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

            Query readCustomersById = requestFactory.query()
                                                    .byIds(Customer.class,
                                                           newHashSet(
                                                                   customerIdFor(1),
                                                                   customerIdFor(2)
                                                           ));

            checkEmptyResultForTargetOnEmptyStorage(readCustomersById);
        }

        @Test
        @DisplayName("for aggregate reads with filters not set")
        void onAggregateFiltersNotSet() {
            StandStorage standStorageMock = mock(StandStorage.class);

            // Return non-empty results on any storage read call.
            EntityRecord someRecord = EntityRecord.getDefaultInstance();
            ImmutableList<EntityRecord> nonEmptyList =
                    ImmutableList.<EntityRecord>builder().add(someRecord)
                                                         .build();
            when(standStorageMock.readAllByType(any(TypeUrl.class)))
                    .thenReturn(nonEmptyList.iterator());
            when(standStorageMock.readAll())
                    .thenReturn(emptyIterator());
            when(standStorageMock.readMultiple(anyIterable()))
                    .thenReturn(nonEmptyList.iterator());
            when(standStorageMock.readMultiple(anyIterable()))
                    .thenReturn(nonEmptyList.iterator());

            Stand stand = prepareStandWithAggregateRepo(standStorageMock);

            Query noneOfCustomersQuery = requestFactory.query()
                                                       .byIds(Customer.class, emptySet());

            MemoizeQueryResponseObserver responseObserver =
                    new MemoizeQueryResponseObserver();
            stand.execute(noneOfCustomersQuery, responseObserver);

            verifyObserver(responseObserver);

            List<Any> messageList = checkAndGetMessageList(responseObserver);
            assertTrue(messageList.isEmpty(), "Query returned a non-empty response message list " +
                    "though the filter was not set");
        }
    }

    @Nested
    @DisplayName("return single result")
    class ReturnSingleResult {

        @Test
        @DisplayName("for aggregate state read by ID")
        void forAggregateReadById() {
            doCheckReadingCustomersById(1);
        }

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
        @DisplayName("for aggregate state batch read by IDs")
        void forAggregateBatchRead() {
            doCheckReadingCustomersById(TOTAL_CUSTOMERS_FOR_BATCH_READING);
        }

        @Test
        @DisplayName("for projection batch read by IDs")
        void forProjectionBatchRead() {
            doCheckReadingProjectsById(TOTAL_PROJECTS_FOR_BATCH_READING);
        }

        @Test
        @DisplayName("for projection batch read by IDs with field mask")
        void forProjectionReadWithMask() {
            List<Descriptors.FieldDescriptor> projectFields = Project.getDescriptor()
                                                                     .getFields();
            doCheckReadingCustomersByIdAndFieldMask(
                    projectFields.get(0)
                                 .getFullName(), // ID
                    projectFields.get(1)
                                 .getFullName()); // Name
        }
    }

    @Nested
    @DisplayName("trigger subscription callback")
    class TriggerSubscriptionCallback {

        @Test
        @DisplayName("upon update of aggregate")
        void uponUpdateOfAggregate() {
            Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
            Topic allCustomers = requestFactory.topic()
                                               .allOf(Customer.class);

            MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();

            subscribeAndActivate(stand, allCustomers, memoizeCallback);
            assertNull(memoizeCallback.newEntityState());

            Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                               .iterator()
                                                                               .next();
            CustomerId customerId = sampleData.getKey();
            Customer customer = sampleData.getValue();
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customerId, customer, stateVersion));

            Any packedState = AnyPacker.pack(customer);
            assertEquals(packedState, memoizeCallback.newEntityState());
        }

        @Test
        @DisplayName("upon update of projection")
        void uponUpdateOfProjection() {
            Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
            Topic allProjects = requestFactory.topic()
                                              .allOf(Project.class);

            MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
            subscribeAndActivate(stand, allProjects, memoizeCallback);
            assertNull(memoizeCallback.newEntityState());

            Map.Entry<ProjectId, Project> sampleData = fillSampleProjects(1).entrySet()
                                                                            .iterator()
                                                                            .next();
            ProjectId projectId = sampleData.getKey();
            Project project = sampleData.getValue();
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(projectId, project, stateVersion));

            Any packedState = AnyPacker.pack(project);
            assertEquals(packedState, memoizeCallback.newEntityState());
        }
    }

    @Test
    @DisplayName("trigger subscription callbacks matching by ID")
    void triggerSubscriptionsMatchingById() {
        Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));

        Map<CustomerId, Customer> sampleCustomers = fillSampleCustomers(10);

        Topic someCustomers = requestFactory.topic()
                                            .select(Customer.class)
                                            .byId(sampleCustomers.keySet())
                                            .build();
        Map<CustomerId, Customer> callbackStates = newHashMap();
        MemoizeEntityUpdateCallback callback = new MemoizeEntityUpdateCallback() {
            @Override
            public void onStateChanged(EntityStateUpdate update) {
                super.onStateChanged(update);
                Customer customerInCallback = unpack(update.getState());
                CustomerId customerIdInCallback = unpack(update.getId());
                callbackStates.put(customerIdInCallback, customerInCallback);
            }
        };
        subscribeAndActivate(stand, someCustomers, callback);

        for (Map.Entry<CustomerId, Customer> sampleEntry : sampleCustomers.entrySet()) {
            CustomerId customerId = sampleEntry.getKey();
            Customer customer = sampleEntry.getValue();
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customerId, customer, stateVersion));
        }

        assertEquals(newHashMap(sampleCustomers), callbackStates);
    }

    @Test
    @DisplayName("allow cancelling subscriptions")
    void cancelSubscriptions() {
        Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        Topic allCustomers = requestFactory.topic()
                                           .allOf(Customer.class);

        MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
        Subscription subscription =
                subscribeAndActivate(stand, allCustomers, memoizeCallback);

        stand.cancel(subscription, noOpObserver());

        Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                           .iterator()
                                                                           .next();
        CustomerId customerId = sampleData.getKey();
        Customer customer = sampleData.getValue();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        assertNull(memoizeCallback.newEntityState());
    }

    @Test
    @DisplayName("fail if cancelling non-existent subscription")
    void notCancelNonExistent() {
        Stand stand = Stand.newBuilder()
                           .build();
        Subscription inexistentSubscription = Subscription.newBuilder()
                                                          .setId(Subscriptions.generateId())
                                                          .build();
        assertThrows(IllegalArgumentException.class,
                     () -> stand.cancel(inexistentSubscription, noOpObserver()));
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    @DisplayName("trigger each subscription callback once for multiple subscriptions")
    void triggerSubscriptionCallbackOnce() {
        Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        Target allCustomers = Targets.allOf(Customer.class);

        Set<MemoizeEntityUpdateCallback> callbacks = newHashSet();
        int totalCallbacks = 100;

        for (int callbackIndex = 0; callbackIndex < totalCallbacks; callbackIndex++) {
            MemoizeEntityUpdateCallback callback = subscribeWithCallback(stand, allCustomers);
            callbacks.add(callback);
        }

        Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                           .iterator()
                                                                           .next();
        CustomerId customerId = sampleData.getKey();
        Customer customer = sampleData.getValue();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        Any packedState = AnyPacker.pack(customer);
        for (MemoizeEntityUpdateCallback callback : callbacks) {
            assertEquals(packedState, callback.newEntityState());
            verify(callback, times(1)).onStateChanged(any(EntityStateUpdate.class));
        }
    }

    @Test
    @DisplayName("not trigger subscription callbacks in case of another type criterion mismatch")
    void notTriggerOnTypeMismatch() {
        Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        Target allProjects = Targets.allOf(Project.class);
        MemoizeEntityUpdateCallback callback = subscribeWithCallback(stand, allProjects);

        Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                           .iterator()
                                                                           .next();
        CustomerId customerId = sampleData.getKey();
        Customer customer = sampleData.getValue();
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
                        .setId(String.valueOf(numericId))
                        .build();
    }

    @Test
    @DisplayName("retrieve all data if field mask is not set")
    void readAllIfMaskNotSet() {
        Stand stand = prepareStandWithAggregateRepo(createStandStorage());

        Customer sampleCustomer = getSampleCustomer();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(sampleCustomer.getId(), sampleCustomer, stateVersion));

        Query customerQuery = requestFactory.query()
                                            .all(Customer.class);

        //noinspection OverlyComplexAnonymousInnerClass
        MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                Customer customer = unpack(messages.get(0));
                for (Descriptors.FieldDescriptor field : customer.getDescriptorForType()
                                                                 .getFields()) {
                    assertTrue(customer.getField(field)
                                       .equals(sampleCustomer.getField(field)));
                }
            }
        };

        stand.execute(customerQuery, observer);

        verifyObserver(observer);
    }

    @Test
    @DisplayName("retrieve only selected parameter for query")
    void readOnlySelectedParam() {
        requestSampleCustomer(new int[]{Customer.NAME_FIELD_NUMBER - 1}, new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);

                List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                Customer sampleCustomer = getSampleCustomer();
                Customer customer = unpack(messages.get(0));
                assertTrue(customer.getName()
                                   .equals(sampleCustomer.getName()));
                assertFalse(customer.hasId());
                assertTrue(customer.getNicknamesList()
                                   .isEmpty());
            }
        });
    }

    @Test
    @DisplayName("retrieve collection fields if they are required")
    void readCollectionFields() {
        requestSampleCustomer(
                new int[]{Customer.NICKNAMES_FIELD_NUMBER - 1},
                new MemoizeQueryResponseObserver() {
                    @Override
                    public void onNext(QueryResponse value) {
                        super.onNext(value);

                        List<Any> messages = value.getMessagesList();
                        assertFalse(messages.isEmpty());

                        Customer sampleCustomer = getSampleCustomer();
                        Customer customer = unpack(messages.get(0));
                        assertEquals(customer.getNicknamesList(),
                                     sampleCustomer.getNicknamesList());

                        assertFalse(customer.hasName());
                        assertFalse(customer.hasId());
                    }
                }
        );
    }

    @Test
    @DisplayName("retrieve all requested fields")
    void readAllRequestedFields() {
        requestSampleCustomer(
                new int[]{Customer.NICKNAMES_FIELD_NUMBER - 1,
                        Customer.ID_FIELD_NUMBER - 1},
                new MemoizeQueryResponseObserver() {
                    @Override
                    public void onNext(QueryResponse value) {
                        super.onNext(value);

                        List<Any> messages = value.getMessagesList();
                        assertFalse(messages.isEmpty());

                        Customer sampleCustomer = getSampleCustomer();
                        Customer customer = unpack(messages.get(0));
                        assertEquals(customer.getNicknamesList(),
                                     sampleCustomer.getNicknamesList());

                        assertFalse(customer.hasName());
                        assertTrue(customer.hasId());
                    }
                }
        );
    }

    @SuppressWarnings("ZeroLengthArrayAllocation") // It is necessary for test.
    @Test
    @DisplayName("retrieve whole entity if nothing is requested")
    void readEntityIfNothingRequested() {
        requestSampleCustomer(new int[]{}, getDuplicateCostumerStreamObserver());
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    @DisplayName("select entity singleton by ID and apply field masks")
    void selectByIdAndApplyMasks() {
        Stand stand = prepareStandWithAggregateRepo(createStandStorage());
        String customerDescriptor = Customer.getDescriptor()
                                            .getFullName();
        @SuppressWarnings("DuplicateStringLiteralInspection")   // clashes with non-related tests.
                String[] paths = {customerDescriptor + ".id", customerDescriptor + ".name"};
        FieldMask fieldMask = FieldMask.newBuilder()
                                       .addAllPaths(Arrays.asList(paths))
                                       .build();

        List<Customer> customers = Lists.newLinkedList();
        int count = 10;

        for (int i = 0; i < count; i++) {
            // Has new ID each time
            Customer customer = getSampleCustomer();
            customers.add(customer);
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customer.getId(), customer, stateVersion));
        }

        Set<CustomerId> ids = Collections.singleton(customers.get(0)
                                                             .getId());
        Query customerQuery = requestFactory.query()
                                            .byIdsWithMask(Customer.class, ids, paths);

        MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver();
        stand.execute(customerQuery, observer);

        List<Any> read = observer.responseHandled()
                                 .getMessagesList();
        Verify.assertSize(1, read);

        Customer customer = unpack(read.get(0));
        assertMatches(customer, fieldMask);
        assertTrue(ids.contains(customer.getId()));

        verifyObserver(observer);
    }

    @Test
    @DisplayName("handle mistakes in query silently")
    void handleMistakesInQuery() {
        //noinspection ZeroLengthArrayAllocation
        Stand stand = prepareStandWithAggregateRepo(createStandStorage());

        Customer sampleCustomer = getSampleCustomer();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(sampleCustomer.getId(), sampleCustomer, stateVersion));

        // FieldMask with invalid type URLs.
        String[] paths = {"invalid_type_url_example", Project.getDescriptor()
                                                             .getFields()
                                                             .get(2).getFullName()};

        Query customerQuery = requestFactory.query()
                                            .allWithMask(Customer.class, paths);

        MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                Customer customer = unpack(messages.get(0));

                assertNotEquals(customer, null);

                assertFalse(customer.hasId());
                assertFalse(customer.hasName());
                assertTrue(customer.getNicknamesList()
                                   .isEmpty());
            }
        };

        stand.execute(customerQuery, observer);

        verifyObserver(observer);
    }

    @Nested
    @DisplayName("throw invalid query exception packed as IAE")
    class ThrowInvalidQueryEx {

        @Test
        @DisplayName("if querying unknown type")
        void ifQueryingUnknownType() {

            Stand stand = Stand.newBuilder()
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

            Stand stand = Stand.newBuilder()
                               .build();
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
                               .build();
            checkTypesEmpty(stand);

            // Customer type was NOT registered.
            // So create a topic for an unknown type.
            Topic allCustomersTopic = requestFactory.topic()
                                                    .allOf(Customer.class);

            try {
                stand.subscribe(allCustomersTopic, noOpObserver());
                fail("Expected IllegalArgumentException upon subscribing to a topic " +
                             "with unknown target, but got nothing");
            } catch (IllegalArgumentException e) {
                verifyUnsupportedTopicException(allCustomersTopic, e);
            }
        }

        @Test
        @DisplayName("if invalid topic message is passed")
        void ifInvalidTopicMessagePassed() {

            Stand stand = Stand.newBuilder()
                               .build();

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

            Stand stand = Stand.newBuilder()
                               .build();
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

            Stand stand = Stand.newBuilder()
                               .build();
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

            Stand stand = Stand.newBuilder()
                               .build();
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

            Stand stand = Stand.newBuilder()
                               .build();
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

    private StandStorage createStandStorage() {
        BoundedContext bc = BoundedContext.newBuilder()
                                          .setMultitenant(multitenant)
                                          .build();
        return bc.getStorageFactory()
                 .createStandStorage();
    }

    private static void verifyObserver(MemoizeQueryResponseObserver observer) {
        assertNotNull(observer.responseHandled());
        assertTrue(observer.isCompleted());
        assertNull(observer.throwable());
    }

    private static MemoizeQueryResponseObserver getDuplicateCostumerStreamObserver() {
        return new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);

                List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                Customer customer = unpack(messages.get(0));
                Customer sampleCustomer = getSampleCustomer();

                assertEquals(sampleCustomer.getName(), customer.getName());
                assertEquals(sampleCustomer.getNicknamesList(), customer.getNicknamesList());
                assertTrue(customer.hasId());
            }
        };
    }

    private static Customer getSampleCustomer() {
        //noinspection NumericCastThatLosesPrecision
        return Customer.newBuilder()
                       .setId(CustomerId.newBuilder()
                                        .setNumber((int) UUID.randomUUID()
                                                             .getLeastSignificantBits()))
                       .setName(PersonName.newBuilder()
                                          .setGivenName("John")
                                          .build())
                       .addNicknames(PersonName.newBuilder()
                                               .setGivenName("Johnny"))
                       .addNicknames(PersonName.newBuilder()
                                               .setGivenName("Big Guy"))
                       .build();

    }

    private void requestSampleCustomer(int[] fieldIndexes,
                                       MemoizeQueryResponseObserver observer) {
        Stand stand = prepareStandWithAggregateRepo(createStandStorage());

        Customer sampleCustomer = getSampleCustomer();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(sampleCustomer.getId(), sampleCustomer, stateVersion));

        String[] paths = new String[fieldIndexes.length];

        for (int i = 0; i < fieldIndexes.length; i++) {
            paths[i] = Customer.getDescriptor()
                               .getFields()
                               .get(fieldIndexes[i])
                               .getFullName();
        }

        Query customerQuery = requestFactory.query()
                                            .allWithMask(Customer.class, paths);

        stand.execute(customerQuery, observer);

        verifyObserver(observer);
    }

    @SuppressWarnings("unchecked") // Mock instance of no type params
    private void checkEmptyResultForTargetOnEmptyStorage(Query readCustomersQuery) {
        StandStorage standStorageMock = mock(StandStorage.class);
        when(standStorageMock.readAllByType(any(TypeUrl.class)))
                .thenReturn(emptyIterator());
        when(standStorageMock.readMultiple(any(Iterable.class)))
                .thenReturn(Collections.<EntityRecord>emptyIterator());

        Stand stand = prepareStandWithAggregateRepo(standStorageMock);

        MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readCustomersQuery, responseObserver);

        List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertTrue(messageList.isEmpty(),
                   "Query returned a non-empty response message list though the target had been " +
                           "empty");
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
            Project unpackedSingleResult = unpack(singleRecord);
            assertTrue(allCustomers.contains(unpackedSingleResult));
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private void doCheckReadingCustomersByIdAndFieldMask(String... paths) {
        Stand stand = prepareStandWithAggregateRepo(createStandStorage());

        int querySize = 2;

        Set<CustomerId> ids = new HashSet<>();
        for (int i = 0; i < querySize; i++) {
            Customer customer = getSampleCustomer().toBuilder()
                                                   .setId(CustomerId.newBuilder()
                                                                    .setNumber(i))
                                                   .build();
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customer.getId(), customer, stateVersion));

            ids.add(customer.getId());
        }

        Query customerQuery =
                requestFactory.query()
                              .byIdsWithMask(Customer.class, ids, paths);

        FieldMask fieldMask = FieldMask.newBuilder()
                                       .addAllPaths(Arrays.asList(paths))
                                       .build();

        MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                List<Any> messages = value.getMessagesList();
                Verify.assertSize(ids.size(), messages);

                for (Any message : messages) {
                    Customer customer = unpack(message);

                    assertNotEquals(customer, null);

                    assertMatches(customer, fieldMask);
                }
            }
        };

        stand.execute(customerQuery, observer);

        verifyObserver(observer);
    }

    @CanIgnoreReturnValue
    Stand doCheckReadingCustomersById(int numberOfCustomers) {
        // Define the types and values used as a test data.
        TypeUrl customerType = TypeUrl.of(Customer.class);
        Map<CustomerId, Customer> sampleCustomers = fillSampleCustomers(numberOfCustomers);

        // Prepare the stand and its storage to act.
        StandStorage standStorage = setupStandStorageWithCustomers(sampleCustomers,
                                                                   customerType);
        Stand stand = prepareStandWithAggregateRepo(standStorage);

        triggerMultipleUpdates(sampleCustomers, stand);

        Query readMultipleCustomers = requestFactory.query()
                                                    .byIds(Customer.class,
                                                           sampleCustomers.keySet());

        MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readMultipleCustomers, responseObserver);

        List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertEquals(sampleCustomers.size(), messageList.size());
        Collection<Customer> allCustomers = sampleCustomers.values();
        for (Any singleRecord : messageList) {
            Customer unpackedSingleResult = unpack(singleRecord);
            assertTrue(allCustomers.contains(unpackedSingleResult));
        }
        return stand;
    }

    private StandStorage setupStandStorageWithCustomers(Map<CustomerId, Customer> sampleCustomers,
                                                        TypeUrl customerType) {
        BoundedContext bc = BoundedContext.newBuilder()
                                          .setMultitenant(isMultitenant())
                                          .build();
        StandStorage standStorage = bc.getStorageFactory()
                                      .createStandStorage();

        ImmutableList.Builder<AggregateStateId> stateIdsBuilder = ImmutableList.builder();
        ImmutableList.Builder<EntityRecord> recordsBuilder = ImmutableList.builder();
        for (CustomerId customerId : sampleCustomers.keySet()) {
            AggregateStateId stateId = AggregateStateId.of(customerId, customerType);
            Customer customer = sampleCustomers.get(customerId);
            Any customerState = AnyPacker.pack(customer);
            EntityRecord entityRecord = EntityRecord.newBuilder()
                                                    .setState(customerState)
                                                    .build();
            stateIdsBuilder.add(stateId);
            recordsBuilder.add(entityRecord);

            standStorage.write(stateId, entityRecord);
        }

        return standStorage;
    }

    @SuppressWarnings("ConstantConditions")
    private static void setupExpectedFindAllBehaviour(
            Map<ProjectId, Project> sampleProjects,
            StandTestProjectionRepository projectionRepository) {

        Set<ProjectId> projectIds = sampleProjects.keySet();
        ImmutableCollection<Given.StandTestProjection> allResults =
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

        when(projectionRepository.find(argThat(entityFilterMatcher(projectIds)), eq(Order.getDefaultInstance()),
                                       eq(Pagination.getDefaultInstance()),
                                       any(FieldMask.class)))
                .thenReturn(allResults.iterator());
    }

    @SuppressWarnings("OverlyComplexAnonymousInnerClass")
    private static ArgumentMatcher<EntityFilters> entityFilterMatcher(
            Collection<ProjectId> projectIds) {
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

    private static ImmutableCollection<Given.StandTestProjection> toProjectionCollection(
            Collection<ProjectId> values) {
        Collection<Given.StandTestProjection> transformed = Collections2.transform(
                values,
                input -> {
                    checkNotNull(input);
                    return new StandTestProjection(input);
                });
        ImmutableList<Given.StandTestProjection> result = ImmutableList.copyOf(transformed);
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

    private void triggerMultipleUpdates(Map<CustomerId, Customer> sampleCustomers, Stand stand) {
        // Trigger the aggregate state updates.
        for (CustomerId id : sampleCustomers.keySet()) {
            Customer sampleCustomer = sampleCustomers.get(id);
            Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(id, sampleCustomer, stateVersion));
        }
    }

    protected static Map<CustomerId, Customer> fillSampleCustomers(int numberOfCustomers) {
        Map<CustomerId, Customer> sampleCustomers = newHashMap();

        @SuppressWarnings("UnsecureRandomNumberGeneration") Random randomizer = new Random(
                Integer.MAX_VALUE);
            // force non-negative numeric ID values.

        for (int customerIndex = 0; customerIndex < numberOfCustomers; customerIndex++) {

            int numericId = randomizer.nextInt();
            CustomerId customerId = customerIdFor(numericId);
            Customer customer = Customer.newBuilder()
                                        .setName(PersonName.newBuilder()
                                                           .setGivenName(String.valueOf(numericId)))
                                        .build();
            sampleCustomers.put(customerId, customer);
        }
        return sampleCustomers;
    }

    private static Map<ProjectId, Project> fillSampleProjects(int numberOfProjects) {
        Map<ProjectId, Project> sampleProjects = newHashMap();

        @SuppressWarnings("UnsecureRandomNumberGeneration") Random randomizer = new Random(
                Integer.MAX_VALUE);
        // Force non-negative numeric ID values.

        for (int projectIndex = 0; projectIndex < numberOfProjects; projectIndex++) {

            int numericId = randomizer.nextInt();
            ProjectId customerId = projectIdFor(numericId);

            Project project = Project.newBuilder()
                                     .setName(String.valueOf(numericId))
                                     .build();
            sampleProjects.put(customerId, project);
        }
        return sampleProjects;
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

    protected Stand prepareStandWithAggregateRepo(StandStorage standStorage) {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .setMultitenant(multitenant)
                                                      .setStand(Stand.newBuilder()
                                                                     .setStorage(standStorage))
                                                      .build();

        Stand stand = boundedContext.getStand();
        assertNotNull(stand);

        CustomerAggregateRepository customerAggregateRepo = new CustomerAggregateRepository();
        stand.registerTypeSupplier(customerAggregateRepo);
        StandTestProjectionRepository projectProjectionRepo = new StandTestProjectionRepository();
        stand.registerTypeSupplier(projectProjectionRepo);
        return stand;
    }

    private static Stand prepareStandWithProjectionRepo(ProjectionRepository projectionRepository) {
        Stand stand = Stand.newBuilder()
                           .build();
        assertNotNull(stand);
        stand.registerTypeSupplier(projectionRepository);
        return stand;
    }

    private static EntityRecord recordStateMatcher(EntityRecord expectedRecord) {
        return argThat(argument -> {
            boolean matchResult = Objects.equals(expectedRecord.getState(),
                                                 argument.getState());
            return matchResult;
        });
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

    private static void assertMatches(Message message, FieldMask fieldMask) {
        List<String> paths = fieldMask.getPathsList();
        for (Descriptors.FieldDescriptor field : message.getDescriptorForType()
                                                        .getFields()) {

            // Protobuf limitation, has no effect on the test.
            if (field.isRepeated()) {
                continue;
            }

            assertEquals(message.hasField(field), paths.contains(field.getFullName()));
        }
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
}
