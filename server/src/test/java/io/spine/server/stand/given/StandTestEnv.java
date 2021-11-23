/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.stand.given;

import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import io.spine.client.EntityStateUpdate;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.SubscriptionUpdate;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.command.CommandHandler;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.Repository;
import io.spine.server.stand.Stand;
import io.spine.server.stand.SubscriptionCallback;
import io.spine.server.stand.given.Given.StandTestProjectionRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StandTestEnv {

    /** Prevents instantiation of this utility class. */
    private StandTestEnv() {
    }

    public static Stand newStand(boolean multitenant) {
        return newStand(multitenant,
                        new CustomerAggregateRepository(), new StandTestProjectionRepository());
    }

    public static Stand newStand(boolean multitenant, Repository<?, ?>... repositories) {
        BoundedContextBuilder builder = BoundedContextBuilder.assumingTests(multitenant);
        Arrays.stream(repositories)
              .forEach(builder::add);
        BoundedContext context = builder.build();
        return context.stand();
    }

    public static Stand newStand(CommandDispatcher... dispatchers) {
        BoundedContextBuilder builder = BoundedContextBuilder.assumingTests();
        Arrays.stream(dispatchers)
              .forEach(builder::addCommandDispatcher);
        BoundedContext context = builder.build();
        return context.stand();
    }

    /**
     * A {@link StreamObserver} storing the state of {@link Query} execution.
     */
    public static class MemoizeQueryResponseObserver implements StreamObserver<QueryResponse> {

        private QueryResponse responseHandled;
        private Throwable throwable;
        private boolean isCompleted = false;

        @Override
        public void onNext(QueryResponse response) {
            this.responseHandled = response;
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.isCompleted = true;
        }

        public QueryResponse responseHandled() {
            return responseHandled;
        }

        public Throwable throwable() {
            return throwable;
        }

        public boolean isCompleted() {
            return isCompleted;
        }
    }

    /**
     * A subscription callback, which remembers the updates fed to it.
     */
    public static class MemoizeSubscriptionCallback implements SubscriptionCallback {

        private final List<SubscriptionUpdate> acceptedUpdates = new ArrayList<>();
        private @Nullable Any newEntityState = null;
        private @Nullable Event newEvent = null;

        /**
         * {@inheritDoc}
         *
         * <p>Currently there is always exactly one {@code EntityStateUpdate} in a
         * {@code SubscriptionUpdate}.
         */
        @Override
        public void accept(SubscriptionUpdate update) {
            acceptedUpdates.add(update);
            switch (update.getUpdateCase()) {
                case ENTITY_UPDATES:
                    EntityStateUpdate entityStateUpdate = update.getEntityUpdates()
                                                                .getUpdateList()
                                                                .get(0);
                    newEntityState = entityStateUpdate.getState();
                    break;
                case EVENT_UPDATES:
                    newEvent = update.getEventUpdates()
                                     .getEventList()
                                     .get(0);
                    break;
                default:
                    // Do nothing.
            }
        }

        public @Nullable Any newEntityState() {
            return newEntityState;
        }

        public @Nullable Event newEvent() {
            return newEvent;
        }

        public int countAcceptedUpdates() {
            return acceptedUpdates.size();
        }
    }
}
