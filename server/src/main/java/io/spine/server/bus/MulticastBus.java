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
package io.spine.server.bus;

import com.google.common.base.Function;
import com.google.protobuf.Message;
import io.spine.core.MessageEnvelope;
import io.spine.server.delivery.MulticastDelivery;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@code Bus}, which delivers a single message to multiple dispatchers.
 *
 * @author Alex Tymchenko
 */
public abstract class MulticastBus<M extends Message,
                                   E extends MessageEnvelope<?, M, ?>,
                                   C extends MessageClass,
                                   D extends MessageDispatcher<C, E, ?>>
        extends Bus<M, E, C, D> {

    /**
     * The strategy to deliver the messages to the dispatchers.
     */
    private final MulticastDelivery<E, C, D> delivery;

    protected MulticastBus(MulticastDelivery<E, C, D> delivery) {
        super();
        this.delivery = delivery;
        injectDispatcherProvider();
    }

    /**
     * Sets up the {@code MulticastDelivery} with an ability to obtain
     * {@linkplain MessageDispatcher message dispatchers} by a given
     * {@linkplain MessageClass message class} instance at runtime.
     */
    private void injectDispatcherProvider() {
        delivery().setConsumerProvider(new DispatcherProvider());
    }

    /**
     * Call the dispatchers for the {@code messageEnvelope}.
     *
     * @param messageEnvelope the message envelope to pass to the dispatchers.
     * @return the number of the dispatchers called, or {@code 0} if there weren't any.
     */
    protected int callDispatchers(E messageEnvelope) {
        @SuppressWarnings("unchecked")  // it's fine, since the message is validated previously.
        final C messageClass = (C) messageEnvelope.getMessageClass();
        final Collection<D> dispatchers = registry().getDispatchers(messageClass);
        delivery().deliver(messageEnvelope);
        return dispatchers.size();
    }

    /**
     * Obtains the {@linkplain MulticastDelivery delivery strategy} configured for this bus.
     *
     * @return the delivery strategy
     */
    protected MulticastDelivery<E, C, D> delivery() {
        return this.delivery;
    }

    /**
     * A provider of dispatchers for a given message class.
     */
    private class DispatcherProvider implements Function<C, Set<D>> {
        @Nullable
        @Override
        public Set<D> apply(@Nullable C messageClass) {
            checkNotNull(messageClass);
            final Set<D> dispatchers = registry().getDispatchers(messageClass);
            return dispatchers;
        }
    }
}
