/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.event;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.base.FieldFilter;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.command.TestEventFactory;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.server.command.TestEventFactory.newInstance;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("MatchesStreamQuery should")
class MatchesStreamQueryTest {

    private static final String FIELD_NAME = "spine.test.event.ProjectCreated.projectId";

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
        ProjectId properField = ProjectId.newBuilder()
                                               .setId(newUuid())
                                               .build();
        ProjectCreated eventMsg = ProjectCreated.newBuilder()
                                                      .setProjectId(properField)
                                                      .build();
        Event event = eventFactory.createEvent(eventMsg);
        MatchesStreamQuery predicate = eventWith(FIELD_NAME, properField);
        assertTrue(predicate.apply(event));
    }

    @Test
    @DisplayName("not match improper records")
    void notMatchImproperRecords() {
        ProjectId properField = ProjectId.newBuilder()
                                               .setId(newUuid())
                                               .build();
        ProjectId improperField = ProjectId.getDefaultInstance();
        ProjectCreated eventMsg = ProjectCreated.newBuilder()
                                                      .setProjectId(improperField)
                                                      .build();
        Event event = eventFactory.createEvent(eventMsg);
        MatchesStreamQuery predicate = eventWith(FIELD_NAME, properField);
        assertFalse(predicate.apply(event));
    }

    private static MatchesStreamQuery eventWith(String fieldPath, Message field) {
        FieldFilter filter = FieldFilter.newBuilder()
                                              .setFieldPath(fieldPath)
                                              .addValue(AnyPacker.pack(field))
                                              .build();
        EventFilter eventFilter = EventFilter.newBuilder()
                                                   .addEventFieldFilter(filter)
                                                   .build();
        EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .addFilter(eventFilter)
                                                       .build();
        MatchesStreamQuery predicate = new MatchesStreamQuery(query);
        return predicate;
    }

}
