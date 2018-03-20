/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Time;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.MessageEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.server.integration.ChannelId;
import io.spine.string.Stringifiers;
import io.spine.type.ClassName;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility classes for working with the {@linkplain ShardedMessage sharded messages}.
 *
 * @author Alex Tymchenko
 */
public class ShardedMessages {

    /**
     * Prevents an instantiation of this utility class.
     */
    private ShardedMessages() {
    }

    public static ShardedMessage of(Message targetId, CommandEnvelope commandEnvelope) {
        checkNotNull(targetId);
        checkNotNull(commandEnvelope);

        final CommandId id = commandEnvelope.getId();
        final Message originalMessage = commandEnvelope.getMessage();
        final ShardedMessageId shardedMessageId = ShardedMessageId.newBuilder()
                                                       .setValue(Stringifiers.toString(id))
                                                       .build();
        final Any packedOriginalMsg = AnyPacker.pack(originalMessage);
        final Any packedTargetId = AnyPacker.pack(targetId);
        final ShardedMessage result = ShardedMessage.newBuilder()
                                                    .setId(shardedMessageId)
                                                    .setTargetId(packedTargetId)
                                                    .setOriginalMessage(packedOriginalMsg)
                                                    .setWhenSharded(Time.getCurrentTime())
                                                    .build();
        return result;
    }

    public static <E extends MessageEnvelope<?, ?, ?>>
    ChannelId toChannelId(BoundedContextName boundedContextName,
                          ShardingKey key,
                          Class<E> envelopeClass) {
        checkNotNull(key);
        checkNotNull(boundedContextName);
        checkNotNull(envelopeClass);

        final ClassName className = key.getEntityClass()
                                       .getClassName();
        final ShardIndex shardIndex = key.getIndex();

        final String value = on("__").join("bc_", boundedContextName.getValue(),
                                           "target_", className,
                                           "prd_", Stringifiers.toString(shardIndex),
                                           "env_", envelopeClass);
        final StringValue asMsg = StringValue.newBuilder()
                                             .setValue(value)
                                             .build();
        final Any asAny = AnyPacker.pack(asMsg);
        final ChannelId result = ChannelId.newBuilder()
                                          .setIdentifier(asAny)
                                          .build();
        return result;
    }
}
