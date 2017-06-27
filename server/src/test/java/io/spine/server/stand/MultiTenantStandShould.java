/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.core.given.GivenVersion;
import io.spine.protobuf.AnyPacker;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static io.spine.core.TestIdentifiers.newTenantUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class MultiTenantStandShould extends StandShould {

    @Override
    @Before
    public void setUp() {
        final TenantId tenantId = newTenantUuid();

        setCurrentTenant(tenantId);
        setMultitenant(true);
        setRequestFactory(createRequestFactory(tenantId));
    }

    @After
    public void tearDown() {
        clearCurrentTenant();
    }

    @Test
    public void not_allow_reading_aggregate_records_for_another_tenant() {
        final Stand stand = doCheckReadingCustomersById(15);

        final TenantId anotherTenant = newTenantUuid();
        final ActorRequestFactory requestFactory = createRequestFactory(anotherTenant);

        final Query readAllCustomers = requestFactory.query().all(Customer.class);

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readAllCustomers, responseObserver);
        final QueryResponse response = responseObserver.getResponseHandled();
        assertTrue(Responses.isOk(response.getResponse()));
        assertEquals(0, response.getMessagesCount());
    }

    @Test
    public void not_trigger_updates_of_aggregate_records_for_another_tenant_subscriptions() {
        final StandStorage standStorage =
                InMemoryStorageFactory.newInstance(getClass().getSimpleName(),
                                                   isMultitenant())
                                      .createStandStorage();
        final Stand stand = prepareStandWithAggregateRepo(standStorage);

        // --- Default Tenant
        final ActorRequestFactory requestFactory = getRequestFactory();
        final MemoizeEntityUpdateCallback defaultTenantCallback = subscribeToAllOf(stand,
                                                                                   requestFactory,
                                                                                   Customer.class);

        // --- Another Tenant
        final TenantId anotherTenant = newTenantUuid();
        final ActorRequestFactory anotherFactory = createRequestFactory(anotherTenant);
        final MemoizeEntityUpdateCallback anotherCallback = subscribeToAllOf(stand,
                                                                             anotherFactory,
                                                                             Customer.class);

        // Trigger updates in Default Tenant.
        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Version stateVersion = GivenVersion.withNumber(1);
        stand.update(asEnvelope(customerId, customer, stateVersion));

        final Any packedState = AnyPacker.pack(customer);
        // Verify that Default Tenant callback has got the update.
        assertEquals(packedState, defaultTenantCallback.getNewEntityState());

        // And Another Tenant callback has not been called.
        assertEquals(null, anotherCallback.getNewEntityState());
    }

    protected MemoizeEntityUpdateCallback subscribeToAllOf(Stand stand,
                                                           ActorRequestFactory requestFactory,
                                                           Class<? extends Message> entityClass) {
        final Topic allCustomers = requestFactory.topic().allOf(entityClass);
        final MemoizeEntityUpdateCallback callback = new MemoizeEntityUpdateCallback();
        subscribeAndActivate(stand, allCustomers, callback);

        assertNull(callback.getNewEntityState());
        return callback;
    }
}
