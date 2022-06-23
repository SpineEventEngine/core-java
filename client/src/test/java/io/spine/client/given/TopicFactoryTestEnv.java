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

package io.spine.client.given;

import io.spine.core.ActorContext;
import io.spine.test.client.TestEntity;
import io.spine.test.client.TestEntityId;
import io.spine.time.TimestampTemporal;
import io.spine.type.TypeUrl;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

public class TopicFactoryTestEnv {

    // See {@code client_requests.proto} for declaration.
    public static final Class<TestEntity> TEST_ENTITY_TYPE = TestEntity.class;
    public static final TypeUrl TARGET_ENTITY_TYPE_URL = TypeUrl.of(TEST_ENTITY_TYPE);

    /** Prevents instantiation of this test environment class. */
    private TopicFactoryTestEnv() {
    }

    public static TestEntityId entityId(int idValue) {
        return TestEntityId.newBuilder()
                           .setValue(idValue)
                           .build();
    }

    public static void verifyContext(ActorContext expected, ActorContext actual) {
        assertThat(actual).ignoringFields(3 /* timestamp */)
                          .isEqualTo(expected);

        // It's impossible to get the same creation time for the `expected` value,
        //    so checking that the `actual` value is not later than `expected`.
        Comparable<TimestampTemporal> actualTemporal = TimestampTemporal.from(actual.getTimestamp());
        TimestampTemporal expectedTimestamp = TimestampTemporal.from(expected.getTimestamp());
        assertThat(actualTemporal).isAtMost(expectedTimestamp);
    }
}
