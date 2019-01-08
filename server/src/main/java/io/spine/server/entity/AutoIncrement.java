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
import io.spine.core.Versions;

/**
 * A version increment which always advances the given entity version by 1.
 *
 * <p>Such increment strategy is applied to the {@link Entity} types which cannot use the event
 * versions, such as {@link io.spine.server.projection.Projection Projection}s.
 *
 * <p>A {@code Projection} represents an arbitrary cast of data in a specific moment in
 * time. The events applied to a {@code Projection} are produced by different {@code Entities}
 * and have no common versioning. Thus, a {@code Projection} has its own versioning system.
 * Each event <i>increments</i> the {@code Projection} version by one.
 */
@Internal
public class AutoIncrement extends VersionIncrement {

    public AutoIncrement(Transaction transaction) {
        super(transaction);
    }

    @Override
    protected Version nextVersion() {
        Version current = transaction().getVersion();
        Version result = Versions.increment(current);
        return result;
    }
}
