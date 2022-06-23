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

package io.spine.server.delivery;

import io.spine.annotation.GeneratedMixin;
import io.spine.annotation.Internal;
import io.spine.core.TenantId;

import static io.spine.base.Identifier.newUuid;

/**
 * A mixin for {@link InboxMessage}.
 */
@GeneratedMixin
@Internal
public interface InboxMessageMixin extends ShardedRecord, InboxMessageOrBuilder {

    @Override
    default ShardIndex shardIndex() {
        return getId().getIndex();
    }

    /**
     * Returns the {@link TenantId} for the original {@linkplain #getPayloadCase() signal payload}.
     */
    default TenantId tenant() {
        return hasCommand()
                       ? getCommand().tenant()
                       : getEvent().tenant();
    }

    /**
     * Generates a new {@code InboxMessageId} with a auto-generated UUID and the given shard
     * index as parts.
     */
    static InboxMessageId generateIdWith(ShardIndex index) {
        return InboxMessageId.newBuilder()
                             .setUuid(newUuid())
                             .setIndex(index)
                             .vBuild();
    }
}
