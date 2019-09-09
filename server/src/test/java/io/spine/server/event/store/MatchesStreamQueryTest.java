/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.base.FieldFilter;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStreamQuery;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.server.TestEventFactory.newInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MatchesStreamQuery should")
class MatchesStreamQueryTest {

    private static final String FIELD_NAME = "spine.test.event.ProjectCreated.project_id";

    private static final TestEventFactory eventFactory =
            newInstance(MatchesStreamQueryTest.class);

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        MatchesStreamQuery predicate = new MatchesStreamQuery(
                EventStreamQuery.getDefaultInstance());

        new NullPointerTester()
                .setDefault(Event.class, Event.getDefaultInstance())
                .testAllPublicInstanceMethods(predicate);
    }

    @Test
    @DisplayName("match proper records")
    void matchProperRecords() {
        ProjectId properValue = generate();
        Event event = eventWith(properValue);
        MatchesStreamQuery predicate = queryWith(FIELD_NAME, properValue);
        assertTrue(predicate.test(event));
    }

    @Test
    @DisplayName("not match improper records")
    void notMatchImproperRecords() {
        ProjectId properField = generate();
        ProjectId wrongValue = ProjectId.getDefaultInstance();
        Event event = eventWith(wrongValue);
        MatchesStreamQuery predicate = queryWith(FIELD_NAME, properField);
        assertFalse(predicate.test(event));
    }

    private static MatchesStreamQuery queryWith(String fieldPath, Message field) {
        FieldFilter filter = FieldFilter
                .newBuilder()
                .setFieldPath(fieldPath)
                .addValue(AnyPacker.pack(field))
                .build();
        EventFilter eventFilter = EventFilter
                .newBuilder()
                .addEventFieldFilter(filter)
                .build();
        EventStreamQuery query = EventStreamQuery
                .newBuilder()
                .addFilter(eventFilter)
                .build();
        MatchesStreamQuery predicate = new MatchesStreamQuery(query);
        return predicate;
    }

    private static Event eventWith(ProjectId fieldValue) {
        ProjectCreated eventMsg = eventMessage(fieldValue);
        return eventFactory.createEvent(eventMsg);
    }

    private static ProjectCreated eventMessage(ProjectId value) {
        return ProjectCreated
                .newBuilder()
                .setProjectId(value)
                .build();
    }

    private static ProjectId generate() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

}
