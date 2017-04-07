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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Responses;
import org.spine3.client.Query;
import org.spine3.client.QueryFactory;
import org.spine3.client.QueryResponse;
import org.spine3.test.commandservice.customer.Customer;
import org.spine3.users.TenantId;

import static org.junit.Assert.assertEquals;
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
}
