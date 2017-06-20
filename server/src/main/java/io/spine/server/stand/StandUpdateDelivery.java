/*
 *
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
 *
 */
package io.spine.server.stand;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import io.spine.annotation.SPI;
import io.spine.server.delivery.Delivery;
import io.spine.server.entity.EntityStateEnvelope;
import io.spine.server.projection.ProjectionRepository;

import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * A base class for the strategies on delivering the {@code Entity} state updates
 * to the {@code Stand}.
 *
 * <p>Common delivery sources are
 * {@link io.spine.server.aggregate.AggregateRepository AggregateRepository}
 * and {@link ProjectionRepository}.
 *
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess")   // Part of API.
public abstract class StandUpdateDelivery extends Delivery<EntityStateEnvelope<?, ?>, Stand> {

    private Stand stand;

    protected StandUpdateDelivery(Executor delegate) {
        super(delegate);
    }

    protected StandUpdateDelivery() {
        super();
    }

    void setStand(Stand stand) {
        this.stand = stand;
    }

    @Override
    protected Runnable getDeliveryAction(final Stand consumer,
                                         final EntityStateEnvelope<?, ?> deliverable) {
        return new Runnable() {
            @Override
            public void run() {
                consumer.update(deliverable);
            }
        };
    }

    @Override
    protected Collection<Stand> consumersFor(EntityStateEnvelope deliverable) {
        return Lists.newArrayList(stand);
    }

    /**
     * Obtains a pre-defined instance of the {@code StandUpdateDelivery}, which does NOT
     * postpone any event dispatching and uses
     * {@link com.google.common.util.concurrent.MoreExecutors#directExecutor() direct executor}
     * for operation.
     *
     * @return the pre-configured direct delivery
     */
    public static StandUpdateDelivery directDelivery() {
        return new DirectDelivery();
    }

    /**
     * A delivery implementation which does not postpone events.
     *
     * @see #directDelivery()
     */
    @VisibleForTesting
    static final class DirectDelivery extends StandUpdateDelivery {
        @Override
        public boolean shouldPostponeDelivery(EntityStateEnvelope envelope,
                                              Stand consumer) {
            return false;
        }
    }
}
