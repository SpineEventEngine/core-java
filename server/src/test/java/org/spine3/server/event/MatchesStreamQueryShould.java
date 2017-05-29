/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.FieldFilter;
import org.spine3.base.Identifiers;
import org.spine3.protobuf.AnyPacker;
import org.spine3.test.TestEventFactory;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.ProjectId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.TestEventFactory.newInstance;

/**
 * @author Dmytro Dashenkov
 */
public class MatchesStreamQueryShould {

    private static final String FIELD_NAME = "spine.test.event.ProjectCreated.projectId";

    private static final TestEventFactory eventFactory =
            newInstance(MatchesStreamQueryShould.class);

    @Test
    public void not_accept_nulls() {
        final MatchesStreamQuery predicate = new MatchesStreamQuery(
                EventStreamQuery.getDefaultInstance());

        new NullPointerTester()
                .setDefault(Event.class, Event.getDefaultInstance())
                .testAllPublicInstanceMethods(predicate);
    }

    @Test
    public void match_proper_records() {
        final ProjectId properField = ProjectId.newBuilder()
                                               .setId(Identifiers.newUuid())
                                               .build();
        final ProjectCreated eventMsg = ProjectCreated.newBuilder()
                                                      .setProjectId(properField)
                                                      .build();
        final Event event = eventFactory.createEvent(eventMsg);
        final MatchesStreamQuery predicate = eventWith(FIELD_NAME, properField);
        assertTrue(predicate.apply(event));
    }

    @Test
    public void not_match_improper_records() {
        final ProjectId properField = ProjectId.newBuilder()
                                               .setId(Identifiers.newUuid())
                                               .build();
        final ProjectId improperField = ProjectId.getDefaultInstance();
        final ProjectCreated eventMsg = ProjectCreated.newBuilder()
                                                      .setProjectId(improperField)
                                                      .build();
        final Event event = eventFactory.createEvent(eventMsg);
        final MatchesStreamQuery predicate = eventWith(FIELD_NAME, properField);
        assertFalse(predicate.apply(event));
    }

    private static MatchesStreamQuery eventWith(String fieldPath, Message field) {
        final FieldFilter filter = FieldFilter.newBuilder()
                                              .setFieldPath(fieldPath)
                                              .addValue(AnyPacker.pack(field))
                                              .build();
        final EventFilter eventFilter = EventFilter.newBuilder()
                                                   .addEventFieldFilter(filter)
                                                   .build();
        final EventStreamQuery query = EventStreamQuery.newBuilder()
                                                       .addFilter(eventFilter)
                                                       .build();
        final MatchesStreamQuery predicate = new MatchesStreamQuery(query);
        return predicate;
    }

}
