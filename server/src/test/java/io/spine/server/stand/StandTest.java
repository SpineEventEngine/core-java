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

import com.google.common.base.Optional;
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
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.Record;
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
        final Stand.Builder builder = Stand.newBuilder()
                                           .setMultitenant(isMultitenant());
        final Stand stand = builder.build();

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
            final boolean multitenant = isMultitenant();
            final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                                .setMultitenant(multitenant)
                                                                .build();
            final Stand stand = boundedContext.getStand();

            checkTypesEmpty(stand);

            final StandTestProjectionRepository standTestProjectionRepo =
                    new StandTestProjectionRepository();
            stand.registerTypeSupplier(standTestProjectionRepo);
            checkHasExactlyOne(stand.getExposedTypes(), Project.getDescriptor());

            final ImmutableSet<TypeUrl> knownAggregateTypes = stand.getExposedAggregateTypes();
            // As we registered a projection repo, known aggregate types should be still empty.
            assertTrue(knownAggregateTypes.isEmpty(),
                       "For some reason an aggregate type was registered");

            final StandTestProjectionRepository anotherTestProjectionRepo =
                    new StandTestProjectionRepository();
            stand.registerTypeSupplier(anotherTestProjectionRepo);
            checkHasExactlyOne(stand.getExposedTypes(), Project.getDescriptor());
        }

        @Test
        @DisplayName("aggregate repositories")
        void aggregateRepositories() {
            final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                                .build();
            final Stand stand = boundedContext.getStand();

            checkTypesEmpty(stand);

            final CustomerAggregateRepository customerAggregateRepo =
                    new CustomerAggregateRepository();
            stand.registerTypeSupplier(customerAggregateRepo);

            final Descriptors.Descriptor customerEntityDescriptor = Customer.getDescriptor();
            checkHasExactlyOne(stand.getExposedTypes(), customerEntityDescriptor);
            checkHasExactlyOne(stand.getExposedAggregateTypes(), customerEntityDescriptor);

            @SuppressWarnings("LocalVariableNamingConvention")
            final CustomerAggregateRepository anotherCustomerAggregateRepo =
                    new CustomerAggregateRepository();
            stand.registerTypeSupplier(anotherCustomerAggregateRepo);
            checkHasExactlyOne(stand.getExposedTypes(), customerEntityDescriptor);
            checkHasExactlyOne(stand.getExposedAggregateTypes(), customerEntityDescriptor);
        }
    }

    @Test
    @DisplayName("use provided executor upon update of watched type")
    void useProvidedExecutor() {
        final Executor executor = mock(Executor.class);
        final BoundedContext boundedContext =
                BoundedContext.newBuilder()
                              .setStand(Stand.newBuilder()
                                             .setCallbackExecutor(executor))
                              .build();
        final Stand stand = boundedContext.getStand();

        final StandTestProjectionRepository standTestProjectionRepo =
                new StandTestProjectionRepository();
        stand.registerTypeSupplier(standTestProjectionRepo);

        final Topic projectProjections = requestFactory.topic().allOf(Project.class);

        final MemoizingObserver<Subscription> observer = memoizingObserver();
        stand.subscribe(projectProjections, observer);
        final Subscription subscription = observer.firstResponse();

        final StreamObserver<Response> noopObserver = noOpObserver();
        stand.activate(subscription, emptyUpdateCallback(), noopObserver);
        assertNotNull(subscription);

        verify(executor, never()).execute(any(Runnable.class));

        final ProjectId someId = ProjectId.getDefaultInstance();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(someId, Project.getDefaultInstance(), stateVersion));

        verify(executor, times(1)).execute(any(Runnable.class));
    }

    @SuppressWarnings("OverlyCoupledMethod")
    @Test
    @DisplayName("operate with storage provided through builder")
    void operateWithStorage() {
        final StandStorage standStorageMock = mock(StandStorage.class);
        final BoundedContext boundedContext =
                BoundedContext.newBuilder()
                              .setStand(Stand.newBuilder()
                                             .setStorage(standStorageMock))
                              .build();
        final Stand stand = boundedContext.getStand();

        assertNotNull(stand);

        final CustomerAggregateRepository customerAggregateRepo =
                new CustomerAggregateRepository();
        boundedContext.register(customerAggregateRepo);

        final int numericIdValue = 17;
        final CustomerId customerId = customerIdFor(numericIdValue);
        final CustomerAggregate customerAggregate = customerAggregateRepo.create(customerId);
        final Customer customerState = customerAggregate.getState();
        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final Version stateVersion = GivenVersion.withNumber(1);

        verify(standStorageMock, never()).write(any(AggregateStateId.class),
                                                any(EntityRecordWithColumns.class));

        stand.update(asEnvelope(customerId, customerState, stateVersion));

        final AggregateStateId expectedAggregateStateId =
                AggregateStateId.of(customerId, customerType);
        final Any packedState = AnyPacker.pack(customerState);
        final EntityRecord expectedRecord = EntityRecord.newBuilder()
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
            final Query readAllCustomers = requestFactory.query().all(Customer.class);
            checkEmptyResultForTargetOnEmptyStorage(readAllCustomers);
        }

        @Test
        @DisplayName("on read by IDs when empty")
        void onReadByIdWhenEmpty() {

            final Query readCustomersById = requestFactory.query()
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
            final StandStorage standStorageMock = mock(StandStorage.class);

            // Return non-empty results on any storage read call.
            final EntityRecord someRecord = EntityRecord.getDefaultInstance();
            final ImmutableList<EntityRecord> nonEmptyList =
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

            final Stand stand = prepareStandWithAggregateRepo(standStorageMock);

            final Query noneOfCustomersQuery = requestFactory.query()
                                                             .byIds(Customer.class, emptySet());

            final MemoizeQueryResponseObserver responseObserver =
                    new MemoizeQueryResponseObserver();
            stand.execute(noneOfCustomersQuery, responseObserver);

            verifyObserver(responseObserver);

            final List<Record> recordList = checkAndGetRecordList(responseObserver);
            assertTrue(recordList.isEmpty(), "Query returned a non-empty response record list " +
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
            final List<Descriptors.FieldDescriptor> projectFields = Project.getDescriptor()
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
            final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
            final Topic allCustomers = requestFactory.topic().allOf(Customer.class);

            final MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();

            subscribeAndActivate(stand, allCustomers, memoizeCallback);
            assertNull(memoizeCallback.newEntityState());

            final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                     .iterator()
                                                                                     .next();
            final CustomerId customerId = sampleData.getKey();
            final Customer customer = sampleData.getValue();
            final Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customerId, customer, stateVersion));

            final Any packedState = AnyPacker.pack(customer);
            assertEquals(packedState, memoizeCallback.newEntityState());
        }

        @Test
        @DisplayName("upon update of projection")
        void uponUpdateOfProjection() {
            final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
            final Topic allProjects = requestFactory.topic().allOf(Project.class);

            final MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
            subscribeAndActivate(stand, allProjects, memoizeCallback);
            assertNull(memoizeCallback.newEntityState());

            final Map.Entry<ProjectId, Project> sampleData = fillSampleProjects(1).entrySet()
                                                                                  .iterator()
                                                                                  .next();
            final ProjectId projectId = sampleData.getKey();
            final Project project = sampleData.getValue();
            final Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(projectId, project, stateVersion));

            final Any packedState = AnyPacker.pack(project);
            assertEquals(packedState, memoizeCallback.newEntityState());
        }
    }

    @Test
    @DisplayName("trigger subscription callbacks matching by ID")
    void triggerSubscriptionsMatchingById() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));

        final Map<CustomerId, Customer> sampleCustomers = fillSampleCustomers(10);

        final Topic someCustomers = requestFactory.topic()
                                                  .someOf(Customer.class,
                                                          sampleCustomers.keySet());
        final Map<CustomerId, Customer> callbackStates = newHashMap();
        final MemoizeEntityUpdateCallback callback = new MemoizeEntityUpdateCallback() {
            @Override
            public void onStateChanged(EntityStateUpdate update) {
                super.onStateChanged(update);
                final Customer customerInCallback = unpack(update.getState());
                final CustomerId customerIdInCallback = unpack(update.getId());
                callbackStates.put(customerIdInCallback, customerInCallback);
            }
        };
        subscribeAndActivate(stand, someCustomers, callback);

        for (Map.Entry<CustomerId, Customer> sampleEntry : sampleCustomers.entrySet()) {
            final CustomerId customerId = sampleEntry.getKey();
            final Customer customer = sampleEntry.getValue();
            final Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customerId, customer, stateVersion));
        }

        assertEquals(newHashMap(sampleCustomers), callbackStates);
    }

    @Test
    @DisplayName("allow cancelling subscriptions")
    void cancelSubscriptions() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Topic allCustomers = requestFactory.topic().allOf(Customer.class);

        final MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
        final Subscription subscription =
                subscribeAndActivate(stand, allCustomers, memoizeCallback);

        stand.cancel(subscription, noOpObserver());

        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        assertNull(memoizeCallback.newEntityState());
    }

    @Test
    @DisplayName("fail if cancelling non-existent subscription")
    void notCancelNonExistent() {
        final Stand stand = Stand.newBuilder()
                                 .build();
        final Subscription inexistentSubscription = Subscription.newBuilder()
                                                                .setId(Subscriptions.generateId())
                                                                .build();
        assertThrows(IllegalArgumentException.class,
                     () -> stand.cancel(inexistentSubscription, noOpObserver()));
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    @DisplayName("trigger each subscription callback once for multiple subscriptions")
    void triggerSubscriptionCallbackOnce() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Target allCustomers = Targets.allOf(Customer.class);

        final Set<MemoizeEntityUpdateCallback> callbacks = newHashSet();
        final int totalCallbacks = 100;

        for (int callbackIndex = 0; callbackIndex < totalCallbacks; callbackIndex++) {
            final MemoizeEntityUpdateCallback callback = subscribeWithCallback(stand, allCustomers);
            callbacks.add(callback);
        }

        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        final Any packedState = AnyPacker.pack(customer);
        for (MemoizeEntityUpdateCallback callback : callbacks) {
            assertEquals(packedState, callback.newEntityState());
            verify(callback, times(1)).onStateChanged(any(EntityStateUpdate.class));
        }
    }

    @Test
    @DisplayName("not trigger subscription callbacks in case of another type criterion mismatch")
    void notTriggerOnTypeMismatch() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Target allProjects = Targets.allOf(Project.class);
        final MemoizeEntityUpdateCallback callback = subscribeWithCallback(stand, allProjects);

        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        verify(callback, never()).onStateChanged(any(EntityStateUpdate.class));
    }

    private MemoizeEntityUpdateCallback
    subscribeWithCallback(Stand stand, Target subscriptionTarget) {
        final MemoizeEntityUpdateCallback callback = spy(new MemoizeEntityUpdateCallback());
        final Topic topic = requestFactory.topic().forTarget(subscriptionTarget);
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
        final Stand stand = prepareStandWithAggregateRepo(createStandStorage());

        final Customer sampleCustomer = getSampleCustomer();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(sampleCustomer.getId(), sampleCustomer, stateVersion));

        final Query customerQuery = requestFactory.query().all(Customer.class);

        //noinspection OverlyComplexAnonymousInnerClass
        final MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                final List<Record> records = value.getRecordsList();
                assertFalse(records.isEmpty());

                final Record record = records.get(0);
                final Customer customer = unpack(record.getState());
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

                final List<Record> records = value.getRecordsList();
                assertFalse(records.isEmpty());

                final Customer sampleCustomer = getSampleCustomer();
                final Record record = records.get(0);
                final Customer customer = unpack(record.getState());
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

                        final List<Record> records = value.getRecordsList();
                        assertFalse(records.isEmpty());

                        final Customer sampleCustomer = getSampleCustomer();
                        final Record record = records.get(0);
                        final Customer customer = unpack(record.getState());
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

                        final List<Record> records = value.getRecordsList();
                        assertFalse(records.isEmpty());

                        final Customer sampleCustomer = getSampleCustomer();
                        final Record record = records.get(0);
                        final Customer customer = unpack(record.getState());
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
        final Stand stand = prepareStandWithAggregateRepo(createStandStorage());
        final String customerDescriptor = Customer.getDescriptor()
                                                  .getFullName();
        @SuppressWarnings("DuplicateStringLiteralInspection")   // clashes with non-related tests.
        final String[] paths = {customerDescriptor + ".id", customerDescriptor + ".name"};
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(paths))
                                             .build();

        final List<Customer> customers = Lists.newLinkedList();
        final int count = 10;

        for (int i = 0; i < count; i++) {
            // Has new ID each time
            final Customer customer = getSampleCustomer();
            customers.add(customer);
            final Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customer.getId(), customer, stateVersion));
        }

        final Set<CustomerId> ids = Collections.singleton(customers.get(0)
                                                                   .getId());
        final Query customerQuery = requestFactory.query()
                                                  .byIdsWithMask(Customer.class, ids, paths);

        final MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver();
        stand.execute(customerQuery, observer);

        final List<Record> read = observer.responseHandled().getRecordsList();
        Verify.assertSize(1, read);

        final Record record = read.get(0);
        final Customer customer = unpack(record.getState());
        assertMatches(customer, fieldMask);
        assertTrue(ids.contains(customer.getId()));

        verifyObserver(observer);
    }

    @Test
    @DisplayName("handle mistakes in query silently")
    void handleMistakesInQuery() {
        //noinspection ZeroLengthArrayAllocation
        final Stand stand = prepareStandWithAggregateRepo(createStandStorage());

        final Customer sampleCustomer = getSampleCustomer();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(sampleCustomer.getId(), sampleCustomer, stateVersion));

        // FieldMask with invalid type URLs.
        final String[] paths = {"invalid_type_url_example", Project.getDescriptor()
                                                                   .getFields()
                                                                   .get(2).getFullName()};

        final Query customerQuery = requestFactory.query().allWithMask(Customer.class, paths);

        final MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                final List<Record> records = value.getRecordsList();
                assertFalse(records.isEmpty());

                // todo extract method unpackCustomer or something
                final Record record = records.get(0);
                final Customer customer = unpack(record.getState());

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

            final Stand stand = Stand.newBuilder()
                                     .build();

            checkTypesEmpty(stand);

            // Customer type was NOT registered.
            // So create a query for an unknown type.
            final Query readAllCustomers = requestFactory.query()
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

            final Stand stand = Stand.newBuilder()
                                     .build();
            final Query invalidQuery = Query.getDefaultInstance();

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
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidQueryException);

            final InvalidQueryException queryException = (InvalidQueryException) cause;
            assertEquals(query, queryException.getRequest());

            assertEquals(UNSUPPORTED_QUERY_TARGET.getNumber(),
                         queryException.asError()
                                       .getCode());
        }

        private void verifyInvalidQueryException(Query invalidQuery,
                                                 IllegalArgumentException e) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidQueryException);

            final InvalidQueryException queryException = (InvalidQueryException) cause;
            assertEquals(invalidQuery, queryException.getRequest());

            assertEquals(INVALID_QUERY.getNumber(),
                         queryException.asError()
                                       .getCode());

            final ValidationError validationError = queryException.asError()
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

            final Stand stand = Stand.newBuilder()
                                     .build();
            checkTypesEmpty(stand);

            // Customer type was NOT registered.
            // So create a topic for an unknown type.
            final Topic allCustomersTopic = requestFactory.topic()
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

            final Stand stand = Stand.newBuilder()
                                     .build();

            final Topic invalidTopic = Topic.getDefaultInstance();

            try {
                stand.subscribe(invalidTopic, noOpObserver());
                fail("Expected IllegalArgumentException due to an invalid topic message, " +
                             "but got nothing");
            } catch (IllegalArgumentException e) {
                verifyInvalidTopicException(invalidTopic, e);
            }
        }

        private void verifyUnsupportedTopicException(Topic topic, IllegalArgumentException e) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidTopicException);

            final InvalidTopicException topicException = (InvalidTopicException) cause;
            assertEquals(topic, topicException.getRequest());

            assertEquals(UNSUPPORTED_TOPIC_TARGET.getNumber(),
                         topicException.asError()
                                       .getCode());
        }

        private void verifyInvalidTopicException(Topic invalidTopic,
                                                 IllegalArgumentException e) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidTopicException);

            final InvalidTopicException topicException = (InvalidTopicException) cause;
            assertEquals(invalidTopic, topicException.getRequest());

            assertEquals(INVALID_TOPIC.getNumber(),
                         topicException.asError()
                                       .getCode());

            final ValidationError validationError = topicException.asError()
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

            final Stand stand = Stand.newBuilder()
                                     .build();
            final Subscription subscription = subscriptionWithUnknownTopic();

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

            final Stand stand = Stand.newBuilder()
                                     .build();
            final Subscription subscription = subscriptionWithUnknownTopic();

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

            final Stand stand = Stand.newBuilder()
                                     .build();
            final Subscription invalidSubscription = Subscription.getDefaultInstance();

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

            final Stand stand = Stand.newBuilder()
                                     .build();
            final Subscription invalidSubscription = Subscription.getDefaultInstance();

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
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidSubscriptionException);

            final InvalidSubscriptionException exception = (InvalidSubscriptionException) cause;
            assertEquals(invalidSubscription, exception.getRequest());

            assertEquals(SubscriptionValidationError.INVALID_SUBSCRIPTION.getNumber(),
                         exception.asError()
                                  .getCode());

            final ValidationError validationError = exception.asError()
                                                             .getValidationError();
            assertTrue(Validate.isNotDefault(validationError));
        }

        private void verifyUnknownSubscriptionException(IllegalArgumentException e,
                                                        Subscription subscription) {
            final Throwable cause = e.getCause();
            assertTrue(cause instanceof InvalidSubscriptionException);

            final InvalidSubscriptionException exception = (InvalidSubscriptionException) cause;
            assertEquals(subscription, exception.getRequest());

            assertEquals(SubscriptionValidationError.UNKNOWN_SUBSCRIPTION.getNumber(),
                         exception.asError()
                                  .getCode());
        }

        private Subscription subscriptionWithUnknownTopic() {
            final Topic allCustomersTopic = requestFactory.topic()
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
        final MemoizingObserver<Subscription> observer = memoizingObserver();
        stand.subscribe(topic, observer);
        final Subscription subscription = observer.firstResponse();
        stand.activate(subscription, callback, noOpObserver());

        assertNotNull(subscription);
        return subscription;
    }

    private StandStorage createStandStorage() {
        final BoundedContext bc = BoundedContext.newBuilder()
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

                final List<Record> records = value.getRecordsList();
                assertFalse(records.isEmpty());

                final Record record = records.get(0);
                final Customer customer = unpack(record.getState());
                final Customer sampleCustomer = getSampleCustomer();

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
                                       final MemoizeQueryResponseObserver observer) {
        final Stand stand = prepareStandWithAggregateRepo(createStandStorage());

        final Customer sampleCustomer = getSampleCustomer();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(sampleCustomer.getId(), sampleCustomer, stateVersion));

        final String[] paths = new String[fieldIndexes.length];

        for (int i = 0; i < fieldIndexes.length; i++) {
            paths[i] = Customer.getDescriptor()
                               .getFields()
                               .get(fieldIndexes[i])
                               .getFullName();
        }

        final Query customerQuery = requestFactory.query().allWithMask(Customer.class, paths);

        stand.execute(customerQuery, observer);

        verifyObserver(observer);
    }

    @SuppressWarnings("unchecked") // Mock instance of no type params
    private void checkEmptyResultForTargetOnEmptyStorage(Query readCustomersQuery) {
        final StandStorage standStorageMock = mock(StandStorage.class);
        when(standStorageMock.readAllByType(any(TypeUrl.class)))
                .thenReturn(emptyIterator());
        when(standStorageMock.readMultiple(any(Iterable.class)))
                .thenReturn(Collections.<EntityRecord>emptyIterator());

        final Stand stand = prepareStandWithAggregateRepo(standStorageMock);

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readCustomersQuery, responseObserver);

        final List<Record> recordList = checkAndGetRecordList(responseObserver);
        assertTrue(recordList.isEmpty(),
                   "Query returned a non-empty response record list though the target had been " +
                           "empty");
    }

    private void doCheckReadingProjectsById(int numberOfProjects) {
        // Define the types and values used as a test data.
        final Map<ProjectId, Project> sampleProjects = newHashMap();
        final TypeUrl projectType = TypeUrl.of(Project.class);
        fillSampleProjects(sampleProjects, numberOfProjects);

        final StandTestProjectionRepository projectionRepository =
                mock(StandTestProjectionRepository.class);
        when(projectionRepository.getEntityStateType()).thenReturn(projectType);
        setupExpectedFindAllBehaviour(sampleProjects, projectionRepository);

        final Stand stand = prepareStandWithProjectionRepo(projectionRepository);

        final Query readMultipleProjects =
                requestFactory.query().byIds(Project.class, sampleProjects.keySet());

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readMultipleProjects, responseObserver);

        final List<Record> recordList = checkAndGetRecordList(responseObserver);
        assertEquals(sampleProjects.size(), recordList.size());
        final Collection<Project> allCustomers = sampleProjects.values();
        for (Record singleRecord : recordList) {
            final Project unpackedSingleResult = unpack(singleRecord.getState());
            assertTrue(allCustomers.contains(unpackedSingleResult));
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private void doCheckReadingCustomersByIdAndFieldMask(String... paths) {
        final Stand stand = prepareStandWithAggregateRepo(createStandStorage());

        final int querySize = 2;

        final Set<CustomerId> ids = new HashSet<>();
        for (int i = 0; i < querySize; i++) {
            final Customer customer = getSampleCustomer().toBuilder()
                                                         .setId(CustomerId.newBuilder()
                                                                          .setNumber(i))
                                                         .build();
            final Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(customer.getId(), customer, stateVersion));

            ids.add(customer.getId());
        }

        final Query customerQuery =
                requestFactory.query().byIdsWithMask(Customer.class, ids, paths);

        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(paths))
                                             .build();

        final MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                final List<Record> records = value.getRecordsList();
                Verify.assertSize(ids.size(), records);

                for (Record record : records) {
                    final Customer customer = unpack(record.getState());

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
        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final Map<CustomerId, Customer> sampleCustomers = fillSampleCustomers(numberOfCustomers);

        // Prepare the stand and its storage to act.
        final StandStorage standStorage = setupStandStorageWithCustomers(sampleCustomers,
                                                                         customerType);
        final Stand stand = prepareStandWithAggregateRepo(standStorage);

        triggerMultipleUpdates(sampleCustomers, stand);

        final Query readMultipleCustomers = requestFactory.query().byIds(Customer.class,
                                                                         sampleCustomers.keySet());

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readMultipleCustomers, responseObserver);

        final List<Record> recordList = checkAndGetRecordList(responseObserver);
        assertEquals(sampleCustomers.size(), recordList.size());
        final Collection<Customer> allCustomers = sampleCustomers.values();
        for (Record singleRecord : recordList) {
            final Customer unpackedSingleResult = unpack(singleRecord.getState());
            assertTrue(allCustomers.contains(unpackedSingleResult));
        }
        return stand;
    }

    private StandStorage setupStandStorageWithCustomers(Map<CustomerId, Customer> sampleCustomers,
                                                        TypeUrl customerType) {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(isMultitenant())
                                                .build();
        final StandStorage standStorage = bc.getStorageFactory()
                                            .createStandStorage();

        final ImmutableList.Builder<AggregateStateId> stateIdsBuilder = ImmutableList.builder();
        final ImmutableList.Builder<EntityRecord> recordsBuilder = ImmutableList.builder();
        for (CustomerId customerId : sampleCustomers.keySet()) {
            final AggregateStateId stateId = AggregateStateId.of(customerId, customerType);
            final Customer customer = sampleCustomers.get(customerId);
            final Any customerState = AnyPacker.pack(customer);
            final EntityRecord entityRecord = EntityRecord.newBuilder()
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

        final Set<ProjectId> projectIds = sampleProjects.keySet();
        final ImmutableCollection<StandTestProjection> allResults =
                toProjectionCollection(projectIds);
        final ImmutableCollection<EntityRecord> allRecords = toRecordCollection(projectIds);

        for (ProjectId projectId : projectIds) {
            when(projectionRepository.find(eq(projectId)))
                    .thenReturn(Optional.of(new StandTestProjection(projectId)));
        }

        final Iterable<ProjectId> matchingIds = argThat(projectionIdsIterableMatcher(projectIds));

        when(projectionRepository.loadAll(matchingIds, any(FieldMask.class)))
                .thenReturn(allResults.iterator());
        when(projectionRepository.loadAll())
                .thenReturn(allResults.iterator());
        when(projectionRepository.loadAllRecords())
                .thenReturn(allRecords.iterator());

        final ArgumentMatcher<EntityFilters> matcher = entityFilterMatcher(projectIds);
        final EntityFilters matchingFilter = argThat(matcher);
        when(projectionRepository.find(matchingFilter, any(FieldMask.class)))
                .thenReturn(allResults.iterator());
        // Re-initialize matcher for usage.
        argThat(matcher);
        when(projectionRepository.findRecords(matchingFilter, any(FieldMask.class)))
                .thenReturn(allRecords.iterator());
    }

    @SuppressWarnings("OverlyComplexAnonymousInnerClass")
    private static ArgumentMatcher<EntityFilters> entityFilterMatcher(
            final Collection<ProjectId> projectIds) {
        // This argument matcher does NOT mimic the exact repository behavior.
        // Instead, it only matches the EntityFilters instance in case it has EntityIdFilter with
        // ALL the expected IDs.
        return argument -> {
            boolean everyElementPresent = true;
            for (EntityId entityId : argument.getIdFilter()
                                             .getIdsList()) {
                final Any idAsAny = entityId.getId();
                final Message rawId = unpack(idAsAny);
                if (rawId instanceof ProjectId) {
                    final ProjectId convertedProjectId = (ProjectId) rawId;
                    everyElementPresent = everyElementPresent
                            && projectIds.contains(convertedProjectId);
                } else {
                    everyElementPresent = false;
                }
            }
            return everyElementPresent;
        };
    }

    private static ImmutableCollection<StandTestProjection>
    toProjectionCollection(Collection<ProjectId> values) {
        final Collection<StandTestProjection> transformed = Collections2.transform(
                values,
                input -> {
                    checkNotNull(input);
                    return new StandTestProjection(input);
                });
        final ImmutableList<StandTestProjection> result = ImmutableList.copyOf(transformed);
        return result;
    }

    private static ImmutableCollection<EntityRecord>
    toRecordCollection(Collection<ProjectId> values) {
        final Collection<EntityRecord> transformed = Collections2.transform(
                values,
                input -> {
                    checkNotNull(input);
                    final StandTestProjection projection = new StandTestProjection(input);
                    final Any id = AnyPacker.pack(projection.getId());
                    final Any state = AnyPacker.pack(projection.getState());
                    final EntityRecord record = EntityRecord.newBuilder()
                                                            .setEntityId(id)
                                                            .setState(state)
                                                            .build();
                    return record;
                });
        final ImmutableList<EntityRecord> result = ImmutableList.copyOf(transformed);
        return result;
    }

    private static ArgumentMatcher<Iterable<ProjectId>> projectionIdsIterableMatcher(
            final Set<ProjectId> projectIds) {
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
            final Customer sampleCustomer = sampleCustomers.get(id);
            final Version stateVersion = GivenVersion.withNumber(1);
            stand.update(asEnvelope(id, sampleCustomer, stateVersion));
        }
    }

    protected static Map<CustomerId, Customer> fillSampleCustomers(int numberOfCustomers) {
        final Map<CustomerId, Customer> sampleCustomers = newHashMap();

        @SuppressWarnings("UnsecureRandomNumberGeneration")
        final Random randomizer = new Random(Integer.MAX_VALUE);
            // force non-negative numeric ID values.

        for (int customerIndex = 0; customerIndex < numberOfCustomers; customerIndex++) {

            final int numericId = randomizer.nextInt();
            final CustomerId customerId = customerIdFor(numericId);
            final Customer customer =
                    Customer.newBuilder()
                            .setName(PersonName.newBuilder()
                                               .setGivenName(String.valueOf(numericId)))
                            .build();
            sampleCustomers.put(customerId, customer);
        }
        return sampleCustomers;
    }

    private static Map<ProjectId, Project> fillSampleProjects(int numberOfProjects) {
        final Map<ProjectId, Project> sampleProjects = newHashMap();

        @SuppressWarnings("UnsecureRandomNumberGeneration")
        final Random randomizer = new Random(Integer.MAX_VALUE);
        // Force non-negative numeric ID values.

        for (int projectIndex = 0; projectIndex < numberOfProjects; projectIndex++) {

            final int numericId = randomizer.nextInt();
            final ProjectId customerId = projectIdFor(numericId);

            final Project project = Project.newBuilder()
                                           .setName(String.valueOf(numericId))
                                           .build();
            sampleProjects.put(customerId, project);
        }
        return sampleProjects;
    }

    private static void fillSampleProjects(Map<ProjectId, Project> sampleProjects,
                                           int numberOfProjects) {
        for (int projectIndex = 0; projectIndex < numberOfProjects; projectIndex++) {
            final Project project = Project.getDefaultInstance();
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(UUID.randomUUID()
                                                            .toString())
                                                 .build();
            sampleProjects.put(projectId, project);
        }
    }

    private static List<Record> checkAndGetRecordList(MemoizeQueryResponseObserver responseObserver) {
        assertTrue(responseObserver.isCompleted(), "Query has not completed successfully");
        assertNull(responseObserver.throwable(), "Throwable has been caught upon query execution");

        final QueryResponse response = responseObserver.responseHandled();
        assertEquals(Responses.ok(), response.getResponse(), "Query response is not OK");
        assertNotNull(response, "Query response must not be null");

        final List<Record> recordsList = response.getRecordsList();
        assertNotNull(recordsList, "Query response has null message list");
        return recordsList;
    }

    protected Stand prepareStandWithAggregateRepo(StandStorage standStorage) {
        final BoundedContext boundedContext =
                BoundedContext.newBuilder()
                              .setMultitenant(multitenant)
                              .setStand(Stand.newBuilder()
                                             .setStorage(standStorage))
                              .build();

        final Stand stand = boundedContext.getStand();
        assertNotNull(stand);

        final CustomerAggregateRepository customerAggregateRepo =
                new CustomerAggregateRepository();
        stand.registerTypeSupplier(customerAggregateRepo);
        final StandTestProjectionRepository projectProjectionRepo =
                new StandTestProjectionRepository();
        stand.registerTypeSupplier(projectProjectionRepo);
        return stand;
    }

    private static Stand prepareStandWithProjectionRepo(ProjectionRepository projectionRepository) {
        final Stand stand = Stand.newBuilder()
                                 .build();
        assertNotNull(stand);
        stand.registerTypeSupplier(projectionRepository);
        return stand;
    }

    private static EntityRecord recordStateMatcher(final EntityRecord expectedRecord) {
        return argThat(argument -> {
            final boolean matchResult = Objects.equals(expectedRecord.getState(),
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

        final TypeUrl actualTypeUrl = availableTypes.iterator()
                                                    .next();
        final TypeUrl expectedTypeUrl = TypeUrl.from(expectedType);
        assertEquals(expectedTypeUrl, actualTypeUrl, "Type was registered incorrectly");
    }

    private static Stand.EntityUpdateCallback emptyUpdateCallback() {
        return newEntityState -> {
            //do nothing
        };
    }

    private static void assertMatches(Message message, FieldMask fieldMask) {
        final List<String> paths = fieldMask.getPathsList();
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
        final TenantId tenantId = isMultitenant() ? tenantId() : TenantId.getDefaultInstance();
        return EntityStateEnvelope.of(entityId, entityState, entityVersion, tenantId);
    }
}
