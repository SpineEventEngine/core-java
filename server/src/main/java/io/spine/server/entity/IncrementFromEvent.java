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

package io.spine.server.entity;

import io.spine.annotation.Internal;
import io.spine.core.Version;
import io.spine.server.type.EventEnvelope;

/**
 * A version increment which sets the new version from the given event.
 *
 * <p>Such increment strategy is applied to the {@link Entity} types which represent a sequence of
 * events.
 *
 * <p>One example of such entity is {@link io.spine.server.aggregate.Aggregate Aggregate}.
 * As a sequence of events, an {@code Aggregate} has no own versioning system, thus inherits the
 * versions of the {@linkplain io.spine.server.aggregate.Apply applied} events. In other words, the
 * current version of an {@code Aggregate} is the version of the last applied event.
 */
@Internal
public class IncrementFromEvent extends VersionIncrement {

    private final EventEnvelope event;

    public IncrementFromEvent(Transaction transaction, EventEnvelope event) {
        super(transaction);
        this.event = event;
    }

    @Override
    protected Version nextVersion() {
        Version result = event.context()
                              .getVersion();
        return result;
    }
}
