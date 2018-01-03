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
package io.spine.server.delivery;

import com.google.common.base.Function;
import io.spine.annotation.Internal;
import io.spine.core.MessageEnvelope;
import io.spine.server.outbus.CommandOutputBus;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base functionality for the routines delivering a single
 * {@linkplain MessageEnvelope message envelope} to several consumers, such as
 * {@linkplain io.spine.server.bus.MessageDispatcher message dispatchers}.
 *
 * @param <E> the type of the envelope
 * @param <T> the class of the message packed into the envelope
 * @param <C> the type of the consumer
 * @author Alex Tymchenko
 */
@Internal
public abstract class MulticastDelivery<E extends MessageEnvelope,
                                        T extends MessageClass, C> extends Delivery<E, C> {

    /**
     *  A function, returning a set of consumers by the message class.
     *
     *  <p>Defined and set at runtime by the corresponding {@code Bus}.
     *  Until then it is {@code null}.
     */
    @Nullable
    private Function<T, Set<C>> consumerProvider;

    /**
     * Creates a new delivery instance which performs its actions using
     * the passed {@code Executor}.
     */
    protected MulticastDelivery(Executor delegate) {
        super(delegate);
    }

    /** Creates a new direct delivery instance. */
    protected MulticastDelivery() {
        super();
    }

    /**
     * Used by the instance of {@linkplain CommandOutputBus bus} to inject the knowledge about
     * up-to-date consumers for the message
     */
    public void setConsumerProvider(Function<T, Set<C>> consumerProvider) {
        checkNotNull(consumerProvider);
        this.consumerProvider = consumerProvider;
    }

    @Override
    protected final Collection<C> consumersFor(E envelope) {
        checkNotNull(consumerProvider,
                     "Consumer provider must be set by the corresponding Bus " +
                             "for this delivery: " + getClass());
        @SuppressWarnings("unchecked")  // It's fine by the definition of <E> and <T>.
        final T eventClass = (T) envelope.getMessageClass();
        return consumerProvider.apply(eventClass);
    }
}
