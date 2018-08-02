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
package io.spine.server.outbus;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.core.Rejection;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.bus.MulticastBus;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * A base bus responsible for delivering the {@link io.spine.core.Command command} output.
 *
 * <p>The typical output artifacts of the command processing are:
 *
 * <ul>
 *     <li>{@linkplain Event events} — in case the command is handled successfully;
 *     <li>{@linkplain Rejection rejections} — if the command contradicts the business rules.
 * </ul>
 *
 * <p>The instances of {@code CommandOutputBus} are responsible for a delivery of such output
 * artifacts to the corresponding destinations.
 *
 * @author Alex Tymchenko
 */
@Internal
public abstract class CommandOutputBus<M extends Message,
                                       E extends MessageEnvelope<?, M, ?>,
                                       C extends MessageClass,
                                       D extends MessageDispatcher<C, E, ?>>
        extends MulticastBus<M, E, C, D> {

    protected CommandOutputBus(AbstractBuilder<E, M, ?> builder) {
        super(builder);
    }

    /**
     * Enriches the message posted to this instance of {@code CommandOutputBus}.
     *
     * @param  originalMessage the original message posted to the bus
     * @return the enriched message or the passed instance if {@code originalMessage} cannot
     *         be enriched
     */
    protected abstract E enrich(E originalMessage);

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected abstract OutputDispatcherRegistry<C, D> createRegistry();

    @Override
    protected void dispatch(E envelope) {
        E enrichedEnvelope = enrich(envelope);
        int dispatchersCalled = callDispatchers(enrichedEnvelope);
        checkState(dispatchersCalled != 0,
                   format("Message %s has no dispatchers.", envelope.getMessage()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected OutputDispatcherRegistry<C, D> registry() {
        return (OutputDispatcherRegistry<C, D>) super.registry();
    }
}
