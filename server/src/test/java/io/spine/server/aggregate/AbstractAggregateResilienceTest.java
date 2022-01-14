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
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.given.aggregate.AggregateTestEnv;
import io.spine.server.aggregate.given.employee.Employee;
import io.spine.server.aggregate.given.employee.EmployeeAgg;
import io.spine.server.aggregate.given.employee.EmployeeId;
import io.spine.server.aggregate.given.employee.ResultedEvents;
import io.spine.server.aggregate.given.employee.PersonEmployed;
import io.spine.server.aggregate.given.employee.SalaryDecreased;
import io.spine.server.aggregate.given.employee.SalaryIncreased;
import io.spine.server.event.EventStreamQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.spine.server.aggregate.given.AbstractAggregateResilienceTestEnv.assertEvents;
import static io.spine.server.aggregate.given.AbstractAggregateResilienceTestEnv.eventTypes;
import static io.spine.server.aggregate.given.employee.EmployeeId.generate;
import static io.spine.server.aggregate.given.employee.Employees.decreaseSalary;
import static io.spine.server.aggregate.given.employee.Employees.decreaseSalaryThreeTimes;
import static io.spine.server.aggregate.given.employee.Employees.employ;
import static io.spine.server.aggregate.given.employee.Employees.increaseSalary;

/**
 * Tests how {@code Aggregate} handles the case when one of events, emitted by a command,
 * corrupts the {@code Aggregate}'s state.
 *
 * @see AggregateResilienceTest
 * @see CachedAggregateResilienceTest
 */
@DisplayName("`Aggregate` should")
abstract class AbstractAggregateResilienceTest {

    private final EmployeeId jack = generate();
    private AggregateRepository<EmployeeId, EmployeeAgg, Employee> repository;
    private BoundedContext context;

    @BeforeEach
    void setUp() {
        repository = new DefaultAggregateRepository<>(EmployeeAgg.class);
        context = BoundedContextBuilder.assumingTests()
                                       .add(repository)
                                       .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Nested
    @DisplayName("store and post only successfully applied events when a faulty command emits")
    class StoreAndPostOnlySuccessfulEvents {

        @Test
        @DisplayName("a single event")
        void singleEvent() {
            var resultedEvents = dispatch(
                    employ(jack, 250),
                    increaseSalary(jack, 15),

                    // This command emits the event that will corrupt the aggregate's state
                    // as no employee can be paid less than 200.
                    // This event should neither be stored nor posted.
                    decreaseSalary(jack, 500),

                    increaseSalary(jack, 500)
            );
            var expected = eventTypes(
                    PersonEmployed.class,
                    SalaryIncreased.class,
                    SalaryIncreased.class
            );
            assertEvents(resultedEvents.stored(), expected);
            assertEvents(resultedEvents.posted(), expected);
        }

        @Test
        @DisplayName("multiple events")
        void multipleEvents() {
            var resultedEvents = dispatch(
                    employ(jack, 250),
                    increaseSalary(jack, 200),

                    // This command emits three events. Second one will corrupt
                    // the aggregate's state as no employee can be paid less than 200.
                    // Only first of them should be stored and posted.
                    decreaseSalaryThreeTimes(jack, 200),

                    increaseSalary(jack, 100)
            );
            var expected = eventTypes(
                    PersonEmployed.class,
                    SalaryIncreased.class,
                    SalaryDecreased.class,
                    SalaryIncreased.class
            );
            assertEvents(resultedEvents.stored(), expected);
            assertEvents(resultedEvents.posted(), expected);
        }

        private ResultedEvents dispatch(CommandMessage... messages) {
            var commands = Arrays.stream(messages)
                    .map(AggregateTestEnv::command)
                    .collect(Collectors.toList());

            AbstractAggregateResilienceTest.this.dispatch(commands, context);

            var observer = new MemoizingObserver<Event>();
            context.eventBus()
                   .eventStore()
                   .read(EventStreamQuery.getDefaultInstance(), observer);

            var posted = observer.responses();
            var stored = repository.aggregateStorage()
                                         .read(jack)
                                         .orElseThrow()
                                         .getEventList();

            return new ResultedEvents(stored, posted);
        }
    }

    /**
     * Dispatches the passed commands to the passed context.
     *
     * <p>The way we dispatch commands determines whether
     * an {@code Aggregate} would be cached or not.
     */
    abstract void dispatch(List<Command> commands, BoundedContext context);
}
