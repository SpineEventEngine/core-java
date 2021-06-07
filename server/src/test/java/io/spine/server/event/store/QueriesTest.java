/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.event.store;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.Timestamps;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.query.QueryPredicate;
import io.spine.query.RecordQuery;
import io.spine.query.Subject;
import io.spine.query.SubjectParameter;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.query.LogicalOperator.AND;
import static io.spine.query.LogicalOperator.OR;
import static io.spine.server.event.store.Queries.convert;

@DisplayName("`Queries` should")
class QueriesTest extends UtilityClassTest<Queries> {

    QueriesTest() {
        super(Queries.class, PACKAGE);
    }

    @Test
    @DisplayName("convert an empty query to an empty `RecordQuery`")
    void convertEmptyQuery() {
        EventStreamQuery query = EventStreamQuery.newBuilder()
                                                 .build();
        RecordQuery<EventId, Event> result = convert(query);
        Subject<EventId, Event> subject = result.subject();
        assertThat(subject.id()
                          .values()).isEmpty();
        QueryPredicate<Event> rootPredicate = subject.predicate();
        assertThat(rootPredicate.allParams()).isEmpty();
        assertThat(rootPredicate.children()).isEmpty();
    }

    @Test
    @DisplayName("convert the time-constrained query to the corresponding `RecordQuery`")
    void convertTimeConstrainedQuery() {
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .setAfter(Timestamps.MIN_VALUE)
                .setBefore(Timestamps.MAX_VALUE)
                .build();
        RecordQuery<EventId, Event> result = convert(query);
        Subject<EventId, Event> subject = result.subject();
        QueryPredicate<Event> rootPredicate = subject.predicate();
        assertThat(rootPredicate.children()).isEmpty();

        assertThat(rootPredicate.operator()).isEqualTo(AND);
        assertThat(rootPredicate.parameters()).hasSize(2);
    }

    @Test
    @DisplayName("convert the event-type query to the corresponding `RecordQuery`")
    void convertEventTypeConstrainedQuery() {
        String somethingHappened = " com.acme.SomethingHappened ";
        String somethingElseHappened = "com.acme.SomethingElseHappened";
        EventFilter firstFilter = filterForType(somethingHappened);
        EventFilter secondFilter = filterForType(somethingElseHappened);
        EventFilter invalidFilter = filterForType("   ");
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .addFilter(firstFilter)
                .addFilter(secondFilter)
                .addFilter(invalidFilter)
                .build();
        RecordQuery<EventId, Event> result = convert(query);

        Subject<EventId, Event> subject = result.subject();
        QueryPredicate<Event> root = subject.predicate();
        assertThat(root.operator()).isEqualTo(OR);

        ImmutableList<SubjectParameter<?, ?, ?>> params = root.allParams();
        assertParamValue(params, 0, somethingHappened.trim());
        assertParamValue(params, 1, somethingElseHappened);
    }

    private static void
    assertParamValue(ImmutableList<SubjectParameter<?, ?, ?>> params, int index, String expected) {
        SubjectParameter<?, ?, ?> parameter = params.get(index);
        assertThat(parameter.value())
                .isEqualTo(expected);
    }

    private static EventFilter filterForType(String typeName) {
        return EventFilter.newBuilder()
                          .setEventType(typeName)
                          .build();
    }
}
