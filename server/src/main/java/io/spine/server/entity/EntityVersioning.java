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

package io.spine.server.entity;

import io.spine.annotation.Internal;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;

/**
 * The strategy of versioning of {@linkplain Entity entities} during a certain {@link Transaction}.
 */
@Internal
public enum EntityVersioning {

    /**
     * This strategy is applied to the {@link Entity} types which represent a sequence of
     * events.
     *
     * <p>One example of such entity is {@link io.spine.server.aggregate.Aggregate Aggregate}.
     * As a sequence of events, an {@code Aggregate} has no own versioning system, thus
     * inherits the versions of the {@linkplain io.spine.server.aggregate.Apply applied}
     * events. In other words, the current version of an {@code Aggregate} is
     * the {@linkplain EventContext#getVersion() version} of last applied event.
     */
    FROM_EVENT {
        @Override
        VersionIncrement createVersionIncrement(Transaction transaction, EventEnvelope event) {
            return new IncrementFromEvent(transaction, event);
        }
    },

    /**
     * This strategy is applied to the {@link Entity} types which cannot use the event versions,
     * such as {@link io.spine.server.projection.Projection Projection}s.
     *
     * <p>A {@code Projection} represents an arbitrary cast of data in a specific moment in
     * time. The events applied to a {@code Projection} are produced by different {@code Entities}
     * and have no common versioning. Thus, a {@code Projection} has its own versioning system.
     * Each event <i>increments</i> the {@code Projection} version by one.
     */
    AUTO_INCREMENT {
        @Override
        VersionIncrement createVersionIncrement(Transaction transaction, EventEnvelope ignored) {
            return new AutoIncrement(transaction);
        }
    };

    /**
     * Creates a {@code VersionIncrement} which implements the versioning strategy for the given
     * transaction based on the last applied event.
     *
     * @param transaction
     *         the transaction for which the entity version should be modified
     * @param event
     *         the last event applied to the entity in transaction
     * @return a setter of the new version for the entity in transaction
     */
    abstract VersionIncrement createVersionIncrement(Transaction transaction, EventEnvelope event);
}
