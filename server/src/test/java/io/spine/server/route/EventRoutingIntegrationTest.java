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

package io.spine.server.route;

import io.spine.core.UserId;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.route.given.user.SessionProjection;
import io.spine.server.route.given.user.SessionRepository;
import io.spine.server.route.given.user.UserAggregate;
import io.spine.server.route.given.user.event.RUserSignedIn;
import io.spine.test.event.RSession;
import io.spine.test.event.RSessionId;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Event routing should")
class EventRoutingIntegrationTest {

    /**
     * A test that verifies that the {@linkplain io.spine.core.Event event} routing occurs at the
     * right moment in time.
     *
     * <p>If the routing of {@code RUserConsentRequested} event is done before its origin
     * ({@code RUserSignedIn}) is dispatched, the repository won't be able to route the event
     * properly, making the corresponding field {@code false}.
     */
    @Test
    @Disabled       // See https://github.com/SpineEventEngine/core-java/issues/925.
    @DisplayName("only occur after the event origin has already been dispatched")
    void occurAfterOriginDispatched() {
        UserId userId = GivenUserId.generated();
        RSessionId sessionId = RSessionId.generate();
        RUserSignedIn event = RUserSignedIn
                .newBuilder()
                .setUserId(userId)
                .setSessionId(sessionId)
                .build();
        RSession session = RSession
                .newBuilder()
                .setId(sessionId)
                .setUserId(userId)
                .setUserConsentRequested(true)
                .build();

        BlackBoxContext context = BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                                     .add(UserAggregate.class)
                                     .add(new SessionRepository())
        );
        context.receivesEvent(event);

        context.assertEntity(sessionId, SessionProjection.class)
               .hasStateThat()
               .comparingExpectedFieldsOnly()
               .isEqualTo(session);
    }
}
