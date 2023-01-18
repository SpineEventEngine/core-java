/*
 * Copyright 2023, TeamDev. All rights reserved.
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
package io.spine.server.route

import io.spine.server.given.context.users.RSessionId
import io.spine.server.given.context.users.SessionProjection
import io.spine.server.given.context.users.createUsersContext
import io.spine.server.given.context.users.event.rUserSignedIn
import io.spine.server.given.context.users.rSession
import io.spine.server.testing.blackbox.assertEntity
import io.spine.testing.core.given.GivenUserId
import io.spine.testing.server.blackbox.BlackBox
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest

@DisplayName("Event routing for a reaction event should")
internal class EventReactionRoutingSpec {

    /**
     * Verifies that the routing of the event generated as a reaction on another event
     * occurs after the origin event is routed.
     *
     * If the routing of `RUserConsentRequested` event is done before its origin
     * (`RUserSignedIn`) is dispatched, the repository won't be able to route the event
     * properly, making the corresponding field `false`.
     *
     * The test is made `@RepeatedTest` to increase the changes of failure because
     * historically it failed sometimes. See the GitHub issue link next to commented
     * out `@Disabled` annotation for details.
     */
    @RepeatedTest(50) //@Test
    @Disabled // See https://github.com/SpineEventEngine/core-java/issues/925.
    @DisplayName("only occur after the origin event has been already dispatched")
    fun occurAfterOriginDispatched() {
        val userId = GivenUserId.generated()
        val sessionId = RSessionId.generate()

        BlackBox.from(createUsersContext()).use { context ->

            context.receivesEvent(rUserSignedIn {
                user = userId
                session = sessionId
            })

            val expected = rSession {
                id = sessionId
                this@rSession.userId = userId
                // Check that the event `RUserConsentRequested` was dispatched to
                // the corresponding `SessionProjection`.
                userConsentRequested = true
            }

            context.assertEntity<SessionProjection, _>(sessionId)
                .hasStateThat()
                .comparingExpectedFieldsOnly()
                .isEqualTo(expected)
        }
    }
}
