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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.BoundedContext;
import org.spine3.server.Given;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.stand.Given.StandTestProjectionRepository;
import org.spine3.server.storage.StandStorage;
import org.spine3.test.clientservice.customer.Customer;
import org.spine3.test.clientservice.customer.CustomerId;
import org.spine3.test.projection.Project;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

/**
 * @author Alex Tymchenko
 */
public class StandShould {

// **** Positive scenarios ****

    /**
     * - initialize properly with various Builder options;
     * - register aggregate repositories by changing the known aggregate types.
     * - register entity repositories properly
     * - avoid duplicates while registering repositories
     */

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

        final Given.CustomerAggregateRepository customerAggregateRepo = new Given.CustomerAggregateRepository(boundedContext);
        stand.registerTypeSupplier(customerAggregateRepo);

        final Descriptors.Descriptor customerEntityDescriptor = Customer.getDescriptor();
        checkHasExactlyOne(stand.getAvailableTypes(), customerEntityDescriptor);
        checkHasExactlyOne(stand.getKnownAggregateTypes(), customerEntityDescriptor);

        @SuppressWarnings("LocalVariableNamingConvention")
        final Given.CustomerAggregateRepository anotherCustomerAggregateRepo = new Given.CustomerAggregateRepository(boundedContext);
        stand.registerTypeSupplier(anotherCustomerAggregateRepo);
        checkHasExactlyOne(stand.getAvailableTypes(), customerEntityDescriptor);
        checkHasExactlyOne(stand.getKnownAggregateTypes(), customerEntityDescriptor);
    }

    @Test
    public void use_provided_executor_upon_update_of_watched_type() {
        final Executor executor = mock(Executor.class);
        final InOrder executorInOrder = inOrder(executor);
        final Stand stand = Stand.newBuilder()
                                 .setCallbackExecutor(executor)
                                 .build();
        final BoundedContext boundedContext = newBoundedContext(stand);
        final StandTestProjectionRepository standTestProjectionRepo = new StandTestProjectionRepository(boundedContext);
        stand.registerTypeSupplier(standTestProjectionRepo);

        final TypeUrl projectProjectionType = TypeUrl.of(Project.class);
        stand.watch(projectProjectionType, emptyUpdateCallback());

        executorInOrder.verify(executor, never())
                       .execute(any(Runnable.class));

        final Any someUpdate = AnyPacker.pack(Project.getDefaultInstance());
        final Object someId = new Object();
        stand.update(someId, someUpdate);

        executorInOrder.verify(executor, calls(1))
                       .execute(any(Runnable.class));
    }

    @Test
    public void operate_with_storage_provided_through_builder() {
        final StandStorage standStorageMock = mock(StandStorage.class);
        final InOrder standStorageInOrder = inOrder(standStorageMock);
        final Stand stand = Stand.newBuilder()
                                 .setStorage(standStorageMock)
                                 .build();
        assertNotNull(stand);

        final BoundedContext boundedContext = newBoundedContext(stand);
        final Given.CustomerAggregateRepository customerAggregateRepo = new Given.CustomerAggregateRepository(boundedContext);
        stand.registerTypeSupplier(customerAggregateRepo);


        final int numericIdValue = 17;
        final CustomerId customerId = CustomerId.newBuilder()
                                                .setNumber(numericIdValue)
                                                .build();
        final Given.CustomerAggregate customerAggregate = customerAggregateRepo.create(customerId);
        final Customer customerState = customerAggregate.getState();
        final Any packedState = AnyPacker.pack(customerState);
        final TypeUrl customerType = TypeUrl.of(Customer.class);

        standStorageInOrder.verify(standStorageMock, never())
                           .write(any(AggregateStateId.class), any(EntityStorageRecord.class));

        stand.update(customerId, packedState);

        final AggregateStateId expectedAggregateStateId = AggregateStateId.of(customerId, customerType);
        final EntityStorageRecord expectedRecord = EntityStorageRecord.newBuilder()
                                                                      .setState(packedState)
                                                                      .build();
        standStorageInOrder.verify(standStorageMock, calls(1))
                           .write(eq(expectedAggregateStateId), recordStateMatcher(expectedRecord));
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


    // **** Negative scenarios ****

    /**
     * - fail to initialize with improper build arguments.
     */

}
