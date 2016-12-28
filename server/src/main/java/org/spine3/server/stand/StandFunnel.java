/*
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
 */
package org.spine3.server.stand;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import org.spine3.Internal;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.command.CommandBus;
import org.spine3.server.entity.Entity;
import org.spine3.server.event.EventBus;
import org.spine3.server.projection.ProjectionRepository;

import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Delivers the latest {@link Entity} states from the entity repositories to the {@link Stand}.
 *
 * <p>Note: Unlike {@link EventBus} and {@link CommandBus}, which assume many publishers and many subscribers,
 * the funnel may have zero or more publishers (typically, instances of {@link AggregateRepository} or
 * {@link ProjectionRepository}), but the only subscriber, the instance of {@code Stand}.
 *
 * <p>In scope of a single {@link BoundedContext} there can be the only instance of {@code StandFunnel}.
 *
 * @author Alex Tymchenko
 * @see AggregateRepository#dispatch(Command)
 * @see ProjectionRepository#dispatch(Event)
 */
@Internal
public class StandFunnel {

    /**
     * The instance of {@link Stand} to deliver the {@code Entity} state updates to.
     */
    private final Stand stand;

    /**
     * An {@link Executor} used for execution of data delivery methods.
     */
    private final Executor executor;

    private StandFunnel(Builder builder) {
        this.stand = builder.getStand();
        this.executor = builder.getExecutor();
    }

    /**
     * Post the state of an {@link Entity} to an instance of {@link Stand}.
     *
     * <p>The state data is posted as {@link Any} to allow transferring over the network.
     *
     * @param id            the id of an entity
     * @param entityState   the state of an {@code Entity}
     * @param entityVersion the version of an {@code Entity}
     */
    public void post(final Object id, final Any entityState, final int entityVersion) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                stand.update(id, entityState, entityVersion);
            }
        });
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
         * Optional {@code Executor} for delivering the data to {@code Stand}.
         *
         * <p>If not set, a {@link MoreExecutors#directExecutor()} value will be set by the builder.
         */
        private Executor executor;

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

        public Executor getExecutor() {
            return executor;
        }

        /**
         * Set the {@code Executor} instance for this {@code StandFunnel}.
         *
         * <p>The value must not be {@code null}.
         *
         * <p> If this method is not used, a {@link MoreExecutors#directExecutor()} value will be used.
         *
         * @param executor the instance of {@code Executor}.
         * @return {@code this} instance of {@code Builder}
         */
        public Builder setExecutor(Executor executor) {
            this.executor = checkNotNull(executor);
            return this;
        }

        public StandFunnel build() {
            checkState(stand != null, "Stand must be defined for the funnel");

            if (executor == null) {
                executor = MoreExecutors.directExecutor();
            }

            final StandFunnel result = new StandFunnel(this);
            return result;
        }
    }
}
