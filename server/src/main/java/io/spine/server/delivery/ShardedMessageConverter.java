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
package io.spine.server.delivery;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Time;
import io.spine.core.MessageEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.protobuf.TypeConverter;

/**
 * The converter of messages, which have to be sent to a specific shard, into {@link ShardedMessage}
 * and vice versa.
 *
 * <p>As long as the messages originally travel through the {@linkplain io.spine.server.bus.Bus
 * buses} packed as envelopes, this converter takes an envelope of a certain type and the ID of
 * the target entity, and transforms them into a {@code ShardedMessage} (a Protobuf message), which
 * — unlike the envelope — can be easily transferred over the wire.
 *
 * <p>The converter, of course, serves for the reverse {@code ShardedMessage} -> "original envelope"
 * and {@code ShardedMessage} -> "target ID" transformations as well.
 *
 * @param <I> the identifier of the message targets
 * @param <M> the type of the messages being converted
 * @param <E> the type of the message envelopes
 * @author Alex Tymchenko
 */
abstract class ShardedMessageConverter<I, M extends Message, E extends MessageEnvelope<?, M, ?>> {

    protected abstract E toEnvelope(Any packedOriginalMessage);

    protected ShardedMessage convert(I targetId, E envelope) {

        M originalMessage = envelope.getOuterObject();
        String stringId = envelope.idAsString();
        ShardedMessageId shardedMessageId = ShardedMessageId.newBuilder()
                                                            .setValue(stringId)
                                                            .build();
        Any packedOriginalMsg = AnyPacker.pack(originalMessage);
        Message message = TypeConverter.toMessage(targetId);
        Any packedTargetId = AnyPacker.pack(message);
        ShardedMessage result = ShardedMessage.newBuilder()
                                              .setId(shardedMessageId)
                                              .setTargetId(packedTargetId)
                                              .setOriginalMessage(packedOriginalMsg)
                                              .setWhenSharded(Time.getCurrentTime())
                                              .build();
        return result;
    }

    I targetIdOf(ShardedMessage message, Class<I> idClass) {
        Any asAny = message.getTargetId();
        I result = TypeConverter.toObject(asAny, idClass);
        return result;
    }

    E envelopeOf(ShardedMessage message) {
        Any packedOriginalMessage = message.getOriginalMessage();
        E result = toEnvelope(packedOriginalMessage);
        return result;
    }
}
