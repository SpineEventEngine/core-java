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

package io.spine.server.aggregate;

import io.spine.environment.Tests;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.given.salary.Employee;
import io.spine.server.aggregate.given.salary.EmployeeAgg;
import io.spine.server.aggregate.given.salary.PreparedInboxStorage;
import io.spine.server.aggregate.given.salary.PreparedStorageFactory;
import io.spine.server.delivery.DeliveryStrategy;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.command;
import static io.spine.server.aggregate.given.salary.Employees.decreaseSalary;
import static io.spine.server.aggregate.given.salary.Employees.employ;
import static io.spine.server.aggregate.given.salary.Employees.increaseSalary;
import static io.spine.server.aggregate.given.salary.Employees.newEmployee;

@DisplayName("Cached `Aggregate` should")
class AggregateCachingTest {

    @Test
    @DisplayName("store only successfully applied events")
    void storeEventsOnlyIfApplied() {
        var jack = newEmployee();
        var shardIndex = DeliveryStrategy.newIndex(0, 1);
        var inboxStorage = PreparedInboxStorage.withCommands(
                shardIndex,
                TypeUrl.of(Employee.class),
                command(employ(jack, 250)),
                command(decreaseSalary(jack, 15)),

                // this one will fail the aggregate's state
                // as no employee can be paid less than 200.
                command(decreaseSalary(jack, 500)),

                command(increaseSalary(jack, 500))
        );

        var serverEnv = ServerEnvironment.instance();
        serverEnv.reset();
        ServerEnvironment.when(Tests.class).use(PreparedStorageFactory.with(inboxStorage));

        var repository = new DefaultAggregateRepository<>(EmployeeAgg.class);
        BoundedContextBuilder.assumingTests()
                             .add(repository)
                             .build();

        serverEnv.delivery().deliverMessagesFrom(shardIndex);
        serverEnv.reset();

        var storedEvents = repository.aggregateStorage()
                                     .read(jack)
                                     .orElseThrow()
                                     .getEventList();
        assertThat(storedEvents.size()).isEqualTo(3);
    }
}
