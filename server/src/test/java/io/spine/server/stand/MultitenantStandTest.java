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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.Topic;
import io.spine.core.Responses;
import io.spine.core.TenantId;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.server.stand.given.StandTestEnv.MemoizeEntityUpdateCallback;
import io.spine.server.stand.given.StandTestEnv.MemoizeQueryResponseObserver;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import io.spine.testing.core.given.GivenVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static io.spine.core.BoundedContextNames.newName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("Multitenant Stand should")
class MultitenantStandTest extends StandTest {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        TenantId tenantId = newUuid();

        setCurrentTenant(tenantId);
        setMultitenant(true);
        setRequestFactory(createRequestFactory(tenantId));
    }

    @AfterEach
    void tearDown() {
        clearCurrentTenant();
    }

    @Test
    @DisplayName("not allow reading aggregate records from another tenant")
    void readOnlySameTenant() {
        Stand stand = doCheckReadingCustomersById(15);

        TenantId anotherTenant = newUuid();
        ActorRequestFactory requestFactory = createRequestFactory(anotherTenant);

        Query readAllCustomers = requestFactory.query()
                                               .all(Customer.class);

        MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readAllCustomers, responseObserver);
        QueryResponse response = responseObserver.responseHandled();
        assertTrue(Responses.isOk(response.getResponse()));
        assertEquals(0, response.getRecordsCount());
    }

    @Test
    @DisplayName("not trigger updates of aggregate records for another tenant subscriptions")
    void updateOnlySameTenant() {
        StandStorage standStorage =
                InMemoryStorageFactory.newInstance(newName(getClass().getSimpleName()),
                                                   isMultitenant())
                                      .createStandStorage();
        Stand stand = prepareStandWithAggregateRepo(standStorage);

        // --- Default Tenant
        ActorRequestFactory requestFactory = getRequestFactory();
        MemoizeEntityUpdateCallback defaultTenantCallback =
                subscribeToAllOf(stand, requestFactory, Customer.class);

        // --- Another Tenant
        TenantId anotherTenant = newUuid();
        ActorRequestFactory anotherFactory = createRequestFactory(anotherTenant);
        MemoizeEntityUpdateCallback anotherCallback =
                subscribeToAllOf(stand, anotherFactory, Customer.class);

        // Trigger updates in Default Tenant.
        Map.Entry<CustomerId, Customer> sampleData =
                fillSampleCustomers(1).entrySet()
                                      .iterator()
                                      .next();
        CustomerId customerId = sampleData.getKey();
        Customer customer = sampleData.getValue();
        Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        Any packedState = AnyPacker.pack(customer);
        // Verify that Default Tenant callback has got the update.
        assertEquals(packedState, defaultTenantCallback.newEntityState());

        // And Another Tenant callback has not been called.
        assertNull(anotherCallback.newEntityState());
    }

    protected MemoizeEntityUpdateCallback subscribeToAllOf(Stand stand,
                                                           ActorRequestFactory requestFactory,
                                                           Class<? extends Message> entityClass) {
        Topic allCustomers = requestFactory.topic()
                                           .allOf(entityClass);
        MemoizeEntityUpdateCallback callback = new MemoizeEntityUpdateCallback();
        subscribeAndActivate(stand, allCustomers, callback);

        assertNull(callback.newEntityState());
        return callback;
    }
}
