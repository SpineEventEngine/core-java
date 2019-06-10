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

package io.spine.server.sharding;

import com.google.protobuf.Timestamp;
import io.spine.server.NodeId;

import javax.annotation.OverridingMethodsMustInvokeSuper;

/**
 * The session of processing the messages, which reside in a shard.
 *
 * <p>Starts by {@linkplain ShardedWorkRegistry#pickUp(ShardIndex, NodeId)} picking up}
 * the shard to process.
 *
 * @author Alex Tymchenko
 */
public abstract class ShardProcessingSession {

    private final ShardIndex index;
    private Timestamp whenLastMessageProcessed;

    protected ShardProcessingSession(ShardSessionRecord record) {
        this.index = record.getIndex();
        whenLastMessageProcessed = record.getWhenLastMessageProcessed();
    }

    public ShardIndex shardIndex() {
        return index;
    }

    public Timestamp whenLastMessageProcessed() {
        return whenLastMessageProcessed;
    }

    @OverridingMethodsMustInvokeSuper
    protected void updateLastProcessed(Timestamp timestamp) {
        this.whenLastMessageProcessed = timestamp;
    }

    /**
     * Completes this session and releases the picked shard, making it available for picking up.
     */
    protected abstract void complete();
}
