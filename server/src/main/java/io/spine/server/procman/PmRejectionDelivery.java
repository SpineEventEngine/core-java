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
package io.spine.server.procman;

import io.spine.core.RejectionEnvelope;
import io.spine.server.sharding.RejectionShardedStream;
import io.spine.server.sharding.ShardingTag;

/**
 * A strategy on delivering the rejections to the instances of a certain process manager type.
 *
 * @param <I> the ID type of process manager, to which rejections are being delivered
 * @param <P> the type of process manager
 * @author Alex Tymchenko
 */
public class PmRejectionDelivery<I, P extends ProcessManager<I, ?, ?>>
        extends PmEndpointDelivery<I, P, RejectionEnvelope,
                                   RejectionShardedStream<I>, RejectionShardedStream.Builder<I>> {

    protected PmRejectionDelivery(ProcessManagerRepository<I, P, ?> repository) {
        super(new PmRejectionConsumer<>(repository));
    }

    private static class PmRejectionConsumer<I, P extends ProcessManager<I, ?, ?>>
            extends PmMessageConsumer<I, P, RejectionEnvelope, RejectionShardedStream<I>,
            RejectionShardedStream.Builder<I>> {
        protected PmRejectionConsumer(ProcessManagerRepository<I, P, ?> repository) {
            super(ShardingTag.forRejectionsOf(repository.getShardedModelClass()), repository);
        }

        @Override
        protected RejectionShardedStream.Builder<I> newShardedStreamBuilder() {
            return RejectionShardedStream.newBuilder();
        }

        @Override
        protected PmRejectionEndpoint<I, P> getEndpoint(RejectionEnvelope envelope) {
            return PmRejectionEndpoint.of(repository(), envelope);
        }
    }
}
