/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import io.spine.core.given.GivenVersion;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.people.PersonName;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.Given.CustomerAggregate;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityStateEnvelope;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.stand.Given.StandTestProjectionRepository;
import io.spine.server.tenant.TenantAwareTest;
import io.spine.test.Verify;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.time.ZoneOffsets;
import io.spine.type.TypeUrl;
import io.spine.validate.Validate;
import io.spine.validate.ValidationError;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
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
import static io.spine.Identifier.newUuid;
import static io.spine.client.QueryValidationError.INVALID_QUERY;
import static io.spine.client.QueryValidationError.UNSUPPORTED_QUERY_TARGET;
import static io.spine.client.TopicValidationError.INVALID_TOPIC;
import static io.spine.client.TopicValidationError.UNSUPPORTED_TOPIC_TARGET;
import static io.spine.core.given.GivenUserId.of;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.stand.Given.StandTestProjection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
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
@SuppressWarnings({"OverlyCoupledClass", "ClassWithTooManyMethods", "OverlyComplexClass"})
public class StandShould extends TenantAwareTest {
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

    @Before
    public void setUp() {
        setMultitenant(false);
        requestFactory = createRequestFactory(null);
    }

    protected static ActorRequestFactory createRequestFactory(@Nullable TenantId tenant) {
        final ActorRequestFactory.Builder builder = ActorRequestFactory.newBuilder()
                                                         .setActor(of(newUuid()))
                                                         .setZoneOffset(ZoneOffsets.UTC);
        if (tenant != null) {
            builder.setTenantId(tenant);
        }
        return builder.build();
    }

    @Test
    public void initialize_with_empty_builder() {
        final Stand.Builder builder = Stand.newBuilder()
                                           .setMultitenant(isMultitenant());
        final Stand stand = builder.build();

        assertNotNull(stand);
        assertTrue("Exposed types must be empty after the initialization.",
                   stand.getExposedTypes()
                        .isEmpty());
        assertTrue("Exposed aggregate types must be empty after the initialization",
                   stand.getExposedAggregateTypes()
                        .isEmpty());
    }

    @Test
    public void initialize_with_direct_UpdateDelivery_if_none_customized() {
        final Stand.Builder builder = Stand.newBuilder()
                                           .setMultitenant(isMultitenant());
        final Stand stand = builder.build();

        final StandUpdateDelivery actual = stand.delivery();
        assertTrue(actual instanceof StandUpdateDelivery.DirectDelivery);
    }

    @Test
    public void register_projection_repositories() {
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
        assertTrue("For some reason an aggregate type was registered",
                   knownAggregateTypes.isEmpty());

        final StandTestProjectionRepository anotherTestProjectionRepo =
                new StandTestProjectionRepository();
        stand.registerTypeSupplier(anotherTestProjectionRepo);
        checkHasExactlyOne(stand.getExposedTypes(), Project.getDescriptor());
    }

    @Test
    public void register_aggregate_repositories() {
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

    @Test
    public void use_provided_executor_upon_update_of_watched_type() {
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
    public void operate_with_storage_provided_through_builder() {
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
        stand.registerTypeSupplier(customerAggregateRepo);

        final int numericIdValue = 17;
        final CustomerId customerId = customerIdFor(numericIdValue);
        final CustomerAggregate customerAggregate = customerAggregateRepo.create(customerId);
        final Customer customerState = customerAggregate.getState();
        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final Version stateVersion = GivenVersion.withNumber(1);

        verify(standStorageMock, never()).write(any(AggregateStateId.class), any(EntityRecordWithColumns.class));

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

    @Test
    public void return_empty_list_for_aggregate_read_all_on_empty_stand_storage() {
        final Query readAllCustomers = requestFactory.query().all(Customer.class);
        checkEmptyResultForTargetOnEmptyStorage(readAllCustomers);
    }

    @Test
    public void return_empty_list_for_aggregate_read_by_ids_on_empty_stand_storage() {

        final Query readCustomersById = requestFactory.query().byIds(Customer.class, newHashSet(
                customerIdFor(1), customerIdFor(2)
        ));

        checkEmptyResultForTargetOnEmptyStorage(readCustomersById);
    }

    @Test
    public void return_empty_list_for_aggregate_reads_with_filters_not_set() {
        final StandStorage standStorageMock = mock(StandStorage.class);

        // Return non-empty results on any storage read call.
        final EntityRecord someRecord = EntityRecord.getDefaultInstance();
        final ImmutableList<EntityRecord> nonEmptyList =
                ImmutableList.<EntityRecord>builder().add(someRecord)
                                                     .build();
        when(standStorageMock.readAllByType(any(TypeUrl.class)))
                .thenReturn(nonEmptyList.iterator());
        when(standStorageMock.readAll())
                .thenReturn(Collections.<EntityRecord>emptyIterator());
        when(standStorageMock.readMultiple(ArgumentMatchers.<AggregateStateId>anyIterable()))
                .thenReturn(nonEmptyList.iterator());
        when(standStorageMock.readMultiple(ArgumentMatchers.<AggregateStateId>anyIterable()))
                .thenReturn(nonEmptyList.iterator());

        final Stand stand = prepareStandWithAggregateRepo(standStorageMock);

        final Query noneOfCustomersQuery = requestFactory.query()
                                          .byIds(Customer.class, Collections.<Message>emptySet());

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(noneOfCustomersQuery, responseObserver);

        verifyObserver(responseObserver);

        final List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertTrue("Query returned a non-empty response message list " +
                           "though the filter was not set", messageList.isEmpty());
    }

    @Test
    public void return_single_result_for_aggregate_state_read_by_id() {
        doCheckReadingCustomersById(1);
    }

    @Test
    public void return_multiple_results_for_aggregate_state_batch_read_by_ids() {
        doCheckReadingCustomersById(TOTAL_CUSTOMERS_FOR_BATCH_READING);
    }

    @Test
    public void return_single_result_for_projection_read_by_id() {
        doCheckReadingProjectsById(1);
    }

    @Test
    public void return_multiple_results_for_projection_batch_read_by_ids() {
        doCheckReadingProjectsById(TOTAL_PROJECTS_FOR_BATCH_READING);
    }

    @Test
    public void return_multiple_results_for_projection_batch_read_by_ids_with_field_mask() {
        final List<Descriptors.FieldDescriptor> projectFields = Project.getDescriptor()
                                                                       .getFields();
        doCheckReadingCustomersByIdAndFieldMask(
                projectFields.get(0)
                             .getFullName(), // ID
                projectFields.get(1)
                             .getFullName()); // Name
    }

    @Test
    public void trigger_subscription_callback_upon_update_of_aggregate() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Topic allCustomers = requestFactory.topic().allOf(Customer.class);

        final MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();

        subscribeAndActivate(stand, allCustomers,memoizeCallback);
        assertNull(memoizeCallback.newEntityState);

        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        final Any packedState = AnyPacker.pack(customer);
        assertEquals(packedState, memoizeCallback.newEntityState);
    }

    @Test
    public void trigger_subscription_callback_upon_update_of_projection() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Topic allProjects = requestFactory.topic().allOf(Project.class);

        final MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
        subscribeAndActivate(stand, allProjects, memoizeCallback);
        assertNull(memoizeCallback.newEntityState);

        final Map.Entry<ProjectId, Project> sampleData = fillSampleProjects(1).entrySet()
                                                                              .iterator()
                                                                              .next();
        final ProjectId projectId = sampleData.getKey();
        final Project project = sampleData.getValue();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(projectId, project, stateVersion));

        final Any packedState = AnyPacker.pack(project);
        assertEquals(packedState, memoizeCallback.newEntityState);
    }

    @Test
    public void allow_cancelling_subscriptions() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Topic allCustomers = requestFactory.topic().allOf(Customer.class);

        final MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
        final Subscription subscription = subscribeAndActivate(stand, allCustomers, memoizeCallback);

        stand.cancel(subscription, StreamObservers.<Response>noOpObserver());

        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        assertNull(memoizeCallback.newEntityState);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_if_cancelling_inexistent_subscription() {
        final Stand stand = Stand.newBuilder()
                                 .build();
        final Subscription inexistentSubscription = Subscription.newBuilder()
                                                                .setId(Subscriptions.generateId())
                                                                .build();
        stand.cancel(inexistentSubscription, StreamObservers.<Response>noOpObserver());
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void trigger_each_subscription_callback_once_for_multiple_subscriptions() {
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
            assertEquals(packedState, callback.newEntityState);
            verify(callback, times(1)).onStateChanged(any(EntityStateUpdate.class));
        }
    }

    @Test
    public void do_not_trigger_subscription_callbacks_in_case_of_another_type_criterion_mismatch() {
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

    @Test
    public void trigger_subscription_callbacks_matching_by_id() {
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

    private MemoizeEntityUpdateCallback subscribeWithCallback(Stand stand,
                                                              Target subscriptionTarget) {
        final MemoizeEntityUpdateCallback callback = spy(new MemoizeEntityUpdateCallback());
        final Topic topic = requestFactory.topic().forTarget(subscriptionTarget);
        subscribeAndActivate(stand, topic, callback);

        assertNull(callback.newEntityState);
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
    public void retrieve_all_data_if_field_mask_is_not_set() {
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
                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer customer = unpack(messages.get(0));
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
    public void retrieve_only_selected_param_for_query() {
        requestSampleCustomer(new int[]{Customer.NAME_FIELD_NUMBER - 1}, new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);

                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer sampleCustomer = getSampleCustomer();
                final Customer customer = unpack(messages.get(0));
                assertTrue(customer.getName()
                                   .equals(sampleCustomer.getName()));
                assertFalse(customer.hasId());
                assertTrue(customer.getNicknamesList()
                                   .isEmpty());
            }
        });
    }

    @Test
    public void retrieve_collection_fields_if_required() {
        requestSampleCustomer(
                new int[]{Customer.NICKNAMES_FIELD_NUMBER - 1},
                new MemoizeQueryResponseObserver() {
                    @Override
                    public void onNext(QueryResponse value) {
                        super.onNext(value);

                        final List<Any> messages = value.getMessagesList();
                        assertFalse(messages.isEmpty());

                        final Customer sampleCustomer = getSampleCustomer();
                        final Customer customer = unpack(messages.get(0));
                        assertEquals(customer.getNicknamesList(),
                                     sampleCustomer.getNicknamesList());

                        assertFalse(customer.hasName());
                        assertFalse(customer.hasId());
                    }
                }
        );
    }

    @Test
    public void retrieve_all_requested_fields() {
        requestSampleCustomer(
                new int[]{ Customer.NICKNAMES_FIELD_NUMBER - 1,
                        Customer.ID_FIELD_NUMBER - 1 },
                new MemoizeQueryResponseObserver() {
                    @Override
                    public void onNext(QueryResponse value) {
                        super.onNext(value);

                        final List<Any> messages = value.getMessagesList();
                        assertFalse(messages.isEmpty());

                        final Customer sampleCustomer = getSampleCustomer();
                        final Customer customer = unpack(messages.get(0));
                        assertEquals(customer.getNicknamesList(),
                                     sampleCustomer.getNicknamesList());

                        assertFalse(customer.hasName());
                        assertTrue(customer.hasId());
                    }
                }
        );
    }

    @Test
    public void retrieve_whole_entity_if_nothing_is_requested() {
        //noinspection ZeroLengthArrayAllocation
        requestSampleCustomer(new int[]{}, getDuplicateCostumerStreamObserver());
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void select_entity_singleton_by_id_and_apply_field_masks() {
        final Stand stand = prepareStandWithAggregateRepo(createStandStorage());
        final String customerDescriptor = Customer.getDescriptor()
                                                  .getFullName();
        @SuppressWarnings("DuplicateStringLiteralInspection")   // clashes with non-related tests.
        final String[] paths = {customerDescriptor + ".id", customerDescriptor + ".name"};
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(paths))
                                             .build();

        final List<Customer> customers = new LinkedList<>();
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

        final List<Any> read = observer.responseHandled.getMessagesList();
        Verify.assertSize(1, read);

        final Customer customer = unpack(read.get(0));
        assertMatches(customer, fieldMask);
        assertTrue(ids.contains(customer.getId()));

        verifyObserver(observer);
    }

    @Test
    public void handle_mistakes_in_query_silently() {
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
                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer customer = unpack(messages.get(0));

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

    @Test
    public void throw_invalid_query_exception_packed_as_IAE_if_querying_unknown_type() {

        final Stand stand = Stand.newBuilder()
                                 .build();

        checkTypesEmpty(stand);

        // Customer type was NOT registered.
        // So create a query for an unknown type.
        final Query readAllCustomers = requestFactory.query()
                                                     .all(Customer.class);

        try {
            stand.execute(readAllCustomers, StreamObservers.<QueryResponse>noOpObserver());
            fail("Expected IllegalArgumentException upon executing query with unknown target," +
                         " but got nothing");
        } catch (IllegalArgumentException e) {
            verifyUnsupportedQueryTargetException(readAllCustomers, e);
        }
    }

    @Test
    public void throw_invalid_query_exception_packed_as_IAE_if_invalid_query_message_passed() {

        final Stand stand = Stand.newBuilder()
                                 .build();
        final Query invalidQuery = Query.getDefaultInstance();

        try {
            stand.execute(invalidQuery, StreamObservers.<QueryResponse>noOpObserver());
            fail("Expected IllegalArgumentException due to invalid query message passed," +
                         " but got nothing");
        } catch (IllegalArgumentException e) {
            verifyInvalidQueryException(invalidQuery, e);
        }
    }

    @Test
    public void throw_invalid_topic_ex_packed_as_IAE_if_subscribing_to_unknown_type_changes() {

        final Stand stand = Stand.newBuilder()
                                 .build();
        checkTypesEmpty(stand);

        // Customer type was NOT registered.
        // So create a topic for an unknown type.
        final Topic allCustomersTopic = requestFactory.topic()
                                                      .allOf(Customer.class);

        try {
            stand.subscribe(allCustomersTopic, StreamObservers.<Subscription>noOpObserver());
            fail("Expected IllegalArgumentException upon subscribing to a topic " +
                         "with unknown target, but got nothing");
        } catch (IllegalArgumentException e) {
            verifyUnsupportedTopicException(allCustomersTopic, e);
        }
    }

    @Test
    public void throw_invalid_topic_exception_packed_as_IAE_if_invalid_topic_message_passed() {

        final Stand stand = Stand.newBuilder()
                                 .build();

        final Topic invalidTopic = Topic.getDefaultInstance();

        try {
            stand.subscribe(invalidTopic, StreamObservers.<Subscription>noOpObserver());
            fail("Expected IllegalArgumentException due to an invalid topic message, " +
                         "but got nothing");
        } catch (IllegalArgumentException e) {
            verifyInvalidTopicException(invalidTopic, e);
        }
    }

    @Test
    public void throw_invalid_subscription_ex_if_activating_subscription_with_unsupported_target() {

        final Stand stand = Stand.newBuilder()
                                 .build();
        final Subscription subscription = subscriptionWithUnknownTopic();

        try {
            stand.activate(subscription,
                           emptyUpdateCallback(),
                           StreamObservers.<Response>noOpObserver());
            fail("Expected IllegalArgumentException upon activating an unknown subscription, " +
                         "but got nothing");
        } catch (IllegalArgumentException e) {
            verifyUnknownSubscriptionException(e, subscription);
        }
    }

    @Test
    public void throw_invalid_subscription_ex_if_cancelling_subscription_with_unsupported_target() {

        final Stand stand = Stand.newBuilder()
                                 .build();
        final Subscription subscription = subscriptionWithUnknownTopic();

        try {
            stand.cancel(subscription, StreamObservers.<Response>noOpObserver());
            fail("Expected IllegalArgumentException upon cancelling an unknown subscription, " +
                         "but got nothing");
        } catch (IllegalArgumentException e) {
            verifyUnknownSubscriptionException(e, subscription);
        }
    }

    @Test
    public void throw_invalid_subscription_ex_if_invalid_subscription_msg_passed_to_activate() {

        final Stand stand = Stand.newBuilder()
                                 .build();
        final Subscription invalidSubscription = Subscription.getDefaultInstance();

        try {
            stand.activate(invalidSubscription,
                           emptyUpdateCallback(),
                           StreamObservers.<Response>noOpObserver());
            fail("Expected IllegalArgumentException due to an invalid subscription message " +
                         "passed to `activate`, but got nothing");
        } catch (IllegalArgumentException e) {
            verifyInvalidSubscriptionException(invalidSubscription, e);
        }
    }

    @Test
    public void throw_invalid_subscription_ex_if_invalid_subscription_msg_passed_to_cancel() {

        final Stand stand = Stand.newBuilder()
                                 .build();
        final Subscription invalidSubscription = Subscription.getDefaultInstance();

        try {
            stand.cancel(invalidSubscription, StreamObservers.<Response>noOpObserver());
            fail("Expected IllegalArgumentException due to an invalid subscription message " +
                         "passed to `cancel`, but got nothing");
        } catch (IllegalArgumentException e) {
            verifyInvalidSubscriptionException(invalidSubscription, e);
        }
    }

    private static void verifyUnsupportedQueryTargetException(Query query,
                                                              IllegalArgumentException e) {
        final Throwable cause = e.getCause();
        assertTrue(cause instanceof InvalidQueryException);

        final InvalidQueryException queryException = (InvalidQueryException) cause;
        assertEquals(query, queryException.getRequest());

        assertEquals(UNSUPPORTED_QUERY_TARGET.getNumber(),
                     queryException.asError()
                                   .getCode());
    }

    private static void verifyInvalidQueryException(Query invalidQuery,
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

    private static void verifyUnsupportedTopicException(Topic topic, IllegalArgumentException e) {
        final Throwable cause = e.getCause();
        assertTrue(cause instanceof InvalidTopicException);

        final InvalidTopicException topicException = (InvalidTopicException) cause;
        assertEquals(topic, topicException.getRequest());

        assertEquals(UNSUPPORTED_TOPIC_TARGET.getNumber(),
                     topicException.asError()
                                   .getCode());
    }

    private static void verifyInvalidTopicException(Topic invalidTopic,
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

    private static void verifyInvalidSubscriptionException(Subscription invalidSubscription,
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

    private static void verifyUnknownSubscriptionException(IllegalArgumentException e,
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

    @CanIgnoreReturnValue
    protected static Subscription subscribeAndActivate(Stand stand,
                                                     Topic topic,
                                                     Stand.EntityUpdateCallback callback) {
        final MemoizingObserver<Subscription> observer = memoizingObserver();
        stand.subscribe(topic, observer);
        final Subscription subscription = observer.firstResponse();
        stand.activate(subscription, callback, StreamObservers.<Response>noOpObserver());

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
        assertNotNull(observer.responseHandled);
        assertTrue(observer.isCompleted);
        assertNull(observer.throwable);
    }

    private static MemoizeQueryResponseObserver getDuplicateCostumerStreamObserver() {
        return new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);

                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer customer = unpack(messages.get(0));
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
                .thenReturn(Collections.<EntityRecord>emptyIterator());
        when(standStorageMock.readMultiple(any(Iterable.class)))
                .thenReturn(Collections.<EntityRecord>emptyIterator());

        final Stand stand = prepareStandWithAggregateRepo(standStorageMock);

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readCustomersQuery, responseObserver);

        final List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertTrue("Query returned a non-empty response message list though the target was empty",
                   messageList.isEmpty());
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

        final List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertEquals(sampleProjects.size(), messageList.size());
        final Collection<Project> allCustomers = sampleProjects.values();
        for (Any singleRecord : messageList) {
            final Project unpackedSingleResult = unpack(singleRecord);
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

        final Query customerQuery = requestFactory.query().byIdsWithMask(Customer.class, ids, paths);

        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(paths))
                                             .build();

        final MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                final List<Any> messages = value.getMessagesList();
                Verify.assertSize(ids.size(), messages);

                for (Any message : messages) {
                    final Customer customer = unpack(message);

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

        final List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertEquals(sampleCustomers.size(), messageList.size());
        final Collection<Customer> allCustomers = sampleCustomers.values();
        for (Any singleRecord : messageList) {
            final Customer unpackedSingleResult = unpack(singleRecord);
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
            Map<ProjectId,
            Project> sampleProjects,
            StandTestProjectionRepository projectionRepository) {

        final Set<ProjectId> projectIds = sampleProjects.keySet();
        final ImmutableCollection<Given.StandTestProjection> allResults =
                toProjectionCollection(projectIds);

        for (ProjectId projectId : projectIds) {
            when(projectionRepository.find(eq(projectId)))
                    .thenReturn(Optional.of(new StandTestProjection(projectId)));
        }

        final Iterable<ProjectId> matchingIds = argThat(projectionIdsIterableMatcher(projectIds));

        when(projectionRepository.loadAll(matchingIds, any(FieldMask.class)))
                .thenReturn(allResults.iterator());
        when(projectionRepository.loadAll())
                .thenReturn(allResults.iterator());

        final EntityFilters matchingFilter = argThat(entityFilterMatcher(projectIds));
        when(projectionRepository.find(matchingFilter, any(FieldMask.class)))
                .thenReturn(allResults.iterator());
    }

    @SuppressWarnings("OverlyComplexAnonymousInnerClass")
    private static ArgumentMatcher<EntityFilters> entityFilterMatcher(
            final Collection<ProjectId> projectIds) {
        // This argument matcher does NOT mimic the exact repository behavior.
        // Instead, it only matches the EntityFilters instance in case it has EntityIdFilter with
        // ALL the expected IDs.
        return new ArgumentMatcher<EntityFilters>() {
            @Override
            public boolean matches(EntityFilters argument) {
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
            }
        };
    }

    private static ImmutableCollection<Given.StandTestProjection> toProjectionCollection(
            Collection<ProjectId> values) {
        final Collection<Given.StandTestProjection> transformed = Collections2.transform(
                values,
                new Function<ProjectId, Given.StandTestProjection>() {
                    @Override
                    public StandTestProjection apply(@Nullable ProjectId input) {
                        checkNotNull(input);
                        return new StandTestProjection(input);
                    }
                });
        final ImmutableList<Given.StandTestProjection> result = ImmutableList.copyOf(transformed);
        return result;
    }

    private static ArgumentMatcher<Iterable<ProjectId>> projectionIdsIterableMatcher(
            final Set<ProjectId> projectIds) {
        return new ArgumentMatcher<Iterable<ProjectId>>() {
            @Override
            public boolean matches(Iterable<ProjectId> argument) {
                boolean everyElementPresent = true;
                for (ProjectId projectId : argument) {
                    everyElementPresent = everyElementPresent && projectIds.contains(projectId);
                }
                return everyElementPresent;
            }
        };
    }

    private void triggerMultipleUpdates(Map<CustomerId,
                                               Customer> sampleCustomers,
                                               Stand stand) {
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
            // force non-negative numeric ID values.

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

    private static List<Any> checkAndGetMessageList(MemoizeQueryResponseObserver responseObserver) {
        assertTrue("Query has not completed successfully", responseObserver.isCompleted);
        assertNull("Throwable has been caught upon query execution", responseObserver.throwable);

        final QueryResponse response = responseObserver.responseHandled;
        assertEquals("Query response is not OK", Responses.ok(), response.getResponse());
        assertNotNull("Query response must not be null", response);

        final List<Any> messageList = response.getMessagesList();
        assertNotNull("Query response has null message list", messageList);
        return messageList;
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
        return argThat(new ArgumentMatcher<EntityRecord>() {
            @Override
            public boolean matches(EntityRecord argument) {
                final boolean matchResult = Objects.equals(expectedRecord.getState(),
                                                           argument.getState());
                return matchResult;
            }
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
        assertEquals("Type was registered incorrectly", expectedTypeUrl, actualTypeUrl);
    }

    private static Stand.EntityUpdateCallback emptyUpdateCallback() {
        return new Stand.EntityUpdateCallback() {
            @Override
            public void onStateChanged(EntityStateUpdate newEntityState) {
                //do nothing
            }
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

    // ***** Inner classes used for tests. *****

    /**
     * A {@link StreamObserver} storing the state of {@link Query} execution.
     */
    protected static class MemoizeQueryResponseObserver implements StreamObserver<QueryResponse> {

        private QueryResponse responseHandled;
        private Throwable throwable;
        private boolean isCompleted = false;

        @Override
        public void onNext(QueryResponse response) {
            this.responseHandled = response;
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.isCompleted = true;
        }

        public QueryResponse getResponseHandled() {
            return responseHandled;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }

    protected static class MemoizeEntityUpdateCallback implements Stand.EntityUpdateCallback {

        private Any newEntityState = null;

        @Override
        public void onStateChanged(EntityStateUpdate newEntityState) {
            this.newEntityState = newEntityState.getState();
        }

        @Nullable
        public Any getNewEntityState() {
            return newEntityState;
        }
    }
}
