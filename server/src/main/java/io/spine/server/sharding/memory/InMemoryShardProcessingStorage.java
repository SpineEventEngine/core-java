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

package io.spine.server.sharding.memory;

import io.spine.server.sharding.ShardIndex;
import io.spine.server.sharding.ShardProcessingSessionRecord;
import io.spine.server.sharding.ShardProcessingStorage;
import io.spine.server.sharding.ShardRegistryReadRequest;

import java.util.Iterator;
import java.util.Optional;

/**
 * An in-memory implementation of {@code ShardProcessingStorage}.
 */
public class InMemoryShardProcessingStorage extends ShardProcessingStorage {

    @Override
    public Iterator<ShardIndex> index() {
        return null;
    }

    @Override
    public Optional<ShardProcessingSessionRecord> read(ShardRegistryReadRequest request) {
        return Optional.empty();
    }

    @Override
    public void write(ShardIndex id, ShardProcessingSessionRecord record) {

    }
}
