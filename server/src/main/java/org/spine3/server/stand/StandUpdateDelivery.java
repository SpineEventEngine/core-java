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
package org.spine3.server.stand;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.SPI;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.delivery.Delivery;
import org.spine3.server.entity.Entity;
import org.spine3.server.projection.ProjectionRepository;

import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * A base class for the strategies on delivering the {@code Entity} state updates to
 * the {@code Stand} from the sources such as {@link org.spine3.server.aggregate.AggregateRepository AggregateRepository}
 * and {@link ProjectionRepository} via {@link StandFunnel}.
 *
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess")   // Part of API.
public abstract class StandUpdateDelivery extends Delivery<Entity, Stand> {

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
    protected Runnable getDeliveryAction(final Stand consumer, final Entity deliverable) {
        return new Runnable() {
            @Override
            public void run() {
                final Message state = deliverable.getState();
                final Any packedState = AnyPacker.pack(state);
                consumer.update(deliverable.getId(), packedState, deliverable.getVersion());
            }
        };
    }

    @Override
    protected Collection<Stand> consumersFor(Entity deliverable) {
        return Lists.newArrayList(stand);
    }

    /**
     * Returns an instance of {@code StandUpdateDelivery} which does NOT postpone any state
     * update propagation and uses the specified {@code executor} for the operation.
     *
     * @param executor an instance of {@code Executor} to use for the delivery
     * @return the instance of {@code StandUpdateDelivery} with the given executor
     */
    public static StandUpdateDelivery immediateDeliveryWithExecutor(Executor executor) {
        final StandUpdateDelivery immediateDelivery = new StandUpdateDelivery(executor) {
            @Override
            protected boolean shouldPostponeDelivery(Entity deliverable, Stand consumer) {
                return false;
            }
        };
        return immediateDelivery;
    }

    public static StandUpdateDelivery directDelivery() {
        return PredefinedDeliveryStrategies.DIRECT_DELIVERY;
    }

    /** Utility wrapper class for predefined delivery strategies designed to be constants. */
    private static final class PredefinedDeliveryStrategies {

        /**
         * A pre-defined instance of the {@code StandUpdateDelivery}, which does not postpone any
         * update delivery and uses a default executor for the operation.
         */
        private static final StandUpdateDelivery DIRECT_DELIVERY = new StandUpdateDelivery() {

            @Override
            protected boolean shouldPostponeDelivery(Entity deliverable, Stand consumer) {
                return false;
            }
        };
    }
}
