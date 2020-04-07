/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.ResponseFormat;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.SubscriptionValidationError;
import io.spine.client.Subscriptions;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.client.Targets;
import io.spine.client.Topic;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.MessageId;
import io.spine.core.Responses;
import io.spine.core.TenantId;
import io.spine.core.Version;
import io.spine.grpc.MemoizingObserver;
import io.spine.people.PersonName;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.Given.CustomerAggregate;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.Repository;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.stand.given.Given.StandTestProjectionRepository;
import io.spine.server.stand.given.StandTestEnv.MemoizeQueryResponseObserver;
import io.spine.server.stand.given.StandTestEnv.MemoizeSubscriptionCallback;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import io.spine.test.commandservice.customer.command.CreateCustomer;
import io.spine.test.commandservice.customer.event.CustomerCreated;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.TestBoundedContext;
import io.spine.testing.server.tenant.TenantAwareTest;
import io.spine.type.TypeUrl;
import io.spine.validate.ValidationError;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.client.QueryValidationError.INVALID_QUERY;
import static io.spine.client.QueryValidationError.UNSUPPORTED_QUERY_TARGET;
import static io.spine.client.TopicValidationError.INVALID_TOPIC;
import static io.spine.client.TopicValidationError.UNSUPPORTED_TOPIC_TARGET;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Messages.isNotDefault;
import static io.spine.server.entity.given.Given.aggregateOfClass;
import static io.spine.server.entity.given.Given.projectionOfClass;
import static io.spine.server.stand.given.Given.StandTestProjection;
import static io.spine.server.stand.given.StandTestEnv.newStand;
import static io.spine.test.projection.Project.Status.CANCELLED;
import static io.spine.test.projection.Project.Status.STARTED;
import static io.spine.test.projection.Project.Status.UNDEFINED;
import static io.spine.testing.Tests.assertMatchesMask;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// It's OK for this test.
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
                .setActor(GivenUserId.of(newUuid()));
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
            BoundedContext boundedContext = BoundedContextBuilder
                    .assumingTests(multitenant)
                    .build();
            Stand stand = boundedContext.stand();

            checkTypesEmpty(stand);

            StandTestProjectionRepository standTestProjectionRepo =
                    new StandTestProjectionRepository();
            stand.registerTypeSupplier(standTestProjectionRepo);
            checkHasExactlyOne(stand.exposedTypes(), Project.getDescriptor());

            ImmutableSet<TypeUrl> knownAggregateTypes = stand.exposedAggregateTypes();
            // As we registered a projection repo, known aggregate types should be still empty.
            assertTrue(knownAggregateTypes.isEmpty(),
                       "For some reason an aggregate type was registered");

            StandTestProjectionRepository anotherTestProjectionRepo =
                    new StandTestProjectionRepository();
            stand.registerTypeSupplier(anotherTestProjectionRepo);
            checkHasExactlyOne(stand.exposedTypes(), Project.getDescriptor());
        }

        @Test
        @DisplayName("aggregate repositories")
        void aggregateRepositories() {
            BoundedContext boundedContext = BoundedContextBuilder.assumingTests().build();
            Stand stand = boundedContext.stand();

            checkTypesEmpty(stand);

            CustomerAggregateRepository customerAggregateRepo = new CustomerAggregateRepository();
            stand.registerTypeSupplier(customerAggregateRepo);

            Descriptor customerEntityDescriptor = Customer.getDescriptor();
            checkHasExactlyOne(stand.exposedTypes(), customerEntityDescriptor);
            checkHasExactlyOne(stand.exposedAggregateTypes(), customerEntityDescriptor);

            @SuppressWarnings("LocalVariableNamingConvention")
            CustomerAggregateRepository anotherCustomerAggregateRepo =
                    new CustomerAggregateRepository();
            stand.registerTypeSupplier(anotherCustomerAggregateRepo);
            checkHasExactlyOne(stand.exposedTypes(), customerEntityDescriptor);
            checkHasExactlyOne(stand.exposedAggregateTypes(), customerEntityDescriptor);
        }
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

            List<EntityStateWithVersion> messageList = checkAndGetMessageList(responseObserver);
            assertTrue(
                    messageList.isEmpty(),
                    "Query returned a non-empty response message list though the target had " +
                            "been empty"
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
            Stand stand = createStand(repository);

            int querySize = 2;
            int projectVersion = 1;

            Set<ProjectId> ids = new HashSet<>();
            for (int i = 0; i < querySize; i++) {
                Project project = Project
                        .newBuilder()
                        .setId(projectIdFor(i))
                        .setName(String.valueOf(i))
                        .setStatus(STARTED)
                        .build();
                repository.store(projectionOfClass(StandTestProjection.class)
                                         .withId(project.getId())
                                         .withState(project)
                                         .withVersion(projectVersion)
                                         .build());
                ids.add(project.getId());
            }

            Query query = requestFactory.query()
                                        .byIdsWithMask(Project.class, ids, paths);

            FieldMask fieldMask = FieldMask.newBuilder()
                                           .addAllPaths(asList(paths))
                                           .build();
            MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
                @Override
                public void onNext(QueryResponse value) {
                    super.onNext(value);
                    List<EntityStateWithVersion> messages = value.getMessageList();
                    assertThat(messages).hasSize(ids.size());
                    for (EntityStateWithVersion stateWithVersion : messages) {
                        Any state = stateWithVersion.getState();
                        Project project = unpack(state, Project.class);
                        assertThat(project).isNotNull();
                        assertMatchesMask(project, fieldMask);

                        Version version = stateWithVersion.getVersion();
                        assertThat(version.getNumber())
                                .isEqualTo(projectVersion);
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
            // Subscribe to changes of Customer aggregate.
            CustomerAggregateRepository repository = new CustomerAggregateRepository();
            Stand stand = createStand(repository);
            Topic allCustomers = requestFactory.topic()
                                               .allOf(Customer.class);

            MemoizeSubscriptionCallback callback = new MemoizeSubscriptionCallback();
            subscribeAndActivate(stand, allCustomers, callback);
            assertNull(callback.newEntityState());

            // Post a new entity state.
            Customer customer = fillSampleCustomers(1)
                    .iterator()
                    .next();
            CustomerId customerId = customer.getId();
            int version = 1;
            CustomerAggregate entity = aggregateOfClass(CustomerAggregate.class)
                    .withId(customerId)
                    .withState(customer)
                    .withVersion(version)
                    .build();

            stand.post(entity, repository.lifecycleOf(customerId));

            // Check the callback is called with the correct value.
            Any packedState = AnyPacker.pack(customer);
            assertEquals(packedState, callback.newEntityState());
        }

        @Test
        @DisplayName("upon update of projection")
        void uponUpdateOfProjection() {
            // Subscribe to changes of StandTest projection.
            StandTestProjectionRepository repository = new StandTestProjectionRepository();
            Stand stand = createStand(repository);
            Topic allProjects = requestFactory.topic()
                                              .allOf(Project.class);

            MemoizeSubscriptionCallback callback = new MemoizeSubscriptionCallback();
            subscribeAndActivate(stand, allProjects, callback);
            assertNull(callback.newEntityState());

            // Post a new entity state.
            Project project = fillSampleProjects(1)
                    .iterator()
                    .next();
            ProjectId projectId = project.getId();
            int version = 1;
            StandTestProjection entity = projectionOfClass(StandTestProjection.class)
                    .withId(projectId)
                    .withState(project)
                    .withVersion(version)
                    .build();
            stand.post(entity, repository.lifecycleOf(projectId));

            // Check the callback is called with the correct value.
            Any packedState = AnyPacker.pack(project);
            assertThat(packedState).isEqualTo(callback.newEntityState());
        }

        @SuppressWarnings("OverlyCoupledMethod") // Huge end-to-end test.
        @Test
        @DisplayName("upon event of observed type received")
        void uponEvent() {
            // Subscribe to Customer aggregate updates.
            CustomerAggregateRepository repository = new CustomerAggregateRepository();
            Stand stand = createStand(repository);
            Topic topic = requestFactory.topic()
                                        .allOf(CustomerCreated.class);
            MemoizeSubscriptionCallback callback = new MemoizeSubscriptionCallback();
            subscribeAndActivate(stand, topic, callback);

            // Send a command creating a new Customer and triggering a CustomerCreated event.
            Customer customer = fillSampleCustomers(1)
                    .iterator()
                    .next();
            CustomerId customerId = customer.getId();
            CreateCustomer createCustomer = CreateCustomer
                    .newBuilder()
                    .setCustomerId(customerId)
                    .setCustomer(customer)
                    .build();
            Command command = requestFactory.command()
                                            .create(createCustomer);
            CommandEnvelope cmd = CommandEnvelope.of(command);
            repository.dispatch(cmd);

            // Check the callback is called with the correct event.
            Event event = callback.newEvent();
            assertNotNull(event);

            EventContext context = event.context();
            MessageId origin = context.getPastMessage()
                                      .messageId();
            assertThat(origin.asCommandId()).isEqualTo(cmd.id());
            assertThat(origin.getTypeUrl()).isEqualTo(cmd.command()
                                                         .enclosedTypeUrl()
                                                         .value());
            Any packedMessage = event.getMessage();
            CustomerCreated eventMessage = unpack(packedMessage, CustomerCreated.class);

            assertThat(eventMessage.getCustomerId())
                    .isEqualTo(customerId);
            assertThat(eventMessage.getCustomer())
                    .isEqualTo(customer);
        }
    }

    @Test
    @DisplayName("trigger subscription callbacks matching by ID")
    void triggerSubscriptionsMatchingById() {
        CustomerAggregateRepository repository = new CustomerAggregateRepository();
        Stand stand = createStand(repository);

        Collection<Customer> sampleCustomers = fillSampleCustomers(10);

        Topic someCustomers = requestFactory.topic()
                                            .select(Customer.class)
                                            .byId(ids(sampleCustomers))
                                            .build();
        Collection<Customer> callbackStates = newHashSet();
        MemoizeSubscriptionCallback callback = new MemoizeSubscriptionCallback() {
            @Override
            public void accept(SubscriptionUpdate update) {
                super.accept(update);
                Customer customerInCallback = (Customer) update.state(0);
                callbackStates.add(customerInCallback);
            }
        };
        subscribeAndActivate(stand, someCustomers, callback);

        for (Customer customer : sampleCustomers) {
            CustomerId customerId = customer.getId();
            int version = 1;
            CustomerAggregate entity = aggregateOfClass(CustomerAggregate.class)
                    .withId(customerId)
                    .withState(customer)
                    .withVersion(version)
                    .build();
            stand.post(entity, repository.lifecycleOf(customerId));
        }

        assertEquals(newHashSet(sampleCustomers), callbackStates);
    }

    @Test
    @DisplayName("allow cancelling subscriptions")
    void cancelSubscriptions() {
        CustomerAggregateRepository repository = new CustomerAggregateRepository();
        Stand stand = createStand(repository);
        Topic allCustomers = requestFactory.topic()
                                           .allOf(Customer.class);

        MemoizeSubscriptionCallback callback = new MemoizeSubscriptionCallback();
        Subscription subscription =
                subscribeAndActivate(stand, allCustomers, callback);

        stand.cancel(subscription, noOpObserver());

        Customer customer = fillSampleCustomers(1)
                .iterator()
                .next();
        CustomerId customerId = customer.getId();
        int version = 1;
        CustomerAggregate entity = aggregateOfClass(CustomerAggregate.class)
                .withId(customerId)
                .withState(customer)
                .withVersion(version)
                .build();
        stand.post(entity, repository.lifecycleOf(customerId));

        assertNull(callback.newEntityState());
    }

    @Test
    @DisplayName("fail if cancelling non-existent subscription")
    void notCancelNonExistent() {
        Stand stand = createStand();
        Subscription nonExistingSubscription = Subscription
                .newBuilder()
                .setId(Subscriptions.generateId())
                .build();
        assertThrows(InvalidSubscriptionException.class,
                     () -> stand.cancel(nonExistingSubscription, noOpObserver()));
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    @DisplayName("trigger each subscription callback once for multiple subscriptions")
    void triggerSubscriptionCallbackOnce() {
        CustomerAggregateRepository repository = new CustomerAggregateRepository();
        Stand stand = createStand(repository);
        Target allCustomers = Targets.allOf(Customer.class);

        Set<MemoizeSubscriptionCallback> callbacks = newHashSet();
        int totalCallbacks = 100;

        for (int callbackIndex = 0; callbackIndex < totalCallbacks; callbackIndex++) {
            MemoizeSubscriptionCallback callback = subscribeWithCallback(stand, allCustomers);
            callbacks.add(callback);
        }

        Customer customer = fillSampleCustomers(1)
                .iterator()
                .next();
        CustomerId customerId = customer.getId();
        int version = 1;
        CustomerAggregate entity = aggregateOfClass(CustomerAggregate.class)
                .withId(customerId)
                .withState(customer)
                .withVersion(version)
                .build();
        stand.post(entity, repository.lifecycleOf(customerId));

        Any packedState = AnyPacker.pack(customer);
        for (MemoizeSubscriptionCallback callback : callbacks) {
            assertEquals(packedState, callback.newEntityState());
            assertEquals(1, callback.countAcceptedUpdates());
        }
    }

    @Test
    @DisplayName("not trigger subscription callbacks in case of another type criterion mismatch")
    void notTriggerOnTypeMismatch() {
        CustomerAggregateRepository repository = new CustomerAggregateRepository();
        StandTestProjectionRepository projectionRepository = new StandTestProjectionRepository();
        Stand stand = createStand(repository, projectionRepository);
        Target allProjects = Targets.allOf(Project.class);
        MemoizeSubscriptionCallback callback = subscribeWithCallback(stand, allProjects);
        Customer customer = fillSampleCustomers(1)
                .iterator()
                .next();
        CustomerId customerId = customer.getId();
        int version = 1;
        CustomerAggregate entity = aggregateOfClass(CustomerAggregate.class)
                .withId(customerId)
                .withState(customer)
                .withVersion(version)
                .build();
        stand.post(entity, repository.lifecycleOf(customerId));

        assertEquals(0, callback.countAcceptedUpdates());
    }

    private MemoizeSubscriptionCallback  subscribeWithCallback(Stand stand, Target subscriptionTarget) {
        MemoizeSubscriptionCallback callback = new MemoizeSubscriptionCallback();
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
    @DisplayName("query `AggregateRepository` for aggregate states")
    void readAggregates() {
        boolean multitenant = isMultitenant();
        Stand stand = Stand
                .newBuilder()
                .setMultitenant(multitenant)
                .build();
        CustomerAggregateRepository repository = new CustomerAggregateRepository();
        TestBoundedContext.create().register(repository);
        stand.registerTypeSupplier(repository);
        Query query = getRequestFactory().query()
                                         .all(Customer.class);
        stand.execute(query, noOpObserver());

        Optional<TargetFilters> actualFilter = repository.memoizedFilters();
        assertThat(actualFilter).isPresent();
        assertThat(actualFilter.get()).isEqualTo(query.filters());

        Optional<ResponseFormat> actualFormat = repository.memoizedFormat();
        assertThat(actualFormat).isPresent();
        assertThat(actualFormat.get()).isEqualTo(query.getFormat());
    }

    @Test
    @MuteLogging
    @DisplayName("handle mistakes in query silently")
    void handleMistakesInQuery() {
        StandTestProjectionRepository repository = new StandTestProjectionRepository();
        Stand stand = createStand(repository);
        Project sampleProject = Project
                .newBuilder()
                .setId(projectIdFor(42))
                .setName("Test Project")
                .setStatus(CANCELLED)
                .build();
        int projectVersion = 42;
        repository.store(projectionOfClass(StandTestProjection.class)
                                 .withId(sampleProject.getId())
                                 .withState(sampleProject)
                                 .withVersion(projectVersion)
                                 .build());
        // FieldMask with invalid field paths.
        String[] paths = {"invalid_field_path_example", Project.getDescriptor()
                                                               .getFields()
                                                               .get(2).getFullName()};
        Query query = requestFactory.query()
                                    .allWithMask(Project.class, paths);
        MemoizeQueryResponseObserver observer = new MemoizeQueryResponseObserver() {
            @Override
            public void onNext(QueryResponse response) {
                super.onNext(response);
                assertFalse(response.isEmpty());

                Project project = (Project) response.state(0);

                assertNotNull(project);

                assertFalse(project.hasId());
                assertThat(project.getName())
                        .isEmpty();
                assertEquals(UNDEFINED, project.getStatus());
                assertThat(project.getTaskList())
                        .isEmpty();

                Version version = response.version(0);
                assertThat(version.getNumber())
                        .isEqualTo(projectVersion);
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
                               .setMultitenant(isMultitenant())
                               .build();
            checkTypesEmpty(stand);

            // Customer type was NOT registered.
            // So create a query for an unknown type.
            Query readAllCustomers = requestFactory.query()
                                                   .all(Customer.class);
            InvalidQueryException exception =
                    assertThrows(InvalidQueryException.class,
                                 () -> stand.execute(readAllCustomers, noOpObserver()));
            assertEquals(readAllCustomers, exception.getRequest());

            assertEquals(UNSUPPORTED_QUERY_TARGET.getNumber(),
                         exception.asError().getCode());
        }

        @Test
        @DisplayName("if invalid query message is passed")
        void ifInvalidQueryMessagePassed() {
            Stand stand = createStand();
            Query invalidQuery = Query.getDefaultInstance();

            InvalidQueryException exception =
                    assertThrows(InvalidQueryException.class,
                                 () -> stand.execute(invalidQuery, noOpObserver()));
            assertEquals(invalidQuery, exception.getRequest());

            assertEquals(INVALID_QUERY.getNumber(),
                         exception.asError().getCode());
            ValidationError validationError = exception.asError()
                                                       .getValidationError();
            assertTrue(isNotDefault(validationError));
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
                               .build();
            checkTypesEmpty(stand);

            // Project type was NOT registered.
            // So create a topic for an unknown type.
            Topic allProjectsTopic = requestFactory.topic()
                                                   .allOf(Project.class);
            InvalidTopicException exception =
                    assertThrows(InvalidTopicException.class,
                                 () -> stand.subscribe(allProjectsTopic, noOpObserver()));
            assertEquals(allProjectsTopic, exception.getRequest());

            assertEquals(UNSUPPORTED_TOPIC_TARGET.getNumber(),
                         exception.asError().getCode());
        }

        @Test
        @DisplayName("if invalid topic message is passed")
        void ifInvalidTopicMessagePassed() {
            Stand stand = createStand();
            Topic invalidTopic = Topic.getDefaultInstance();
            InvalidTopicException exception =
                    assertThrows(InvalidTopicException.class,
                                 () -> stand.subscribe(invalidTopic, noOpObserver()));
            assertEquals(invalidTopic, exception.getRequest());

            assertEquals(INVALID_TOPIC.getNumber(),
                         exception.asError()
                                       .getCode());

            ValidationError validationError = exception.asError()
                                                       .getValidationError();
            assertTrue(isNotDefault(validationError));
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

            InvalidSubscriptionException exception =
                    assertThrows(InvalidSubscriptionException.class,
                                 () -> stand.activate(subscription,
                                                      emptyUpdateCallback(),
                                                      noOpObserver()));
            verifyUnknownSubscription(subscription, exception);
        }

        @Test
        @DisplayName("if cancelling subscription with unsupported target")
        void ifCancelWithUnsupportedTarget() {
            Stand stand = createStand();
            Subscription subscription = subscriptionWithUnknownTopic();

            InvalidSubscriptionException exception =
                    assertThrows(InvalidSubscriptionException.class,
                                 () -> stand.cancel(subscription, noOpObserver()));
            verifyUnknownSubscription(subscription, exception);
        }

        @Test
        @DisplayName("if invalid subscription message is passed on activation")
        void ifActivateWithInvalidMessage() {
            Stand stand = createStand();
            Subscription invalidSubscription = Subscription.getDefaultInstance();

            InvalidSubscriptionException exception =
                    assertThrows(InvalidSubscriptionException.class,
                                 () -> stand.activate(invalidSubscription,
                                                      emptyUpdateCallback(),
                                                      noOpObserver()));
            verifyInvalidSubscription(invalidSubscription, exception);

        }

        @Test
        @DisplayName("if invalid subscription message is passed on cancel")
        void ifCancelWithInvalidMessage() {
            Stand stand = createStand();
            Subscription invalidSubscription = Subscription.getDefaultInstance();

            InvalidSubscriptionException exception =
                    assertThrows(InvalidSubscriptionException.class,
                                 () -> stand.cancel(invalidSubscription, noOpObserver()));
            verifyInvalidSubscription(invalidSubscription, exception);
        }

        private void verifyInvalidSubscription(Subscription invalidSubscription,
                                               InvalidSubscriptionException exception) {
            assertEquals(invalidSubscription, exception.getRequest());

            assertEquals(SubscriptionValidationError.INVALID_SUBSCRIPTION.getNumber(),
                         exception.asError()
                                  .getCode());

            ValidationError validationError = exception.asError()
                                                       .getValidationError();
            assertTrue(isNotDefault(validationError));
        }

        private void verifyUnknownSubscription(Subscription subscription,
                                               InvalidSubscriptionException exception) {
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

    @SuppressWarnings("OverloadedVarargsMethod") // OK for this helper method.
    private Stand createStand(Repository<?, ?>... repositories) {
        return newStand(isMultitenant(), repositories);
    }

    @CanIgnoreReturnValue
    protected static Subscription
    subscribeAndActivate(Stand stand, Topic topic, SubscriptionCallback callback) {
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
        Map<ProjectId, Project> sampleProjects = new HashMap<>();
        fillSampleProjects(sampleProjects, numberOfProjects);

        StandTestProjectionRepository projectionRepository = new StandTestProjectionRepository();
        setupExpectedFindAllBehaviour(sampleProjects, projectionRepository);

        Stand stand = prepareStandWithProjectionRepo(projectionRepository);

        Query readMultipleProjects = requestFactory.query()
                                                   .byIds(Project.class, sampleProjects.keySet());

        MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readMultipleProjects, responseObserver);

        List<EntityStateWithVersion> messageList = checkAndGetMessageList(responseObserver);
        assertEquals(sampleProjects.size(), messageList.size());
        Collection<Project> allCustomers = sampleProjects.values();
        for (EntityStateWithVersion singleRecord : messageList) {
            Any state = singleRecord.getState();
            Project unpackedSingleResult = unpack(state, Project.class);
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
        ImmutableCollection<EntityRecord> allRecords = toProjectionRecords(projectIds);

        projectionRepository.setRecords(allRecords.iterator());
    }

    private static ImmutableCollection<EntityRecord>
    toProjectionRecords(Collection<ProjectId> projectionIds) {
        Collection<EntityRecord> transformed = Collections2.transform(
                projectionIds,
                input -> {
                    checkNotNull(input);
                    StandTestProjection projection = new StandTestProjection(input);
                    Any id = Identifier.pack(projection.id());
                    Any state = AnyPacker.pack(projection.state());
                    EntityRecord record = EntityRecord
                            .newBuilder()
                            .setEntityId(id)
                            .setState(state)
                            .build();
                    return record;
                });
        ImmutableList<EntityRecord> result = ImmutableList.copyOf(transformed);
        return result;
    }

    static Collection<Customer> fillSampleCustomers(int numberOfCustomers) {
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
                                            .setName(String.valueOf(numericId))
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

    private static List<EntityStateWithVersion>
    checkAndGetMessageList(MemoizeQueryResponseObserver responseObserver) {
        assertTrue(responseObserver.isCompleted(), "Query has not completed successfully");
        assertNull(responseObserver.throwable(), "Throwable has been caught upon query execution");

        QueryResponse response = responseObserver.responseHandled();
        assertEquals(Responses.ok(), response.getResponse(), "Query response is not OK");
        assertNotNull(response, "Query response must not be null");

        List<EntityStateWithVersion> messages = response.getMessageList();
        assertNotNull(messages, "Query response has null message list");
        return messages;
    }

    private static void checkTypesEmpty(Stand stand) {
        assertTrue(stand.exposedTypes()
                        .isEmpty());
        assertTrue(stand.exposedAggregateTypes()
                        .isEmpty());
    }

    private static void checkHasExactlyOne(Collection<TypeUrl> availableTypes,
                                           Descriptor expectedType) {
        assertEquals(1, availableTypes.size());

        TypeUrl actualTypeUrl = availableTypes.iterator()
                                              .next();
        TypeUrl expectedTypeUrl = TypeUrl.from(expectedType);
        assertEquals(expectedTypeUrl, actualTypeUrl, "Type was registered incorrectly");
    }

    @SuppressWarnings("UnnecessaryLambda") // To give a name to an empty callback.
    private static SubscriptionCallback emptyUpdateCallback() {
        return newEntityState -> {
            //do nothing
        };
    }

    private static Set<CustomerId> ids(Collection<Customer> customers) {
        return customers.stream()
                        .map(Customer::getId)
                        .collect(toSet());
    }
}
