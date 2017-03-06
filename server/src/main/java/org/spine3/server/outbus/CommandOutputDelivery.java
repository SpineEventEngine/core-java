/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.outbus;

import com.google.common.base.Function;
import org.spine3.Internal;
import org.spine3.base.EventClass;
import org.spine3.base.EventEnvelope;
import org.spine3.base.MessageClass;
import org.spine3.base.MessageEnvelope;
import org.spine3.server.delivery.Delivery;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventDispatcher;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Base functionality for the routines delivering the
 * {@linkplain org.spine3.base.MessageEnvelope message envelopes} to the consumers, such as
 * {@linkplain org.spine3.server.bus.MessageDispatcher message dispatchers}.
 *
 * @param <E> the type of the envelope
 * @param <T> the class of the message packed into the envelope
 * @param <C> the type of the consumer
 * @author Alex Tymchenko
 */
public abstract class CommandOutputDelivery<E extends MessageEnvelope,
                                            T extends MessageClass, C> extends Delivery<E, C> {

    private Function<T, Set<C>> consumerProvider;

    /** {@inheritDoc} */
    protected CommandOutputDelivery(Executor delegate) {
        super(delegate);
    }

    /** {@inheritDoc} */
    protected CommandOutputDelivery() {
        super();
    }

    /**
     * Used by the instance of {@linkplain CommandOutputBus bus} to inject the knowledge about
     * up-to-date consumers for the message
     */
    public void setConsumerProvider(Function<T, Set<C>> consumerProvider) {
        this.consumerProvider = consumerProvider;
    }

    @Override
    protected final Collection<C> consumersFor(E envelope) {
        @SuppressWarnings("unchecked")  // It's fine by the definition of <E> and <T>.
        final T eventClass = (T) envelope.getMessageClass();
        return consumerProvider.apply(eventClass);
    }
}
