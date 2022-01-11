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
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.environment.Tests;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.given.salary.Employee;
import io.spine.server.aggregate.given.salary.EmployeeAgg;
import io.spine.server.aggregate.given.salary.EmployeeId;
import io.spine.server.aggregate.given.salary.PreparedInboxStorage;
import io.spine.server.aggregate.given.salary.PreparedStorageFactory;
import io.spine.server.aggregate.given.salary.event.NewEmployed;
import io.spine.server.aggregate.given.salary.event.SalaryDecreased;
import io.spine.server.aggregate.given.salary.event.SalaryIncreased;
import io.spine.server.delivery.DeliveryStrategy;
import io.spine.server.entity.Repository;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.aggregate.given.salary.Employees.decreaseSalary;
import static io.spine.server.aggregate.given.salary.Employees.decreaseSalaryThreeTimes;
import static io.spine.server.aggregate.given.salary.Employees.employ;
import static io.spine.server.aggregate.given.salary.Employees.increaseSalary;
import static io.spine.server.aggregate.given.salary.Employees.newEmployee;

/**
 * Tests a cached `Aggregate`.
 *
 * <p>An `Aggregate` is cached when multiple messages are dispatched from `Inbox`. Under the hood,
 * they are processed as a "batch", which triggers the aggregate to be cached for their processing.
 */
@DisplayName("Cached `Aggregate` should")
class AggregateCachingTest {

    @Nested
    @DisplayName("store only successfully applied events when a command assignee emitted")
    class StoreSuccessfulEvents {

        @BeforeEach
        void setUp() {
            ServerEnvironment.instance().reset();
        }

        @AfterEach
        void tearDown() {
            ServerEnvironment.instance().reset();
        }

        @Test
        @DisplayName("a single event")
        void singleEvent() throws Exception {
            var jack = newEmployee();
            var storedEvents = dispatchInBatch(
                    jack,
                    employ(jack, 250),
                    decreaseSalary(jack, 15),

                    // This command emits the event that will corrupt the aggregate's state
                    // as no employee can be paid less than 200.
                    decreaseSalary(jack, 500),

                    increaseSalary(jack, 500)
            );

            assertThat(storedEvents.size()).isEqualTo(3);
            assertEvent(storedEvents.get(0), NewEmployed.class);
            assertEvent(storedEvents.get(1), SalaryDecreased.class);
            assertEvent(storedEvents.get(2), SalaryIncreased.class);
        }

        @Test
        @DisplayName("multiple events")
        void multipleEvents() throws Exception {
            var jack = newEmployee();
            var storedEvents = dispatchInBatch(
                    jack,
                    employ(jack, 250),
                    increaseSalary(jack, 200),

                    // This command emits three events. Second one will corrupt
                    // the aggregate's state as no employee can be paid less than 200.
                    decreaseSalaryThreeTimes(jack, 200),

                    increaseSalary(jack, 100)
            );

            assertThat(storedEvents.size()).isEqualTo(3);
            assertEvent(storedEvents.get(0), NewEmployed.class);
            assertEvent(storedEvents.get(1), SalaryIncreased.class);
            assertEvent(storedEvents.get(2), SalaryIncreased.class);
        }

        /**
         * Returns a list of events which were actually put into a storage
         * as a result of commands dispatching.
         */
        private List<Event> dispatchInBatch(EmployeeId entityId, CommandMessage... commands) throws Exception {
            var shardIndex = DeliveryStrategy.newIndex(0, 1);
            var inboxStorage = PreparedInboxStorage.withCommands(
                    shardIndex,
                    TypeUrl.of(Employee.class),
                    commands
            );

            var serverEnv = ServerEnvironment.instance();
            ServerEnvironment.when(Tests.class).use(PreparedStorageFactory.with(inboxStorage));

            var repository = new DefaultAggregateRepository<>(EmployeeAgg.class);
            try(var ignored = createBcWith(repository)) {
                serverEnv.delivery().deliverMessagesFrom(shardIndex);
                var storedEvents = repository.aggregateStorage()
                                             .read(entityId)
                                             .orElseThrow()
                                             .getEventList();
                return storedEvents;
            }
        }

        private BoundedContext createBcWith(Repository<?, ?> repository) {
            return BoundedContextBuilder.assumingTests()
                                        .add(repository)
                                        .build();
        }

        private void assertEvent(Event event, Class<? extends EventMessage> type) {
            assertThat(event.enclosedMessage().getClass())
                    .isEqualTo(type);
        }
    }
}
