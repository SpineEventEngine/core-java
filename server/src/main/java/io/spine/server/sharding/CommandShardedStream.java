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
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.protobuf.AnyPacker;
import io.spine.protobuf.TypeConverter;
import io.spine.server.transport.TransportFactory;
import io.spine.string.Stringifiers;
import io.spine.time.Time;

import javax.annotation.Nullable;

/**
 * @author Alex Tymchenko
 */
public class CommandShardedStream<I> extends ShardedStream<I, CommandEnvelope> {

    @Nullable
    private ShardedMessageConverter<I, CommandEnvelope> converter;

    protected CommandShardedStream(ShardingKey key,
                                   TransportFactory transportFactory,
                                   BoundedContextName boundedContextName) {
        super(key, transportFactory, boundedContextName);
    }

    @Override
    protected ShardedMessageConverter<I, CommandEnvelope> converter() {
        if (converter == null) {
            converter = new Converter<>();
        }
        return converter;
    }

    private static class Converter<I> extends ShardedMessageConverter<I, CommandEnvelope> {

        @Override
        public ShardedMessage convert(I targetId, CommandEnvelope envelope) {

            final CommandId id = envelope.getId();
            final Message originalMessage = envelope.getMessage();
            final String stringId = Stringifiers.toString(id);
            final ShardedMessageId shardedMessageId = ShardedMessageId.newBuilder()
                                                                      .setValue(stringId)
                                                                      .build();
            final Any packedOriginalMsg = AnyPacker.pack(originalMessage);
            final Any packedTargetId = AnyPacker.pack(TypeConverter.toMessage(targetId));
            final ShardedMessage result = ShardedMessage.newBuilder()
                                                        .setId(shardedMessageId)
                                                        .setTargetId(packedTargetId)
                                                        .setOriginalMessage(packedOriginalMsg)
                                                        .setWhenSharded(Time.getCurrentTime())
                                                        .build();
            return result;
        }

        @Override
        public I targetIdOf(ShardedMessage message) {
            final Any asAny = message.getTargetId();
            final I result = TypeConverter.toObject(asAny, getIdClass());
            return result;
        }

        @Override
        public CommandEnvelope envelopeOf(ShardedMessage message) {
            final Any originalMessage = message.getOriginalMessage();
            final Command command = AnyPacker.unpack(originalMessage);
            final CommandEnvelope result = CommandEnvelope.of(command);
            return result;
        }
    }
}
