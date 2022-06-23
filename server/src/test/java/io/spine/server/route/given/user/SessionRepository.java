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

import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toSet;

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
        Iterator<SessionProjection> iterator =
                iterator(projection -> projection.state()
                                                 .getUserId()
                                                 .equals(id));
        Set<RSessionId> ids = stream(iterator)
                .map(SessionProjection::id)
                .collect(toSet());
        return ids;
    }
}
