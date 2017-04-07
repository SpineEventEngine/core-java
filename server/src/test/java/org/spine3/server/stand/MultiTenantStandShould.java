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

package org.spine3.server.stand;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Responses;
import org.spine3.base.Version;
import org.spine3.client.Query;
import org.spine3.client.QueryFactory;
import org.spine3.client.QueryResponse;
import org.spine3.client.Subscription;
import org.spine3.client.Topic;
import org.spine3.client.TopicFactory;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.Tests;
import org.spine3.test.commandservice.customer.Customer;
import org.spine3.test.commandservice.customer.CustomerId;
import org.spine3.users.TenantId;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.newTenantUuid;

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
        setQueryFactory(createQueryFactory(tenantId));
        setTopicFactory(createTopicFactory(tenantId));
    }

    @After
    public void tearDown() {
        clearCurrentTenant();
    }

    @Test
    public void not_allow_reading_aggregate_records_for_another_tenant() {
        final Stand stand = doCheckReadingCustomersById(15);

        final TenantId anotherTenant = newTenantUuid();
        final QueryFactory queryFactory = createQueryFactory(anotherTenant);

        final Query readAllCustomers = queryFactory.readAll(Customer.class);

        final MemoizeQueryResponseObserver responseObserver = new MemoizeQueryResponseObserver();
        stand.execute(readAllCustomers, responseObserver);
        final QueryResponse response = responseObserver.getResponseHandled();
        assertTrue(Responses.isOk(response.getResponse()));
        assertEquals(0, response.getMessagesCount());
    }

    @Test
    public void not_trigger_updates_of_aggregate_records_for_another_tenant_subscriptions() {
        final StandStorage standStorage = InMemoryStorageFactory.getInstance(isMultitenant())
                                                                .createStandStorage();
        final Stand stand = prepareStandWithAggregateRepo(standStorage);

        // --- Default Tenant
        final TopicFactory topicFactory = getTopicFactory();
        final MemoizeEntityUpdateCallback defaultTenantCallback = subscribeToAllOf(stand,
                                                                                   topicFactory,
                                                                                   Customer.class);

        // --- Another Tenant
        final TenantId anotherTenant = newTenantUuid();
        final TopicFactory anotherFactory = createTopicFactory(anotherTenant);
        final MemoizeEntityUpdateCallback anotherCallback = subscribeToAllOf(stand,
                                                                             anotherFactory,
                                                                             Customer.class);

        // Trigger updates in Default Tenant.
        final Map.Entry<CustomerId, Customer> sampleData = fillSampleCustomers(1).entrySet()
                                                                                 .iterator()
                                                                                 .next();
        final CustomerId customerId = sampleData.getKey();
        final Customer customer = sampleData.getValue();
        final Any packedState = AnyPacker.pack(customer);
        final Version stateVersion = Tests.newVersionWithNumber(1);
        stand.update(customerId, packedState, stateVersion);

        // Verify that Default Tenant callback has got the update.
        assertEquals(packedState, defaultTenantCallback.getNewEntityState());

        // And Another Tenant callback has not been called.
        assertEquals(null, anotherCallback.getNewEntityState());
    }

    protected MemoizeEntityUpdateCallback subscribeToAllOf(Stand stand, TopicFactory topicFactory,
                                                           Class<? extends Message> entityClass) {
        final Topic allCustomers = topicFactory.allOf(entityClass);
        final MemoizeEntityUpdateCallback memoizeCallback = new MemoizeEntityUpdateCallback();
        final Subscription subscription = stand.subscribe(allCustomers);
        stand.activate(subscription, memoizeCallback);
        assertNotNull(subscription);
        assertNull(memoizeCallback.getNewEntityState());
        return memoizeCallback;
    }
}
