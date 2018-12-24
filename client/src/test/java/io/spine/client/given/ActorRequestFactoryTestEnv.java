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

package io.spine.client.given;

import io.spine.client.ActorRequestFactory;
import io.spine.core.UserId;
import io.spine.testing.core.given.GivenUserId;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;

import static io.spine.base.Identifier.newUuid;

public class ActorRequestFactoryTestEnv {

    public static final UserId ACTOR = GivenUserId.of(newUuid());
    public static final ZoneOffset ZONE_OFFSET = ZoneOffsets.getDefault();
    public static final ZoneId ZONE_ID = ZoneIds.systemDefault();

    /** Prevents instantiation of this test environment class. */
    private ActorRequestFactoryTestEnv() {
    }

    public static ActorRequestFactory.Builder requestFactoryBuilder() {
        return ActorRequestFactory.newBuilder();
    }

    public static ActorRequestFactory requestFactory() {
        return requestFactoryBuilder().setZoneOffset(ZONE_OFFSET)
                                      .setZoneId(ZONE_ID)
                                      .setActor(ACTOR)
                                      .build();
    }
}
