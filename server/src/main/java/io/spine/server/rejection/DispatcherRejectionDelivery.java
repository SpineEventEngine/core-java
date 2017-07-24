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
package io.spine.server.rejection;

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.SPI;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.server.outbus.CommandOutputDelivery;

import java.util.concurrent.Executor;

/**
 * A base class for the strategies on delivering the {@linkplain Rejection rejections}
 * from the {@linkplain RejectionBus rejection bus} to the corresponding
 * {@linkplain RejectionDispatcher rejection dispatchers}.
 *
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess")   // Part of API.
public abstract class DispatcherRejectionDelivery
        extends CommandOutputDelivery<RejectionEnvelope, RejectionClass, RejectionDispatcher<?>> {

    /**
     * Create a dispatcher rejection delivery with an {@link Executor} used for the operation.
     *
     * @param delegate the instance of {@code Executor} used to dispatch business rejections.
     * @see CommandOutputDelivery#CommandOutputDelivery(Executor)
     */
    protected DispatcherRejectionDelivery(Executor delegate) {
        super(delegate);
    }

    /**
     * Creates an instance of rejection delivery with a
     * {@link com.google.common.util.concurrent.MoreExecutors#directExecutor() direct executor}
     * used for the business rejection dispatching.
     *
     * @see CommandOutputDelivery#CommandOutputDelivery()
     */
    protected DispatcherRejectionDelivery() {
        super();
    }

    @Override
    protected Runnable getDeliveryAction(final RejectionDispatcher<?> consumer,
                                         final RejectionEnvelope deliverable) {
        return new Runnable() {
            @Override
            public void run() {
                consumer.dispatch(deliverable);
            }
        };
    }

    /**
     * Obtains a pre-defined instance of the {@code DispatcherFailureDelivery}, which does not
     * postpone any rejeciton dispatching and uses
     * {@link com.google.common.util.concurrent.MoreExecutors#directExecutor() direct executor}
     * for operation.
     *
     * @return the pre-configured direct delivery
     */
    public static DispatcherRejectionDelivery directDelivery() {
        return new DirectDelivery();
    }

    /**
     * A delivery implementation which does not postpone events.
     *
     * @see #directDelivery()
     */
    @VisibleForTesting
    static final class DirectDelivery extends DispatcherRejectionDelivery {
        @Override
        public boolean shouldPostponeDelivery(RejectionEnvelope envelope,
                                              RejectionDispatcher<?> dispatcher) {
            return false;
        }
    }
}
