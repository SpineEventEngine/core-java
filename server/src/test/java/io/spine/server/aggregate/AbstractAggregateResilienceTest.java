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

import com.google.protobuf.InvalidProtocolBufferException;
import io.spine.base.CommandMessage;
import io.spine.client.ResponseFormat;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.given.aggregate.AggregateTestEnv;
import io.spine.server.aggregate.given.employee.Employee;
import io.spine.server.aggregate.given.employee.EmployeeAgg;
import io.spine.server.aggregate.given.employee.EmployeeId;
import io.spine.server.aggregate.given.employee.DispatchExhaust;
import io.spine.server.aggregate.given.employee.PersonEmployed;
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

import static com.google.common.truth.Truth.assertThat;
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
        repository.aggregateStorage().enableStateQuerying();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Nested
    @DisplayName("not store and post events emitted by a command if one of them was not applied successfully")
    class NotStoreAndPostEventsBundle {

        @Nested
        @DisplayName("when a faulty command produced")
        class WhenCommandEmitted {

            @Test
            @DisplayName("a single event")
            void singleEvent() {
                dispatch(
                        employ(jack, 250),
                        increaseSalary(jack, 15),

                        // This command emits the event that will corrupt the aggregate's state
                        // as no employee can be paid less than 200.
                        //
                        // This event should neither be stored nor posted.
                        decreaseSalary(jack, 500),

                        increaseSalary(jack, 500)
                );

                var exhaust = collectExhaust();
                var expectedEvents = eventTypes(
                        PersonEmployed.class,
                        SalaryIncreased.class,
                        SalaryIncreased.class
                );

                assertEvents(exhaust.storedEvents(), expectedEvents);
                assertEvents(exhaust.postedEvents(), expectedEvents);
                assertThat(exhaust.state().getSalary())
                        .isEqualTo(250 + 15 + 500);
            }

            @Test
            @DisplayName("multiple events")
            void multipleEvents() {
                dispatch(
                        employ(jack, 250),
                        increaseSalary(jack, 200),

                        // This command emits three events. Second one will corrupt
                        // the aggregate's state as no employee can be paid less than 200.
                        //
                        // These events should neither be stored nor posted.
                        decreaseSalaryThreeTimes(jack, 200),

                        increaseSalary(jack, 100)
                );

                var exhaust = collectExhaust();
                var expectedEvents = eventTypes(
                        PersonEmployed.class,
                        SalaryIncreased.class,
                        SalaryIncreased.class
                );

                assertEvents(exhaust.storedEvents(), expectedEvents);
                assertEvents(exhaust.postedEvents(), expectedEvents);
                assertThat(exhaust.state().getSalary())
                        .isEqualTo(250 + 200 + 100);
            }

            private void dispatch(CommandMessage... messages) {
                var commands = Arrays.stream(messages)
                        .map(AggregateTestEnv::command)
                        .collect(Collectors.toList());
                AbstractAggregateResilienceTest.this.dispatch(commands, context);
            }

            private DispatchExhaust collectExhaust() {
                var observer = new MemoizingObserver<Event>();
                context.eventBus()
                       .eventStore()
                       .read(EventStreamQuery.getDefaultInstance(), observer);
                var postedEvents = observer.responses();

                var storedEvents = repository.aggregateStorage()
                                             .read(jack)
                                             .orElseThrow()
                                             .getEventList();

                var rawState = repository.aggregateStorage()
                                         .readStates(ResponseFormat.getDefaultInstance())
                                         .next()
                                         .getState();

                try {
                    var state = rawState.unpack(Employee.class);
                    return new DispatchExhaust(storedEvents, postedEvents, state);

                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalStateException(e);
                }
            }
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
