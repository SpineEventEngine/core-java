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
import com.google.protobuf.Descriptors;
import org.junit.Test;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.BoundedContext;
import org.spine3.server.Given;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.storage.StandStorage;
import org.spine3.test.clientservice.customer.Customer;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
    // TODO[alex.tymchenko]: either add more meaningful checks or remove it.
    public void initialize_with_storage_provided_through_builder() {
        final StandStorage standStorageMock = spy(mock(StandStorage.class));
        final Stand stand = Stand.newBuilder()
                                 .setStorage(standStorageMock)
                                 .build();
        assertNotNull(stand);
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


    // **** Negative scenarios ****

    /**
     * - fail to initialize with improper build arguments.
     */


    // ***** Inner classes used for tests. *****

    private static class StandTestProjection extends Projection<ProjectId, Project> {
        /**
         * Creates a new instance.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the supported types
         */
        public StandTestProjection(ProjectId id) {
            super(id);
        }
    }


    private static class StandTestProjectionRepository extends ProjectionRepository<ProjectId, StandTestProjection, Project> {
        protected StandTestProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }
}
