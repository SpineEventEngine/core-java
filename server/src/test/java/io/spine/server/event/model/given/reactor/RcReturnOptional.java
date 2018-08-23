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

package io.spine.server.event.model.given.reactor;

import io.spine.server.event.React;
import io.spine.test.reflect.event.RefProjectCreated;
import io.spine.test.reflect.event.RefProjectStarted;

import java.util.Optional;

/**
 * Provides reactor method that returns {@link Optional} with an event,
 * if the incoming event has valid identifier. Otherwise, return {@code Optional.empty()}.
 *
 * @author Alexander Yevsyukov
 */
public class RcReturnOptional extends TestEventReactor {

    @React
    Optional<RefProjectStarted> react(RefProjectCreated event) {
        if (event.hasProjectId()) {
            return Optional.of(
                    RefProjectStarted.newBuilder()
                                     .setProjectId(event.getProjectId())
                                     .build()
            );
        }
        return Optional.empty();
    }
}
