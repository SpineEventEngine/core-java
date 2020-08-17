/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.util.Timestamps;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.query.QueryPredicate;
import io.spine.query.RecordQuery;
import io.spine.query.Subject;
import io.spine.query.SubjectParameter;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.query.LogicalOperator.AND;
import static io.spine.query.LogicalOperator.OR;
import static io.spine.server.event.store.Queries.convert;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;

@DisplayName("`Queries` should")
class QueriesTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Queries.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testStaticMethods(Queries.class, PACKAGE);
    }

    @Test
    @DisplayName("convert an empty query to an empty `RecordQuery`")
    void convertEmptyToFilters() {
        EventStreamQuery query = EventStreamQuery.newBuilder()
                                                 .build();
        RecordQuery<EventId, Event> result = convert(query);
        Subject<EventId, Event> subject = result.subject();
        assertThat(subject.id()
                          .values()).isEmpty();
        assertThat(subject.predicates()).isEmpty();
    }

    @Test
    @DisplayName("convert the time-constrained query to the corresponding `RecordQuery`")
    void convertTimeToFilters() {
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .setAfter(Timestamps.MIN_VALUE)
                .setBefore(Timestamps.MAX_VALUE)
                .build();
        RecordQuery<EventId, Event> result = convert(query);
        Subject<EventId, Event> subject = result.subject();
        assertThat(subject.predicates()).hasSize(1);

        QueryPredicate<Event> predicate = subject.predicates()
                                                 .get(0);
        assertThat(predicate.operator()).isEqualTo(AND);
        assertThat(predicate.parameters()).hasSize(2);
    }

    @Test
    @DisplayName("convert the event-type query to the corresponding `RecordQuery`")
    void convertTypeToFilters() {
        String expected = " com.example.EventType ";
        EventFilter validFilter = filterForType(expected);
        EventFilter invalidFilter = filterForType("   ");
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .addFilter(validFilter)
                .addFilter(invalidFilter)
                .build();
        RecordQuery<EventId, Event> result = convert(query);
        Subject<EventId, Event> subject = result.subject();
        assertThat(subject.predicates()).hasSize(1);
        QueryPredicate<Event> predicate = subject.predicates()
                                                 .get(0);
        assertThat(predicate.operator()).isEqualTo(OR);

        ImmutableList<SubjectParameter<Event, ?, ?>> parameters = predicate.parameters();
        assertThat(parameters).hasSize(1);

        SubjectParameter<Event, ?, ?> parameter = parameters.get(0);
        Object value = parameter.value();
        assertThat(value).isEqualTo(expected.trim());
    }

    private static EventFilter filterForType(String typeName) {
        return EventFilter.newBuilder()
                          .setEventType(typeName)
                          .build();
    }
}
