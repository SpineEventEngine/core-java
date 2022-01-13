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

import io.spine.base.CommandMessage;
import io.spine.core.Event;
import io.spine.environment.Tests;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.given.employee.EmployeeAgg;
import io.spine.server.aggregate.given.salary.EmployeeId;
import io.spine.server.aggregate.given.employee.PreparedInboxStorage;
import io.spine.server.aggregate.given.employee.PreparedStorageFactory;
import io.spine.server.aggregate.given.salary.event.PersonEmployed;
import io.spine.server.aggregate.given.salary.event.SalaryDecreased;
import io.spine.server.aggregate.given.salary.event.SalaryIncreased;
import io.spine.server.delivery.DeliveryStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.server.aggregate.given.AggregateCachingTestEnv.*;
import static io.spine.server.aggregate.given.salary.EmployeeId.*;
import static io.spine.server.aggregate.given.employee.Employees.decreaseSalary;
import static io.spine.server.aggregate.given.employee.Employees.decreaseSalaryThreeTimes;
import static io.spine.server.aggregate.given.employee.Employees.employ;
import static io.spine.server.aggregate.given.employee.Employees.increaseSalary;

/**
 * Tests interaction of `Aggregate` with `UncommittedHistory`.
 *
 * An `Aggregate` is cached when multiple messages are dispatched from `Inbox`. Under the hood,
 * they are processed as a "batch", which triggers the aggregate to be cached for their processing.
 */
@DisplayName("Cached `Aggregate` should")
class AggregateCachingTest {

    private final EmployeeId jack = generate();

    @BeforeEach
    void setUp() {
        ServerEnvironment.instance().reset();
    }

    @AfterEach
    void tearDown() {
        ServerEnvironment.instance().reset();
    }

    @Nested
    @DisplayName("store only successfully applied events when a faulty command emitted")
    class StoreSuccessfulEvents {

        @Test
        @DisplayName("a single event")
        void singleEvent() throws Exception {
            var storedEvents = dispatchInBatch(
                    employ(jack, 250),
                    increaseSalary(jack, 15),

                    // This command emits the event that will corrupt the aggregate's state
                    // as no employee can be paid less than 200.
                    // This event should NOT be stored.
                    decreaseSalary(jack, 500),

                    increaseSalary(jack, 500)
            );
            assertEvents(storedEvents,
                         PersonEmployed.class,
                         SalaryIncreased.class,
                         SalaryIncreased.class
            );
        }

        @Test
        @DisplayName("multiple events")
        void multipleEvents() throws Exception {
            var storedEvents = dispatchInBatch(
                    employ(jack, 250),
                    increaseSalary(jack, 200),

                    // This command emits three events. Second one will corrupt
                    // the aggregate's state as no employee can be paid less than 200.
                    // Only first of them should be stored.
                    decreaseSalaryThreeTimes(jack, 200),

                    increaseSalary(jack, 100)
            );
            assertEvents(storedEvents,
                         PersonEmployed.class,
                         SalaryIncreased.class,
                         SalaryDecreased.class,
                         SalaryIncreased.class
            );
        }

        private List<Event> dispatchInBatch(CommandMessage... commands) throws Exception {
            var shardIndex = DeliveryStrategy.newIndex(0, 1);
            var inboxStorage = PreparedInboxStorage.withCommands(
                    shardIndex,
                    commands
            );

            var serverEnv = ServerEnvironment.instance();
            ServerEnvironment.when(Tests.class).use(PreparedStorageFactory.with(inboxStorage));

            var repository = new DefaultAggregateRepository<>(EmployeeAgg.class);
            try(var ignored = createBcWith(repository)) {
                serverEnv.delivery().deliverMessagesFrom(shardIndex);
                var storedEvents = repository.aggregateStorage()
                                             .read(jack)
                                             .orElseThrow()
                                             .getEventList();
                return storedEvents;
            }
        }
    }
}
