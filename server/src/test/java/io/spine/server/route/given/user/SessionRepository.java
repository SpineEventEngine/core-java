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

package io.spine.server.route.given.user;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.UserId;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRouting;
import io.spine.server.route.given.user.event.RUserConsentRequested;
import io.spine.server.route.given.user.event.RUserSignedIn;
import io.spine.test.event.RSession;
import io.spine.test.event.RSessionId;

import java.util.Iterator;
import java.util.Set;

public class SessionRepository
        extends ProjectionRepository<RSessionId, SessionProjection, RSession> {

    @OverridingMethodsMustInvokeSuper
    @Override
    protected void setupEventRouting(EventRouting<RSessionId> routing) {
        super.setupEventRouting(routing);
        routing.route(RUserSignedIn.class,
                      (message, context) -> ImmutableSet.of(message.getSessionId()))
               .route(RUserConsentRequested.class,
                      (message, context) -> findByUserId(message.getUserId()));
    }

    private Set<RSessionId> findByUserId(UserId id) {
        RSession.Query query =
                RSession.query()
                        .userId().is(id)
                        .build();
        Iterator<RSessionId> identifiers = recordStorage().index(query);
        ImmutableSet<RSessionId> result = ImmutableSet.copyOf(identifiers);
        return result;
    }
}
