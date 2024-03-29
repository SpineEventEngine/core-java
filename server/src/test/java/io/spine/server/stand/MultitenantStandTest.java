/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.base.EntityState;
import io.spine.client.ActorRequestFactory;
import io.spine.protobuf.AnyPacker;
import io.spine.server.Given.CustomerAggregate;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.stand.given.StandTestEnv.MemoizeSubscriptionCallback;
import io.spine.test.commandservice.customer.Customer;
import io.spine.testing.core.given.GivenTenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.entity.given.Given.aggregateOfClass;
import static io.spine.server.stand.given.StandTestEnv.createRequestFactory;
import static io.spine.server.stand.given.StandTestEnv.einCustomer;
import static io.spine.server.stand.given.StandTestEnv.newStand;
import static io.spine.server.stand.given.StandTestEnv.subscribeAndActivate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Multitenant `Stand` should")
class MultitenantStandTest extends StandTest {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        var tenantId = GivenTenantId.generate();

        setCurrentTenant(tenantId);
        setMultitenant(true);
        setRequestFactory(createRequestFactory(tenantId));
    }

    @AfterEach
    void tearDown() {
        clearCurrentTenant();
    }

    @Test
    @DisplayName("not trigger updates of aggregate records for another tenant subscriptions")
    void updateOnlySameTenant() {
        var repository = new CustomerAggregateRepository();
        var stand = newStand(isMultitenant(), repository);

        // --- Default Tenant
        var requestFactory = getRequestFactory();
        var defaultTenantCallback =
                subscribeToAllOf(stand, requestFactory, Customer.class);

        // --- Another Tenant
        var anotherTenant = GivenTenantId.generate();
        var anotherFactory = createRequestFactory(anotherTenant);
        var anotherCallback =
                subscribeToAllOf(stand, anotherFactory, Customer.class);

        // Trigger updates in Default Tenant.
        var customer = einCustomer();
        var customerId = customer.getId();
        var version = 1;
        var entity = aggregateOfClass(CustomerAggregate.class)
                .withId(customerId)
                .withState(customer)
                .withVersion(version)
                .build();
        stand.post(entity, repository.lifecycleOf(customerId));

        var packedState = AnyPacker.pack(customer);
        // Verify that Default Tenant callback has got the update.
        assertEquals(packedState, defaultTenantCallback.newEntityState());

        // And Another Tenant callback has not been called.
        assertNull(anotherCallback.newEntityState());
    }

    MemoizeSubscriptionCallback
    subscribeToAllOf(Stand stand,
                     ActorRequestFactory requestFactory,
                     Class<? extends EntityState<?>> stateClass) {
        var allCustomers = requestFactory.topic()
                                         .allOf(stateClass);
        var action = new MemoizeSubscriptionCallback();
        subscribeAndActivate(stand, allCustomers, action);

        assertNull(action.newEntityState());
        return action;
    }
}
