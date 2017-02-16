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
package org.spine3.server.stand;

import org.spine3.Internal;
import org.spine3.server.entity.VersionableEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Delivers the latest {@link VersionableEntity} states from the entity repositories to the {@link Stand}.
 *
 * <p><strong>Note:</strong> Unlike {@link org.spine3.server.event.EventBus EventBus} and
 * {@link org.spine3.server.command.CommandBus CommandBus}, which assume many publishers and
 * many subscribers, the funnel may have zero or more publishers (typically, instances of
 * {@link org.spine3.server.aggregate.AggregateRepository AggregateRepository} or
 * {@link org.spine3.server.projection.ProjectionRepository ProjectionRepository}),
 * but the only subscriber, the instance of {@code Stand}.
 *
 * <p>In scope of a single {@link org.spine3.server.BoundedContext BoundedContext}
 * there can be the only instance of {@code StandFunnel}.
 *
 * @author Alex Tymchenko
 * @see org.spine3.server.aggregate.AggregateRepository#dispatch(org.spine3.base.Command)
 *      AggregateRepository.dispatch(Command)
 * @see org.spine3.server.projection.ProjectionRepository#dispatch(org.spine3.base.Event)
 *      ProjectionRepository.dispatch(Event)
 */
@Internal
public class StandFunnel {

    /**
     * The delivery strategy to propagate the {@code Entity} state to the instance of {@code Stand}.
     */
    private final StandUpdateDelivery delivery;

    private StandFunnel(Builder builder) {
        this.delivery = builder.getDelivery();
        this.delivery.setStand(builder.getStand());
    }

    /**
     * Post the state of an {@link VersionableEntity} to an instance of {@link Stand}.
     *
     * @param entity the entity which state should be delivered to the {@code Stand}
     */
    public void post(VersionableEntity entity) {
        delivery.deliver(entity);
    }

    /**
     * Create a new {@code Builder} for {@link StandFunnel}.
     *
     * @return a new {@code Builder} instance.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        /**
         * The target {@code Stand} to deliver the {@code Entity} updates to.
         */
        private Stand stand;

        /**
         * Optional {@code StandUpdateDelivery} for propagating the data to {@code Stand}.
         *
         * <p>If not set, a {@link StandUpdateDelivery#directDelivery() directDelivery()}
         * value will be set by the builder.
         */
        private StandUpdateDelivery delivery;

        public Stand getStand() {
            return stand;
        }

        /**
         * Set the {@link Stand} instance for this {@code StandFunnel}.
         *
         * <p> The value must not be null.
         *
         * @param stand the instance of {@link Stand}.
         * @return {@code this} instance of {@code Builder}
         */
        public Builder setStand(Stand stand) {
            this.stand = checkNotNull(stand);
            return this;
        }

        public StandUpdateDelivery getDelivery() {
            return delivery;
        }

        /**
         * Set the {@code StandUpdateDelivery} instance for this {@code StandFunnel}.
         *
         * <p>The value must not be {@code null}.
         *
         * <p> If this method is not used, a
         * {@link StandUpdateDelivery#directDelivery() directDelivery()} value will be used.
         *
         * @param delivery the instance of {@code StandUpdateDelivery}.
         * @return {@code this} instance of {@code Builder}
         */
        public Builder setDelivery(StandUpdateDelivery delivery) {
            this.delivery = checkNotNull(delivery);
            return this;
        }

        public StandFunnel build() {
            checkState(stand != null,
                       "Stand must be defined for the funnel");

            if (delivery == null) {
                delivery = StandUpdateDelivery.directDelivery();
            }

            final StandFunnel result = new StandFunnel(this);
            return result;
        }
    }
}
