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
package io.spine.server.procman;

import io.spine.core.CommandEnvelope;
import io.spine.server.delivery.CommandShardedStream;
import io.spine.server.delivery.DeliveryTag;
import io.spine.type.MessageClass;

/**
 * A strategy on delivering the ecommandsvents to the instances of a certain process manager type.
 *
 * @param <I> the ID type of process manager, to which commands are being delivered
 * @param <P> the type of process manager
 * @author Alex Tymchenko
 */
public class PmCommandDelivery<I, P extends ProcessManager<I, ?, ?>>
        extends PmDelivery<I,
                           P,
                           CommandEnvelope,
                           CommandShardedStream<I>,
                           CommandShardedStream.Builder<I>> {

    protected PmCommandDelivery(ProcessManagerRepository<I, P, ?> repository) {
        super(new PmCommandConsumer<>(repository));
    }

    private static class PmCommandConsumer<I, P extends ProcessManager<I, ?, ?>>
            extends PmMessageConsumer<I,
                                      P,
                                      CommandEnvelope,
                                      CommandShardedStream<I>,
                                      CommandShardedStream.Builder<I>> {

        protected PmCommandConsumer(ProcessManagerRepository<I, P, ?> repository) {
            super(DeliveryTag.forCommandsOf(repository), repository);
        }

        @Override
        protected CommandShardedStream.Builder<I> newShardedStreamBuilder() {
            return CommandShardedStream.newBuilder();
        }

        @Override
        protected PmCommandEndpoint<I, P> proxyFor(I procmanId, MessageClass targetClass) {
            return new PmCommandEndpoint<>(repository(), procmanId);
        }
    }
}

