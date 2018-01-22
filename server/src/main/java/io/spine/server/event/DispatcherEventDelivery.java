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
package io.spine.server.event;

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.SPI;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.delivery.MulticastDelivery;

import java.util.concurrent.Executor;

/**
 * A base class for the strategies on delivering the {@link Event events}
 * from the {@linkplain EventBus event bus} to the corresponding
 * {@linkplain EventDispatcher event dispatchers}.
 *
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess")   // Part of API.
public abstract class DispatcherEventDelivery
        extends MulticastDelivery<EventEnvelope, EventClass, EventDispatcher<?>> {

    /**
     * Create a dispatcher event delivery with an {@link Executor} used for the operation.
     *
     * @param delegate the instance of {@code Executor} used to dispatch events.
     * @see MulticastDelivery#MulticastDelivery(Executor)
     */
    protected DispatcherEventDelivery(Executor delegate) {
        super(delegate);
    }

    /**
     * Creates an instance of event delivery with a
     * {@link com.google.common.util.concurrent.MoreExecutors#directExecutor() direct executor}
     * used for event dispatching.
     *
     * @see MulticastDelivery#MulticastDelivery()
     */
    protected DispatcherEventDelivery() {
        super();
    }

    @Override
    protected Runnable getDeliveryAction(final EventDispatcher<?> consumer,
                                         final EventEnvelope envelope) {
        return new Runnable() {
            @Override
            public void run() {
                consumer.dispatch(envelope);
            }
        };
    }

    /**
     * Obtains a pre-defined instance of the {@code DispatcherEventDelivery}, which does NOT
     * postpone any event dispatching and uses
     * {@link com.google.common.util.concurrent.MoreExecutors#directExecutor() direct executor}
     * for operation.
     *
     * @return the pre-configured direct delivery
     */
    public static DispatcherEventDelivery directDelivery() {
        return new DirectDelivery();
    }

    /**
     * A delivery implementation which does not postpone events.
     *
     * @see #directDelivery()
     */
    @VisibleForTesting
    static final class DirectDelivery extends DispatcherEventDelivery {
        @Override
        public boolean shouldPostponeDelivery(EventEnvelope envelope,
                                              EventDispatcher dispatcher) {
            return false;
        }
    }
}
