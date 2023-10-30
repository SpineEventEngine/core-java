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

package io.spine.server.storage.memory;

import io.spine.server.storage.RecordWithColumns;
import io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv;
import io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.StgProjectColumns;
import io.spine.test.storage.StgProjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.storage.given.GivenStorageProject.newState;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.newBuilder;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.recordSubject;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.projectSpec;
import static io.spine.testdata.Sample.messageOfType;
import static io.spine.testing.TestValues.nullRef;

@DisplayName("`RecordQueryMatcher` should")
class RecordQueryMatcherTest {

    @Test
    @DisplayName("match everything except `null` to empty query")
    void matchEverythingToEmpty() {
        var sampleSubject = recordSubject();
        var matcher = new RecordQueryMatcher<>(sampleSubject);

        assertThat(matcher.test(nullRef()))
                .isFalse();
        assertThat(matcher.test(RecordWithColumns.create(newState(), projectSpec())))
                .isTrue();
    }

    @Test
    @DisplayName("match IDs")
    void matchIds() {
        var matchingId = messageOfType(StgProjectId.class);
        var nonMatchingId = messageOfType(StgProjectId.class);
        var subject = recordSubject(matchingId);

        var matcher = new RecordQueryMatcher<>(subject);
        var matching = newState(matchingId);
        var nonMatching = newState(nonMatchingId);
        var matchingRecord = RecordWithColumns.create(matching, projectSpec());
        var nonMatchingRecord = RecordWithColumns.create(nonMatching, projectSpec());
        assertThat(matcher.test(matchingRecord))
                .isTrue();
        assertThat(matcher.test(nonMatchingRecord))
                .isFalse();
    }

    @Test
    @DisplayName("match columns")
    void matchColumns() {
        var matchingState = newState();
        var matchingName = matchingState.getName();
        var query = newBuilder().where(StgProjectColumns.name).is(matchingName).build();
        var matcher = new RecordQueryMatcher<>(query.subject());
        var matchingRecord = RecordWithColumns.create(matchingState, projectSpec());
        var nonMatching = newState();
        var nonMatchingRecord = RecordWithColumns.create(nonMatching, projectSpec());

        assertThat(matcher.test(matchingRecord))
                .isTrue();
        assertThat(matcher.test(nonMatchingRecord))
                .isFalse();
    }

    @Test
    @DisplayName("match `Any` instances")
    void matchAnyInstances() {
        var matchingState = newState();
        var queryValue = pack(matchingState);
        var matchingRecord = RecordWithColumns.create(matchingState, projectSpec());
        var nonMatchingRecord = RecordWithColumns.create(newState(), projectSpec());

        var query = newBuilder()
                .where(StgProjectColumns.state_as_any).is(queryValue).build();
        var matcher = new RecordQueryMatcher<>(query);
        assertThat(matcher.test(matchingRecord))
                .isTrue();
        assertThat(matcher.test(nonMatchingRecord))
                .isFalse();
    }

    @Test
    @DisplayName("not match by wrong field name")
    void notMatchByWrongField() {
        var query = newBuilder()
                .where(StgProjectColumns.random_non_stored_column).is("whatever")
                .build();
        var matcher = new RecordQueryMatcher<>(query);

        var recordWithColumns = RecordWithColumns.create(newState(), projectSpec());
        assertThat(matcher.test(recordWithColumns))
                .isFalse();
    }
}
