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

import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.server.transport.TransportFactory;

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
        if(converter == null) {
            converter = new ShardedMessageConverter<I, CommandEnvelope>() {
                @Override
                public ShardedMessage convert(Object id, CommandEnvelope envelope) {
                    return null;
                }

                @Override
                public I targetIdOf(ShardedMessage message) {
                    return null;
                }

                @Override
                public CommandEnvelope envelopeOf(ShardedMessage message) {
                    return null;
                }
            };
        }
        return converter;
    }


}
