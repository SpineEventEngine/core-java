/*
 *
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
 *
 */
package org.spine3.server.stand;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.spine3.base.Queries;
import org.spine3.base.Responses;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.Query;
import org.spine3.client.QueryResponse;
import org.spine3.client.Subscription;
import org.spine3.client.Target;
import org.spine3.people.PersonName;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.BoundedContext;
import org.spine3.server.Given.CustomerAggregate;
import org.spine3.server.Given.CustomerAggregateRepository;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.stand.Given.StandTestProjectionRepository;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.StandStorage;
import org.spine3.server.storage.memory.InMemoryStandStorage;
import org.spine3.test.commandservice.customer.Customer;
import org.spine3.test.commandservice.customer.CustomerId;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;

import javax.annotation.Nullable;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.spine3.server.stand.Given.StandTestProjection;
import static org.spine3.test.Verify.assertSize;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

/**
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
//It's OK for a test.
@SuppressWarnings({"OverlyCoupledClass", "InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public class StandShould {
    private static final int TOTAL_CUSTOMERS_FOR_BATCH_READING = 10;
    private static final int TOTAL_PROJECTS_FOR_BATCH_READING = 10;


    @Test
    public void initialize_with_empty_builder() {
        final Stand.Builder builder = Stand.newBuilder();
        final Stand stand = builder.build();

        assertNotNull(stand);
        assertTrue("Available types must be empty after the initialization.", stand.getAvailableTypes()
                                                                                   .isEmpty());
        assertTrue("Known aggregate types must be empty after the initialization", stand.getKnownAggregateTypes()
                                                                                        .isEmpty());

    }

    @Test
    public void register_projection_repositories() {
        final Stand stand = Stand.newBuilder()
                                 .build();
        final BoundedContext boundedContext = newBoundedContext(stand);

        checkTypesEmpty(stand);

        final StandTestProjectionRepository standTestProjectionRepo = new StandTestProjectionRepository(boundedContext);
        stand.registerTypeSupplier(standTestProjectionRepo);
        checkHasExactlyOne(stand.getAvailableTypes(), Project.getDescriptor());

        final ImmutableSet<TypeUrl> knownAggregateTypes = stand.getKnownAggregateTypes();
        // As we registered a projection repo, known aggregate types should be still empty.
        assertTrue("For some reason an aggregate type was registered", knownAggregateTypes.isEmpty());

        final StandTestProjectionRepository anotherTestProjectionRepo = new StandTestProjectionRepository(boundedContext);
        stand.registerTypeSupplier(anotherTestProjectionRepo);
        checkHasExactlyOne(stand.getAvailableTypes(), Project.getDescriptor());
    }

    @Test
    public void register_aggregate_repositories() {
        final Stand stand = Stand.newBuilder()
                                 .build();
        final BoundedContext boundedContext = newBoundedContext(stand);

        checkTypesEmpty(stand);

        final CustomerAggregateRepository customerAggregateRepo = new CustomerAggregateRepository(boundedContext);
        stand.registerTypeSupplier(customerAggregateRepo);

        final Descriptors.Descriptor customerEntityDescriptor = Customer.getDescriptor();
        checkHasExactlyOne(stand.getAvailableTypes(), customerEntityDescriptor);
        checkHasExactlyOne(stand.getKnownAggregateTypes(), customerEntityDescriptor);

        @SuppressWarnings("LocalVariableNamingConvention")
        final CustomerAggregateRepository anotherCustomerAggregateRepo = new CustomerAggregateRepository(boundedContext);
        stand.registerTypeSupplier(anotherCustomerAggregateRepo);
        checkHasExactlyOne(stand.getAvailableTypes(), customerEntityDescriptor);
        checkHasExactlyOne(stand.getKnownAggregateTypes(), customerEntityDescriptor);
    }

    @Test
    public void use_provided_executor_upon_update_of_watched_type() {
        final Executor executor = mock(Executor.class);
        final Stand stand = Stand.newBuilder()
                                 .setCallbackExecutor(executor)
                                 .build();
        final BoundedContext boundedContext = newBoundedContext(stand);
        final StandTestProjectionRepository standTestProjectionRepo = new StandTestProjectionRepository(boundedContext);
        stand.registerTypeSupplier(standTestProjectionRepo);

        final Target projectProjectionTarget = Queries.Targets.allOf(Project.class);
        final Subscription subscription = stand.subscribe(projectProjectionTarget);
        stand.activate(subscription, emptyUpdateCallback());
        assertNotNull(subscription);

        verify(executor, never()).execute(any(Runnable.class));

        final Any someUpdate = AnyPacker.pack(Project.getDefaultInstance());
        final Object someId = new Object();
        stand.update(someId, someUpdate);

        verify(executor, times(1)).execute(any(Runnable.class));
    }

    @Test
    public void operate_with_storage_provided_through_builder() {
        final StandStorage standStorageMock = mock(StandStorage.class);
        final Stand stand = Stand.newBuilder()
                                 .setStorage(standStorageMock)
                                 .build();
        assertNotNull(stand);

        final BoundedContext boundedContext = newBoundedContext(stand);
        final CustomerAggregateRepository customerAggregateRepo = new CustomerAggregateRepository(boundedContext);
        stand.registerTypeSupplier(customerAggregateRepo);


        final int numericIdValue = 17;
        final CustomerId customerId = customerIdFor(numericIdValue);
        final CustomerAggregate customerAggregate = customerAggregateRepo.create(customerId);
        final Customer customerState = customerAggregate.getState();
        final Any packedState = AnyPacker.pack(customerState);
        final TypeUrl customerType = TypeUrl.of(Customer.class);

        verify(standStorageMock, never()).write(any(AggregateStateId.class), any(EntityStorageRecord.class));

        stand.update(customerId, packedState);

        final AggregateStateId expectedAggregateStateId = AggregateStateId.of(customerId, customerType);
        final EntityStorageRecord expectedRecord = EntityStorageRecord.newBuilder()
                                                                      .setState(packedState)
                                                                      .build();
        verify(standStorageMock, times(1)).write(eq(expectedAggregateStateId), recordStateMatcher(expectedRecord));
    }

    @Test
    public void return_empty_list_for_aggregate_read_all_on_empty_stand_storage() {
        final Query readAllCustomers = Queries.readAll(Customer.class);
        checkEmptyResultForTargetOnEmptyStorage(readAllCustomers);
    }

    @Test
    public void return_empty_list_to_unknown_type_reading() {

        final Stand stand = Stand.newBuilder()
                                 .build();

        checkTypesEmpty(stand);

        // Customer type was NOT registered.
        // So create a query for an unknown type.
        final Query readAllCustomers = Queries.readAll(Customer.class);

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readAllCustomers, responseObserver);

        verifyObserver(responseObserver);

        final List<Any> messageList = checkAndGetMessageList(responseObserver);

        assertTrue("Query returned a non-empty response message list for an unknown type", messageList.isEmpty());
    }

    @Test
    public void return_empty_list_for_aggregate_read_by_ids_on_empty_stand_storage() {


        final Query readCustomersById = Queries.readByIds(Customer.class, newHashSet(
                customerIdFor(1), customerIdFor(2)
        ));

        checkEmptyResultForTargetOnEmptyStorage(readCustomersById);
    }

    @Test
    public void return_empty_list_for_aggregate_reads_with_filters_not_set() {

        final Target noneOfCustomers = Queries.Targets.someOf(Customer.class, Collections.<Message>emptySet());
        checkEmptyResultOnNonEmptyStorageForQueryTarget(noneOfCustomers);
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
        final List<Descriptors.FieldDescriptor> projectFields = Project.getDescriptor().getFields();
        doCheckReadingCustomersByIdAndFieldMask(
                projectFields.get(0).getFullName(), // ID
                projectFields.get(1).getFullName()); // Name
    }

    @Test
    public void trigger_subscription_callback_upon_update_of_aggregate() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Target allCustomers = Queries.Targets.allOf(Customer.class);

        final MemoizeStandUpdateCallback memoizeCallback = new MemoizeStandUpdateCallback();
        final Subscription subscription = stand.subscribe(allCustomers);
        stand.activate(subscription, memoizeCallback);
        assertNotNull(subscription);
        assertNull(memoizeCallback.newEntityState);

        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Any packedState = AnyPacker.pack(customer);
        stand.update(customerId, packedState);

        assertEquals(packedState, memoizeCallback.newEntityState);
    }


    @Test
    public void trigger_subscription_callback_upon_update_of_projection() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Target allProjects = Queries.Targets.allOf(Project.class);

        final MemoizeStandUpdateCallback memoizeCallback = new MemoizeStandUpdateCallback();
        final Subscription subscription = stand.subscribe(allProjects);
        stand.activate(subscription, memoizeCallback);
        assertNotNull(subscription);
        assertNull(memoizeCallback.newEntityState);

        final Map.Entry<ProjectId, Project> sampleData = fillSampleProjects(1).entrySet()
                                                                              .iterator()
                                                                              .next();
        final ProjectId projectId = sampleData.getKey();
        final Project project = sampleData.getValue();
        final Any packedState = AnyPacker.pack(project);
        stand.update(projectId, packedState);

        assertEquals(packedState, memoizeCallback.newEntityState);
    }


    @Test
    public void allow_cancelling_subscriptions() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Target allCustomers = Queries.Targets.allOf(Customer.class);

        final MemoizeStandUpdateCallback memoizeCallback = new MemoizeStandUpdateCallback();
        final Subscription subscription = stand.subscribe(allCustomers);
        stand.activate(subscription, memoizeCallback);
        assertNull(memoizeCallback.newEntityState);

        stand.cancel(subscription);

        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Any packedState = AnyPacker.pack(customer);
        stand.update(customerId, packedState);

        assertNull(memoizeCallback.newEntityState);
    }

    @Test
    public void do_not_fail_if_cancelling_inexistent_subscription() {
        final Stand stand = Stand.newBuilder()
                                 .build();
        final Subscription inexistentSubscription = Subscription.newBuilder()
                                                                .setId(UUID.randomUUID()
                                                                           .toString())
                                                                .build();
        stand.cancel(inexistentSubscription);
    }


    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void trigger_each_subscription_callback_once_for_multiple_subscriptions() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Target allCustomers = Queries.Targets.allOf(Customer.class);

        final Set<MemoizeStandUpdateCallback> callbacks = newHashSet();
        final int totalCallbacks = 100;

        for (int callbackIndex = 0; callbackIndex < totalCallbacks; callbackIndex++) {
            final MemoizeStandUpdateCallback callback = subscribeWithCallback(stand, allCustomers);
            callbacks.add(callback);
        }


        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Any packedState = AnyPacker.pack(customer);
        stand.update(customerId, packedState);


        for (MemoizeStandUpdateCallback callback : callbacks) {
            assertEquals(packedState, callback.newEntityState);
            verify(callback, times(1)).onEntityStateUpdate(any(Any.class));
        }
    }

    @Test
    public void do_not_trigger_subscription_callbacks_in_case_of_another_type_criterion_mismatch() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final Target allProjects = Queries.Targets.allOf(Project.class);
        final MemoizeStandUpdateCallback callback = subscribeWithCallback(stand, allProjects);

        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Any packedState = AnyPacker.pack(customer);
        stand.update(customerId, packedState);

        verify(callback, never()).onEntityStateUpdate(any(Any.class));
    }

    @Test
    public void trigger_subscription_callbacks_matching_by_id() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));

        final Map<CustomerId, Customer> sampleCustomers = fillSampleCustomers(10);

        final Target someCustomers = Queries.Targets.someOf(Customer.class, sampleCustomers.keySet());
        final Set<Customer> callbackStates = newHashSet();
        final MemoizeStandUpdateCallback callback = new MemoizeStandUpdateCallback() {
            @Override
            public void onEntityStateUpdate(Any newEntityState) {
                super.onEntityStateUpdate(newEntityState);
                final Customer customerInCallback = AnyPacker.unpack(newEntityState);
                callbackStates.add(customerInCallback);
            }
        };
        final Subscription subscription = stand.subscribe(someCustomers);
        stand.activate(subscription, callback);

        for (Map.Entry<CustomerId, Customer> sampleEntry : sampleCustomers.entrySet()) {
            final CustomerId customerId = sampleEntry.getKey();
            final Customer customer = sampleEntry.getValue();
            final Any packedState = AnyPacker.pack(customer);
            stand.update(customerId, packedState);
        }

        assertEquals(newHashSet(sampleCustomers.values()), callbackStates);
    }

    private static MemoizeStandUpdateCallback subscribeWithCallback(Stand stand, Target subscriptionTarget) {
        final MemoizeStandUpdateCallback callback = spy(new MemoizeStandUpdateCallback());
        final Subscription subscription = stand.subscribe(subscriptionTarget);
        stand.activate(subscription, callback);
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
        final Stand stand = prepareStandWithAggregateRepo(InMemoryStandStorage.newBuilder().build());

        final Customer sampleCustomer = getSampleCustomer();

        stand.update(sampleCustomer.getId(), AnyPacker.pack(sampleCustomer));

        final Query customerQuery = Queries.readAll(Customer.class);

        //noinspection OverlyComplexAnonymousInnerClass
        final MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer customer = AnyPacker.unpack(messages.get(0));
                for (Descriptors.FieldDescriptor field : customer.getDescriptorForType().getFields()) {
                    assertTrue(customer.getField(field).equals(sampleCustomer.getField(field)));
                }
            }
        };

        stand.execute(customerQuery, observer);

        verifyObserver(observer);
    }

    @Test
    public void retrieve_only_selected_param_for_query() {
        requestSampleCustomer(new int[] {Customer.NAME_FIELD_NUMBER - 1}, new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);

                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer sampleCustomer = getSampleCustomer();
                final Customer customer = AnyPacker.unpack(messages.get(0));
                assertTrue(customer.getName()
                                   .equals(sampleCustomer.getName()));
                assertFalse(customer.hasId());
                assertTrue(customer.getNicknamesList().isEmpty());
            }
        });
    }

    @Test
    public void retrieve_collection_fields_if_required() {
        requestSampleCustomer(new int[] {Customer.NICKNAMES_FIELD_NUMBER - 1}, new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);

                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer sampleCustomer = getSampleCustomer();
                final Customer customer = AnyPacker.unpack(messages.get(0));
                assertEquals(customer.getNicknamesList(), sampleCustomer.getNicknamesList());

                assertFalse(customer.hasName());
                assertFalse(customer.hasId());
            }
        });
    }

    @Test
    public void retrieve_all_requested_fields() {
        requestSampleCustomer(new int[] {Customer.NICKNAMES_FIELD_NUMBER - 1, Customer.ID_FIELD_NUMBER - 1}, new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);

                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer sampleCustomer = getSampleCustomer();
                final Customer customer = AnyPacker.unpack(messages.get(0));
                assertEquals(customer.getNicknamesList(), sampleCustomer.getNicknamesList());

                assertFalse(customer.hasName());
                assertTrue(customer.hasId());
            }
        });
    }

    @Test
    public void retrieve_whole_entity_if_nothing_is_requested() {
        //noinspection ZeroLengthArrayAllocation
        requestSampleCustomer(new int[] {}, getDuplicateCostumerStreamObserver());
    }

    @Test
    public void handle_mistakes_in_query_silently() {
        //noinspection ZeroLengthArrayAllocation
        final Stand stand = prepareStandWithAggregateRepo(InMemoryStandStorage.newBuilder().build());

        final Customer sampleCustomer = getSampleCustomer();

        stand.update(sampleCustomer.getId(), AnyPacker.pack(sampleCustomer));

        // FieldMask with invalid type URLs.
        final String[] paths = {"invalid_type_url_example", Project.getDescriptor().getFields().get(2).getFullName()};

        final Query customerQuery = Queries.readAll(Customer.class, paths);

        final MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer customer = AnyPacker.unpack(messages.get(0));

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

                final Customer customer = AnyPacker.unpack(messages.get(0));
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
                       .setId(CustomerId.newBuilder().setNumber((int) UUID.randomUUID()
                                                                          .getLeastSignificantBits()))
                       .setName(PersonName.newBuilder().setGivenName("Socrates").build())
                       .addNicknames(PersonName.newBuilder().setGivenName("Philosopher"))
                       .addNicknames(PersonName.newBuilder().setGivenName("Wise guy"))
                       .build();

    }

    private static void requestSampleCustomer(int[] fieldIndexes, final MemoizeQueryResponseObserver observer) {
        final Stand stand = prepareStandWithAggregateRepo(InMemoryStandStorage.newBuilder().build());

        final Customer sampleCustomer = getSampleCustomer();

        stand.update(sampleCustomer.getId(), AnyPacker.pack(sampleCustomer));

        final String[] paths = new String[fieldIndexes.length];

        for (int i = 0; i < fieldIndexes.length; i++) {
            paths[i] = Customer.getDescriptor()
                                   .getFields()
                                   .get(fieldIndexes[i])
                                   .getFullName();
        }

        final Query customerQuery = Queries.readAll(Customer.class, paths);

        stand.execute(customerQuery, observer);

        verifyObserver(observer);
    }

    private static void checkEmptyResultForTargetOnEmptyStorage(Query readCustomersQuery) {
        final StandStorage standStorageMock = mock(StandStorage.class);
        // Return an empty collection on {@link StandStorage#readAllByType(TypeUrl)} call.
        final ImmutableList<EntityStorageRecord> emptyResultList = ImmutableList.<EntityStorageRecord>builder().build();
        when(standStorageMock.readAllByType(any(TypeUrl.class))).thenReturn(emptyResultList);

        final Stand stand = prepareStandWithAggregateRepo(standStorageMock);

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readCustomersQuery, responseObserver);

        final List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertTrue("Query returned a non-empty response message list though the target was empty", messageList.isEmpty());
    }

    private static void doCheckReadingProjectsById(int numberOfProjects) {
        // Define the types and values used as a test data.
        final Map<ProjectId, Project> sampleProjects = newHashMap();
        final TypeUrl projectType = TypeUrl.of(Project.class);
        fillSampleProjects(sampleProjects, numberOfProjects);

        final StandTestProjectionRepository projectionRepository = mock(StandTestProjectionRepository.class);
        when(projectionRepository.getEntityStateType()).thenReturn(projectType);
        setupExpectedFindAllBehaviour(sampleProjects, projectionRepository);

        final Stand stand = prepareStandWithProjectionRepo(projectionRepository);

        final Query readMultipleProjects = Queries.readByIds(Project.class, sampleProjects.keySet());

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readMultipleProjects, responseObserver);

        final List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertEquals(sampleProjects.size(), messageList.size());
        final Collection<Project> allCustomers = sampleProjects.values();
        for (Any singleRecord : messageList) {
            final Project unpackedSingleResult = AnyPacker.unpack(singleRecord);
            assertTrue(allCustomers.contains(unpackedSingleResult));
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private static void doCheckReadingCustomersByIdAndFieldMask(String... paths) {
        final Stand stand = prepareStandWithAggregateRepo(InMemoryStandStorage.newBuilder().build());

        final int querySize = 2;

        final Set<CustomerId> ids = new HashSet<>();

        for (int i = 0; i < querySize; i++) {
            final Customer customer = getSampleCustomer().toBuilder()
                    .setId(CustomerId.newBuilder().setNumber(i))
                    .build();

            stand.update(customer.getId(), AnyPacker.pack(customer));

            ids.add(customer.getId());
        }

        final Query customerQuery = Queries.readByIds(Customer.class, ids, paths);

        final FieldMask fieldMask = FieldMask.newBuilder().addAllPaths(Arrays.asList(paths)).build();

        final MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse value) {
                super.onNext(value);
                final List<Any> messages = value.getMessagesList();
                assertSize(ids.size(), messages);

                for (Any message : messages) {
                    final Customer customer = AnyPacker.unpack(message);

                    assertNotEquals(customer, null);

                    assertMatches(customer, fieldMask);
                }
            }
        };

        stand.execute(customerQuery, observer);

        verifyObserver(observer);
    }

    private static void doCheckReadingCustomersById(int numberOfCustomers) {
        // Define the types and values used as a test data.
        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final Map<CustomerId, Customer> sampleCustomers = fillSampleCustomers(numberOfCustomers);

        // Prepare the stand and its mock storage to act.
        final StandStorage standStorageMock = mock(StandStorage.class);
        setupExpectedBulkReadBehaviour(sampleCustomers, customerType, standStorageMock);

        final Stand stand = prepareStandWithAggregateRepo(standStorageMock);
        triggerMultipleUpdates(sampleCustomers, stand);

        final Query readMultipleCustomers = Queries.readByIds(Customer.class, sampleCustomers.keySet());

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readMultipleCustomers, responseObserver);

        final List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertEquals(sampleCustomers.size(), messageList.size());
        final Collection<Customer> allCustomers = sampleCustomers.values();
        for (Any singleRecord : messageList) {
            final Customer unpackedSingleResult = AnyPacker.unpack(singleRecord);
            assertTrue(allCustomers.contains(unpackedSingleResult));
        }
    }

    private static void checkEmptyResultOnNonEmptyStorageForQueryTarget(Target customerTarget) {
        final StandStorage standStorageMock = mock(StandStorage.class);

        // Return non-empty results on any storage read call.
        final EntityStorageRecord someRecord = EntityStorageRecord.getDefaultInstance();
        final ImmutableList<EntityStorageRecord> nonEmptyList = ImmutableList.<EntityStorageRecord>builder().add(someRecord)
                                                                                                            .build();
        when(standStorageMock.readAllByType(any(TypeUrl.class))).thenReturn(nonEmptyList);
        when(standStorageMock.read(any(AggregateStateId.class))).thenReturn(someRecord);
        when(standStorageMock.readAll()).thenReturn(Maps.<AggregateStateId, EntityStorageRecord>newHashMap());
        when(standStorageMock.readBulk(ArgumentMatchers.<AggregateStateId>anyIterable())).thenReturn(nonEmptyList);

        final Stand stand = prepareStandWithAggregateRepo(standStorageMock);

        final Query queryWithNoFilters = Query.newBuilder()
                                              .setTarget(customerTarget)
                                              .build();

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(queryWithNoFilters, responseObserver);

        verifyObserver(responseObserver);

        final List<Any> messageList = checkAndGetMessageList(responseObserver);
        assertTrue("Query returned a non-empty response message list though the filter was not set", messageList.isEmpty());
    }


    @SuppressWarnings("ConstantConditions")
    private static void setupExpectedBulkReadBehaviour(Map<CustomerId, Customer> sampleCustomers, TypeUrl customerType,
            StandStorage standStorageMock) {
        final ImmutableList.Builder<AggregateStateId> stateIdsBuilder = ImmutableList.builder();
        final ImmutableList.Builder<EntityStorageRecord> recordsBuilder = ImmutableList.builder();
        for (CustomerId customerId : sampleCustomers.keySet()) {
            final AggregateStateId stateId = AggregateStateId.of(customerId, customerType);
            final Customer customer = sampleCustomers.get(customerId);
            final Any customerState = AnyPacker.pack(customer);
            final EntityStorageRecord entityStorageRecord = EntityStorageRecord.newBuilder()
                                                                               .setState(customerState)
                                                                               .build();
            stateIdsBuilder.add(stateId);
            recordsBuilder.add(entityStorageRecord);

            when(standStorageMock.read(eq(stateId))).thenReturn(entityStorageRecord);
        }

        final ImmutableList<AggregateStateId> stateIds = stateIdsBuilder.build();
        final ImmutableList<EntityStorageRecord> records = recordsBuilder.build();

        final Iterable<AggregateStateId> matchingIds = argThat(aggregateIdsIterableMatcher(stateIds));
        when(standStorageMock.readBulk(matchingIds)).thenReturn(records);
    }

    @SuppressWarnings("ConstantConditions")
    private static void setupExpectedFindAllBehaviour(Map<ProjectId, Project> sampleProjects,
            StandTestProjectionRepository projectionRepository) {

        final Set<ProjectId> projectIds = sampleProjects.keySet();
        final ImmutableCollection<Given.StandTestProjection> allResults = toProjectionCollection(projectIds);

        for (ProjectId projectId : projectIds) {
            when(projectionRepository.load(eq(projectId))).thenReturn(new StandTestProjection(projectId));
        }

        final Iterable<ProjectId> matchingIds = argThat(projectionIdsIterableMatcher(projectIds));

        when(projectionRepository.loadAll(matchingIds, any(FieldMask.class))).thenReturn(allResults);

        when(projectionRepository.loadAll()).thenReturn(allResults);

        final EntityFilters matchingFilter = argThat(entityFilterMatcher(projectIds));
        when(projectionRepository.find(matchingFilter, any(FieldMask.class))).thenReturn(allResults);
    }

    @SuppressWarnings("OverlyComplexAnonymousInnerClass")
    private static ArgumentMatcher<EntityFilters> entityFilterMatcher(final Collection<ProjectId> projectIds) {
        // This argument matcher does NOT mimic the exact repository behavior.
        // Instead, it only matches the EntityFilters instance in case it has EntityIdFilter with ALL the expected IDs.
        return new ArgumentMatcher<EntityFilters>() {
            @Override
            public boolean matches(EntityFilters argument) {
                boolean everyElementPresent = true;
                for (EntityId entityId : argument.getIdFilter()
                                                 .getIdsList()) {
                    final Any idAsAny = entityId.getId();
                    final Message rawId = AnyPacker.unpack(idAsAny);
                    if (rawId instanceof ProjectId) {
                        final ProjectId convertedProjectId = (ProjectId) rawId;
                        everyElementPresent = everyElementPresent && projectIds.contains(convertedProjectId);
                    } else {
                        everyElementPresent = false;
                    }
                }
                return everyElementPresent;
            }
        };
    }

    private static ImmutableCollection<Given.StandTestProjection> toProjectionCollection(Collection<ProjectId> values) {
        final Collection<Given.StandTestProjection> transformed = Collections2.transform(values, new Function<ProjectId, Given.StandTestProjection>() {
            @Nullable
            @Override
            public StandTestProjection apply(@Nullable ProjectId input) {
                checkNotNull(input);
                return new StandTestProjection(input);
            }
        });
        final ImmutableList<Given.StandTestProjection> result = ImmutableList.copyOf(transformed);
        return result;
    }

    private static ArgumentMatcher<Iterable<ProjectId>> projectionIdsIterableMatcher(final Set<ProjectId> projectIds) {
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

    private static ArgumentMatcher<Iterable<AggregateStateId>> aggregateIdsIterableMatcher(final List<AggregateStateId> stateIds) {
        return new ArgumentMatcher<Iterable<AggregateStateId>>() {
            @Override
            public boolean matches(Iterable<AggregateStateId> argument) {
                boolean everyElementPresent = true;
                for (AggregateStateId aggregateStateId : argument) {
                    everyElementPresent = everyElementPresent && stateIds.contains(aggregateStateId);
                }
                return everyElementPresent;
            }
        };
    }

    private static void triggerMultipleUpdates(Map<CustomerId, Customer> sampleCustomers, Stand stand) {
        // Trigger the aggregate state updates.
        for (CustomerId id : sampleCustomers.keySet()) {
            final Customer sampleCustomer = sampleCustomers.get(id);
            final Any customerState = AnyPacker.pack(sampleCustomer);
            stand.update(id, customerState);
        }
    }

    private static Map<CustomerId, Customer> fillSampleCustomers(int numberOfCustomers) {
        final Map<CustomerId, Customer> sampleCustomers = newHashMap();

        @SuppressWarnings("UnsecureRandomNumberGeneration")
        final Random randomizer = new Random(Integer.MAX_VALUE);    // force non-negative numeric ID values.

        for (int customerIndex = 0; customerIndex < numberOfCustomers; customerIndex++) {


            final int numericId = randomizer.nextInt();
            final CustomerId customerId = customerIdFor(numericId);
            final Customer customer = Customer.newBuilder()
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
        final Random randomizer = new Random(Integer.MAX_VALUE);    // force non-negative numeric ID values.

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

    private static void fillSampleProjects(Map<ProjectId, Project> sampleProjects, int numberOfProjects) {
        for (int projectIndex = 0; projectIndex < numberOfProjects; projectIndex++) {
            final Project project = Project.getDefaultInstance();
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(UUID.randomUUID()
                                                            .toString())
                                                 .build();
            sampleProjects.put(projectId, project);
        }
    }

    private static void fillRichSampleProjects(Map<ProjectId, Project> sampleProjects, int numberOfProjects) {
        for (int projectIndex = 0; projectIndex < numberOfProjects; projectIndex++) {
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(UUID.randomUUID()
                                                            .toString())
                                                 .build();

            final Project project = Project.newBuilder()
                    .setId(projectId)
                    .setName(String.valueOf(projectIndex))
                    .setStatus(Project.Status.CREATED)
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


    private static Stand prepareStandWithAggregateRepo(StandStorage standStorage) {
        final Stand stand = Stand.newBuilder()
                                 .setStorage(standStorage)
                                 .build();
        assertNotNull(stand);

        final BoundedContext boundedContext = newBoundedContext(stand);
        final CustomerAggregateRepository customerAggregateRepo = new CustomerAggregateRepository(boundedContext);
        stand.registerTypeSupplier(customerAggregateRepo);
        return stand;
    }

    private static Stand prepareStandWithProjectionRepo(ProjectionRepository projectionRepository) {
        final Stand stand = Stand.newBuilder()
                                 .build();
        assertNotNull(stand);
        stand.registerTypeSupplier(projectionRepository);
        return stand;
    }

    private static EntityStorageRecord recordStateMatcher(final EntityStorageRecord expectedRecord) {
        return argThat(new ArgumentMatcher<EntityStorageRecord>() {
            @Override
            public boolean matches(EntityStorageRecord argument) {
                final boolean matchResult = Objects.equals(expectedRecord.getState(), argument.getState());
                return matchResult;
            }
        });
    }

    private static void checkTypesEmpty(Stand stand) {
        assertTrue(stand.getAvailableTypes()
                        .isEmpty());
        assertTrue(stand.getKnownAggregateTypes()
                        .isEmpty());
    }

    private static void checkHasExactlyOne(Set<TypeUrl> availableTypes, Descriptors.Descriptor expectedType) {
        assertEquals(1, availableTypes.size());

        final TypeUrl actualTypeUrl = availableTypes.iterator()
                                                    .next();
        final TypeUrl expectedTypeUrl = TypeUrl.of(expectedType);
        assertEquals("Type was registered incorrectly", expectedTypeUrl, actualTypeUrl);
    }

    private static Stand.StandUpdateCallback emptyUpdateCallback() {
        return new Stand.StandUpdateCallback() {
            @Override
            public void onEntityStateUpdate(Any newEntityState) {
                //do nothing
            }
        };
    }

    private static void assertMatches(Message message, FieldMask fieldMask) {
        final List<String> paths = fieldMask.getPathsList();
        for (Descriptors.FieldDescriptor field : message.getDescriptorForType().getFields()) {

            // Protobuf limitation, has no effect on the test.
            if (field.isRepeated()) {
                continue;
            }

            assertEquals(message.hasField(field), paths.contains(field.getFullName()));
        }
    }


    // ***** Inner classes used for tests. *****

    /**
     * A {@link StreamObserver} storing the state of {@link Query} execution.
     */
    private static class MemoizeQueryResponseObserver implements StreamObserver<QueryResponse> {

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

    }

    private static class MemoizeStandUpdateCallback implements Stand.StandUpdateCallback {

        private Any newEntityState;

        @Override
        public void onEntityStateUpdate(Any newEntityState) {
            this.newEntityState = newEntityState;
        }
    }
}
