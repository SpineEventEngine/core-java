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

package io.spine.server.route.given.user;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.UserId;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRouting;
import io.spine.server.route.given.user.event.UserConsentRequested;
import io.spine.server.route.given.user.event.UserSignedIn;
import io.spine.test.event.Session;
import io.spine.test.event.SessionId;

import java.util.Iterator;
import java.util.Set;

import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toSet;

public class SessionRepository
        extends ProjectionRepository<SessionId, SessionProjection, Session> {

    @OverridingMethodsMustInvokeSuper
    @Override
    protected void setupEventRouting(EventRouting<SessionId> routing) {
        super.setupEventRouting(routing);
        routing.route(UserSignedIn.class,
                      (message, context) -> ImmutableSet.of(message.getSessionId()))
               .route(UserConsentRequested.class,
                      (message, context) -> findByUserId(message.getUserId()));
    }

    private Set<SessionId> findByUserId(UserId id) {
        Iterator<SessionProjection> iterator =
                iterator(projection -> projection.state()
                                                 .getUserId()
                                                 .equals(id));
        Set<SessionId> ids = stream(iterator)
                .map(SessionProjection::id)
                .collect(toSet());
        return ids;
    }
}
