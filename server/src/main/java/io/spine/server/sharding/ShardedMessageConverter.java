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
import io.spine.core.MessageEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.protobuf.TypeConverter;
import io.spine.string.Stringifiers;
import io.spine.time.Time;
import io.spine.util.GenericTypeIndex;

/**
 * @author Alex Tymchenko
 */
abstract class ShardedMessageConverter<I, E extends MessageEnvelope<?, ?, ?>> {

    protected abstract E toEnvelope(Any packedEnvelope);

    protected ShardedMessage convert(I targetId, E envelope) {

        final Message id = envelope.getId();
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

    protected I targetIdOf(ShardedMessage message) {
        final Any asAny = message.getTargetId();
        final I result = TypeConverter.toObject(asAny, getIdClass());
        return result;
    }

    protected E envelopeOf(ShardedMessage message) {
        final Any packedEnvelope = message.getOriginalMessage();
        final E result = toEnvelope(packedEnvelope);
        return result;
    }

    @SuppressWarnings("unchecked") // Ensured by the generic type definition.
    protected Class<I> getIdClass() {
        return (Class<I>) GenericParameter.ID.getArgumentIn(getClass());
    }

    /**
     * Enumeration of generic type parameters of this class.
     */
    enum GenericParameter implements GenericTypeIndex<ShardedMessageConverter> {

        /** The index of the generic type {@code <I>}. */
        ID(0);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return this.index;
        }

        @Override
        public Class<?> getArgumentIn(Class<? extends ShardedMessageConverter> cls) {
            return Default.getArgument(this, cls);
        }
    }
}
