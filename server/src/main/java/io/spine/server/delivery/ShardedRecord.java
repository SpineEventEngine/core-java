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

package io.spine.server.delivery;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.GeneratedMixin;

/**
 * A Protobuf {@link com.google.protobuf.Message Message} used for storage in a sharded environment.
 *
 * @author Alex Tymchenko
 */
@GeneratedMixin
public interface ShardedRecord extends Message {

    /**
     * Returns the index of the shard in which this record resides.
     */
    @SuppressWarnings("override")   // Implemented in Protobuf-generated code.
    ShardIndex getShardIndex();

    /**
     * Returns the moment of time, when the message was originally received to be sharded.
     *
     * <p>This is not the time of storing the record, but the time of the message originating in
     * the subsystem, which stores data splitting it into shards.
     */
    @SuppressWarnings("override")   // Implemented in Protobuf-generated code.
    Timestamp getWhenReceived();
}
