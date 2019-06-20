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

package io.spine.server.trace.given;

import com.google.protobuf.Message;
import io.spine.core.MessageId;
import io.spine.core.Signal;
import io.spine.server.trace.AbstractTracer;
import io.spine.type.TypeUrl;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.base.Identifier.pack;

public final class FakeTracer extends AbstractTracer {

    private final Set<MessageId> receivers = newHashSet();

    FakeTracer(Signal<?, ?, ?> signal) {
        super(signal);
    }

    public boolean isReceiver(Message entityId, TypeUrl entityStateType) {
        checkNotNull(entityId);
        MessageId id = MessageId
                .newBuilder()
                .setId(pack(entityId))
                .setTypeUrl(entityStateType.value())
                .vBuild();
        return receivers.contains(id);
    }

    @Override
    public void processedBy(MessageId receiver) {
        checkNotNull(receiver);
        MessageId idWithoutVersion = receiver
                .toBuilder()
                .clearVersion()
                .vBuild();
        receivers.add(idWithoutVersion);
    }

    @Override
    public void close() {
        // NOP.
    }
}
