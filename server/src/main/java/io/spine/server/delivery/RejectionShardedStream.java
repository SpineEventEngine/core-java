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
import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.protobuf.AnyPacker;

/**
 * The stream of rejections sent to a specific shard.
 *
 * @param <I> the type of the identifiers the of rejection targets.
 * @author Alex Tymchenko
 */
public class RejectionShardedStream<I> extends ShardedStream<I, Rejection, RejectionEnvelope> {

    private RejectionShardedStream(Builder<I> builder) {
        super(builder);
    }

    public static <I> Builder<I> newBuilder() {
        return new Builder<>();
    }

    @Override
    protected ShardedMessageConverter<I, Rejection, RejectionEnvelope> newConverter() {
        return new Converter<>();
    }

    /**
     * The converter of {@link RejectionEnvelope} into {@link ShardedMessage} instances
     * and vice versa.
     *
     * @param <I> the type of identifiers of the rejection targets.
     */
    private static class Converter<I> extends ShardedMessageConverter<I, Rejection, RejectionEnvelope> {

        @Override
        protected RejectionEnvelope toEnvelope(Any packedRejection) {
            Rejection rejection = AnyPacker.unpack(packedRejection);
            RejectionEnvelope result = RejectionEnvelope.of(rejection);
            return result;
        }
    }

    /**
     * The builder for the {@code RejectionShardedStream} instances.
     *
     * @param <I> the type of the identifiers the of rejection targets.
     */
    public static class Builder<I> extends AbstractBuilder<I,
                                                           RejectionEnvelope,
                                                           Builder<I>,
                                                           RejectionShardedStream<I>> {
        @Override
        protected RejectionShardedStream<I> createStream() {
            return new RejectionShardedStream<>(this);
        }
    }
}
