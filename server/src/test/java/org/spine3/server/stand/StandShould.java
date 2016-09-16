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
import org.spine3.base.Responses;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.client.Query;
import org.spine3.client.QueryResponse;
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
import org.spine3.test.clientservice.customer.Customer;
import org.spine3.test.clientservice.customer.CustomerId;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

/**
 * @author Alex Tymchenko
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

        final TypeUrl projectProjectionType = TypeUrl.of(Project.class);
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .build();
        final Target projectProjectionTarget = Target.newBuilder()
                                                     .setIncludeAll(true)
                                                     .setType(projectProjectionType.getTypeName())
                                                     .setFilters(filters)
                                                     .build();


        stand.subscribe(projectProjectionTarget, emptyUpdateCallback());

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
        final CustomerId customerId = CustomerId.newBuilder()
                                                .setNumber(numericIdValue)
                                                .build();
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

        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final Target customerTarget = Target.newBuilder()
                                            .setIncludeAll(true)
                                            .setType(customerType.getTypeName())
                                            .build();

        checkEmptyResultForTargetOnEmptyStorage(customerTarget);
    }

    @Test
    public void return_empty_list_to_unknown_type_reading() {

        final Stand stand = Stand.newBuilder()
                                 .build();

        checkTypesEmpty(stand);

        // Customer type was NOT registered.
        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final Target customerTarget = Target.newBuilder()
                                            .setIncludeAll(true)
                                            .setType(customerType.getTypeName())
                                            .build();
        final Query readAllCustomers = Query.newBuilder()
                                            .setTarget(customerTarget)
                                            .build();

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readAllCustomers, responseObserver);

        final List<Any> messageList = checkAndGetMessageList(responseObserver);

        assertTrue("Query returned a non-empty response message list for an unknown type", messageList.isEmpty());
    }

    @Test
    public void return_empty_list_for_aggregate_read_by_ids_on_empty_stand_storage() {

        final TypeUrl customerType = TypeUrl.of(Customer.class);

        final EntityId firstId = wrapCustomerId(1);
        final EntityId anotherId = wrapCustomerId(2);
        final EntityIdFilter idFilter = EntityIdFilter.newBuilder()
                                                      .addIds(firstId)
                                                      .addIds(anotherId)
                                                      .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .build();
        final Target customerTarget = Target.newBuilder()
                                            .setIncludeAll(false)
                                            .setType(customerType.getTypeName())
                                            .setFilters(filters)
                                            .build();

        checkEmptyResultForTargetOnEmptyStorage(customerTarget);
    }

    @Test
    public void return_empty_list_for_aggregate_reads_with_filters_not_set() {

        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final Target customerTarget = Target.newBuilder()
                                            .setIncludeAll(false)
                                            .setType(customerType.getTypeName())
                                            .build();
        checkEmptyResultOnNonEmptyStorageForQueryTarget(customerTarget);
    }

    @Test
    public void return_empty_list_for_aggregate_reads_with_id_filter_not_set() {

        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .build();
        final Target customerTarget = Target.newBuilder()
                                            .setIncludeAll(false)
                                            .setType(customerType.getTypeName())
                                            .setFilters(filters)
                                            .build();
        checkEmptyResultOnNonEmptyStorageForQueryTarget(customerTarget);
    }

    @Test
    public void return_empty_list_for_aggregate_reads_with_empty_list_of_ids() {

        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(EntityIdFilter.getDefaultInstance())
                                                   .build();
        final Target customerTarget = Target.newBuilder()
                                            .setIncludeAll(false)
                                            .setType(customerType.getTypeName())
                                            .setFilters(filters)
                                            .build();
        checkEmptyResultOnNonEmptyStorageForQueryTarget(customerTarget);
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
    public void trigger_callback_upon_change_of_watched_aggregate_state() {
        final Stand stand = prepareStandWithAggregateRepo(mock(StandStorage.class));
        final TypeUrl customerType = TypeUrl.of(Customer.class);

        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .build();
        final Target customerTarget = Target.newBuilder()
                                            .setIncludeAll(true)
                                            .setType(customerType.getTypeName())
                                            .setFilters(filters)
                                            .build();

        final MemoizeStandUpdateCallback memoizeCallback = new MemoizeStandUpdateCallback();
        stand.subscribe(customerTarget, memoizeCallback);
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
    public void retrieve_all_data_if_field_mask_is_not_set() {
        final Stand stand = prepareStandWithAggregateRepo(InMemoryStandStorage.newBuilder().build());
        final TypeUrl customerType = TypeUrl.of(Customer.class);

        final Customer sampleCustomer = getSampleCustomer();

        stand.update(sampleCustomer.getId(), AnyPacker.pack(sampleCustomer));

        final Query customerQuery = Query.newBuilder()
                                         .setTarget(
                                                 Target.newBuilder().setIncludeAll(true)
                                                       .setType(customerType.getTypeName())
                                                       .build())
                                         .build();


        //noinspection OverlyComplexAnonymousInnerClass
        stand.execute(customerQuery, new StreamObserver<QueryResponse>() {
            @Override
            public void onNext(QueryResponse value) {
                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer customer = AnyPacker.unpack(messages.get(0));
                for (Descriptors.FieldDescriptor field : customer.getDescriptorForType().getFields()) {
                    assertTrue(customer.getField(field).equals(sampleCustomer.getField(field)));
                }
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    @Test
    public void retrieve_only_selected_params_for_query() {
        final Stand stand = prepareStandWithAggregateRepo(InMemoryStandStorage.newBuilder().build());
        final TypeUrl customerType = TypeUrl.of(Customer.class);

        final Customer sampleCustomer = getSampleCustomer();

        stand.update(sampleCustomer.getId(), AnyPacker.pack(sampleCustomer));

        final Query customerQuery = Query.newBuilder()
                                         .setTarget(
                                                 Target.newBuilder().setIncludeAll(true)
                                                       .setType(customerType.getTypeName())
                                                       .build())
                                         .setFieldMask(FieldMask.newBuilder()
                                                                .addPaths(Customer.getDescriptor().getFields().get(1).getFullName()))
                                         .build();


        //noinspection OverlyComplexAnonymousInnerClass
        stand.execute(customerQuery, new StreamObserver<QueryResponse>() {
            @Override
            public void onNext(QueryResponse value) {
                final List<Any> messages = value.getMessagesList();
                assertFalse(messages.isEmpty());

                final Customer customer = AnyPacker.unpack(messages.get(0));
                assertTrue(customer.getName().equals(sampleCustomer.getName()));
                assertFalse(customer.hasId());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    private static Customer getSampleCustomer() {
        return Customer.newBuilder()
                       .setId(CustomerId.newBuilder().setNumber((int) UUID.randomUUID()
                                                                          .getLeastSignificantBits()))
                       .setName(PersonName.newBuilder().setGivenName("Socrates").build())
                       .build();

    }

    private static void checkEmptyResultForTargetOnEmptyStorage(Target customerTarget) {
        final StandStorage standStorageMock = mock(StandStorage.class);
        // Return an empty collection on {@link StandStorage#readAllByType(TypeUrl)} call.
        final ImmutableList<EntityStorageRecord> emptyResultList = ImmutableList.<EntityStorageRecord>builder().build();
        when(standStorageMock.readAllByType(any(TypeUrl.class))).thenReturn(emptyResultList);

        final Stand stand = prepareStandWithAggregateRepo(standStorageMock);

        final Query readAllCustomers = Query.newBuilder()
                                            .setTarget(customerTarget)
                                            .build();

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readAllCustomers, responseObserver);

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

        // Now we are ready to query.
        final EntityIdFilter idFilter = idFilterForProjection(sampleProjects.keySet());

        final Target projectTarget = Target.newBuilder()
                                           .setFilters(EntityFilters.newBuilder()
                                                                    .setIdFilter(idFilter))
                                           .setType(projectType.getTypeName())
                                           .build();
        final Query readMultipleProjects = Query.newBuilder()
                                                .setTarget(projectTarget)
                                                .build();

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

    private static void doCheckReadingCustomersById(int numberOfCustomers) {
        // Define the types and values used as a test data.
        final TypeUrl customerType = TypeUrl.of(Customer.class);
        final Map<CustomerId, Customer> sampleCustomers = fillSampleCustomers(numberOfCustomers);

        // Prepare the stand and its mock storage to act.
        final StandStorage standStorageMock = mock(StandStorage.class);
        setupExpectedBulkReadBehaviour(sampleCustomers, customerType, standStorageMock);

        final Stand stand = prepareStandWithAggregateRepo(standStorageMock);
        triggerMultipleUpdates(sampleCustomers, stand);

        // Now we are ready to query.
        final EntityIdFilter idFilter = idFilterForAggregate(sampleCustomers.keySet());

        final Target customerTarget = Target.newBuilder()
                                            .setFilters(EntityFilters.newBuilder()
                                                                     .setIdFilter(idFilter))
                                            .setType(customerType.getTypeName())
                                            .build();
        final Query readMultipleCustomers = Query.newBuilder()
                                                 .setTarget(customerTarget)
                                                 .build();

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
            final Customer customer = Customer.getDefaultInstance();
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
        final ImmutableCollection<org.spine3.server.stand.Given.StandTestProjection> allResults = toProjectionCollection(projectIds);

        for (ProjectId projectId : projectIds) {
            when(projectionRepository.find(eq(projectId))).thenReturn(new Given.StandTestProjection(projectId));
        }

        final Iterable<ProjectId> matchingIds = argThat(projectionIdsIterableMatcher(projectIds));
        when(projectionRepository.findBulk(matchingIds)).thenReturn(allResults);

        when(projectionRepository.findAll()).thenReturn(allResults);

        final EntityFilters matchingFilter = argThat(entityFilterMatcher(projectIds));
        when(projectionRepository.findAll(matchingFilter)).thenReturn(allResults);
    }

    @SuppressWarnings("OverlyComplexAnonymousInnerClass")
    private static ArgumentMatcher<EntityFilters> entityFilterMatcher(final Set<ProjectId> projectIds) {
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
            public Given.StandTestProjection apply(@Nullable ProjectId input) {
                checkNotNull(input);
                return new Given.StandTestProjection(input);
            }
        });
        final ImmutableList<Given.StandTestProjection> result = ImmutableList.copyOf(transformed);
        return result;
    }

    private static EntityId wrapCustomerId(int number) {
        final CustomerId customerId = CustomerId.newBuilder()
                                                .setNumber(number)
                                                .build();
        final Any packedId = AnyPacker.pack(customerId);
        return EntityId.newBuilder()
                       .setId(packedId)
                       .build();
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

    private static EntityIdFilter idFilterForAggregate(Collection<CustomerId> customerIds) {
        final EntityIdFilter.Builder idFilterBuilder = EntityIdFilter.newBuilder();
        for (CustomerId id : customerIds) {
            idFilterBuilder
                    .addIds(EntityId.newBuilder()
                                    .setId(AnyPacker.pack(id)));
        }
        return idFilterBuilder.build();
    }

    private static EntityIdFilter idFilterForProjection(Collection<ProjectId> projectIds) {
        final EntityIdFilter.Builder idFilterBuilder = EntityIdFilter.newBuilder();
        for (ProjectId id : projectIds) {
            idFilterBuilder
                    .addIds(EntityId.newBuilder()
                                    .setId(AnyPacker.pack(id)));
        }
        return idFilterBuilder.build();
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
        for (int customerIndex = 0; customerIndex < numberOfCustomers; customerIndex++) {
            final Customer customer = Customer.getDefaultInstance();

            @SuppressWarnings("UnsecureRandomNumberGeneration")
            final Random randomizer = new Random();
            final CustomerId customerId = CustomerId.newBuilder()
                                                    .setNumber(randomizer.nextInt())
                                                    .build();
            sampleCustomers.put(customerId, customer);
        }
        return sampleCustomers;
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


    /**
     * A {@link StreamObserver} storing the state of {@link Query} execution.
     */
    private static class MemoizeStandUpdateCallback implements Stand.StandUpdateCallback {

        private Any newEntityState;

        @Override
        public void onEntityStateUpdate(Any newEntityState) {
            this.newEntityState = newEntityState;
        }
    }
}
