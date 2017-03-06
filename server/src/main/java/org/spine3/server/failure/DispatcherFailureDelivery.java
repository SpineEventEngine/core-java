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
package org.spine3.server.failure;

import org.spine3.SPI;
import org.spine3.server.outbus.CommandOutputDelivery;

import java.util.concurrent.Executor;

/**
 * A base class for the strategies on delivering the {@linkplain org.spine3.base.Failure failures}
 * from the {@linkplain FailureBus failure bus} to the corresponding
 * {@linkplain FailureDispatcher failure dispatchers}.
 *
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess")   // Part of API.
public abstract class DispatcherFailureDelivery extends CommandOutputDelivery<FailureEnvelope,
                                                                              FailureClass,
                                                                              FailureDispatcher> {

    /**
     * Create a dispatcher failure delivery with an {@link Executor} used for the operation.
     *
     * @param delegate the instance of {@code Executor} used to dispatch business failures.
     * @see CommandOutputDelivery#CommandOutputDelivery(Executor)
     */
    protected DispatcherFailureDelivery(Executor delegate) {
        super(delegate);
    }

    /**
     * Creates an instance of failure delivery with a
     * {@link com.google.common.util.concurrent.MoreExecutors#directExecutor() direct executor}
     * used for the business failure dispatching.
     *
     * @see CommandOutputDelivery#CommandOutputDelivery()
     */
    protected DispatcherFailureDelivery() {
        super();
    }

    @Override
    protected Runnable getDeliveryAction(final FailureDispatcher consumer,
                                         final FailureEnvelope deliverable) {
        return new Runnable() {
            @Override
            public void run() {
                consumer.dispatch(deliverable);
            }
        };
    }

    /**
     * Obtains a pre-defined instance of the {@code DispatcherFailureDelivery}, which does NOT
     * postpone any failure dispatching and uses
     * {@link com.google.common.util.concurrent.MoreExecutors#directExecutor() direct executor}
     * for operation.
     *
     * @return the pre-configured default executor.
     */
    public static DispatcherFailureDelivery directDelivery() {
        return PredefinedDeliveryStrategies.DIRECT_DELIVERY;
    }

    /** Utility wrapper class for predefined delivery strategies designed to be constants. */
    private static final class PredefinedDeliveryStrategies {

        /**
         * A pre-defined instance of the {@code DispatcherFailureDelivery},
         * which does not postpone any failure dispatching and uses
         * {@link com.google.common.util.concurrent.MoreExecutors#directExecutor() direct executor}
         * for operation.
         */
        private static final DispatcherFailureDelivery DIRECT_DELIVERY =
                new DispatcherFailureDelivery() {
                    @Override
                    public boolean shouldPostponeDelivery(FailureEnvelope envelope,
                                                          FailureDispatcher dispatcher) {
                        return false;
                    }
                };
    }
}
