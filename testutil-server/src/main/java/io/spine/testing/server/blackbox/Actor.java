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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.time.ZoneId;
import io.spine.time.ZoneOffset;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;
import static io.spine.validate.Validate.checkValid;

/**
 * A factory of test actor request factories.
 */
@VisibleForTesting
final class Actor {

    private static final Actor defaultActor = from(BlackBoxBoundedContext.class.getName());

    private final UserId id;
    private final ZoneId zoneId;
    private final ZoneOffset zoneOffset;

    private Actor(UserId id, ZoneId zoneId, ZoneOffset zoneOffset) {
        this.id = id;
        this.zoneId = zoneId;
        this.zoneOffset = zoneOffset;
    }

    /**
     * Obtains the default actor.
     */
    static Actor defaultActor() {
        return defaultActor;
    }

    private static Actor from(String userId) {
        checkNotEmptyOrBlank(userId);
        UserId id = UserId
                .newBuilder()
                .setValue(userId)
                .build();
        return from(id);
    }

    /**
     * Creates a new actor with the given actor ID and the default time zone.
     */
    static Actor from(UserId userId) {
        checkNotNull(userId);
        checkValid(userId);
        return new Actor(userId,
                         ZoneId.getDefaultInstance(),
                         ZoneOffset.getDefaultInstance());
    }

    /**
     * Creates a new actor with the given time zone and the default actor ID.
     */
    static Actor from(ZoneId zoneId, ZoneOffset zoneOffset) {
        checkNotNull(zoneId);
        checkNotNull(zoneOffset);
        return new Actor(defaultActor.id, zoneId, zoneOffset);
    }

    /**
     * Creates a new actor with the given actor ID and time zone.
     */
    static Actor from(UserId userId, ZoneId zoneId, ZoneOffset zoneOffset) {
        checkNotNull(userId);
        checkNotNull(zoneId);
        checkNotNull(zoneOffset);
        return new Actor(userId, zoneId, zoneOffset);
    }

    /**
     * Creates a new factory for requests of the single tenant.
     */
    TestActorRequestFactory requests() {
        return new TestActorRequestFactory(null, id, zoneOffset, zoneId);
    }

    /**
     * Creates a new factory for requests of the given tenant.
     */
    TestActorRequestFactory requestsFor(TenantId tenant) {
        checkNotNull(tenant);
        checkValid(tenant);
        return new TestActorRequestFactory(tenant, id, zoneOffset, zoneId);
    }
}
