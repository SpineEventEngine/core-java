/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.server.BoundedContext;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.entity.Repository;
import io.spine.server.stand.Stand;
import io.spine.server.stand.given.Given.StandTestProjectionRepository;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.system.server.NoOpSystemReadSide;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.core.BoundedContextNames.assumingTests;
import static io.spine.server.storage.StorageFactorySwitch.newInstance;

public class StandTestEnv {

    /** Prevents instantiation of this utility class. */
    private StandTestEnv() {
    }

    public static Stand newStand(boolean multitenant) {
        return newStand(multitenant,
                        new CustomerAggregateRepository(), new StandTestProjectionRepository());
    }

    @SuppressWarnings("unchecked") // Generic type matching issues. OK for tests.
    public static Stand newStand(boolean multitenant, Repository... repositories) {
        Stand.Builder standBuilder = Stand
                .newBuilder()
                .setMultitenant(multitenant)
                .setSystemReadSide(NoOpSystemReadSide.INSTANCE);
        BoundedContext boundedContext = BoundedContext
                .newBuilder()
                .setMultitenant(multitenant)
                .setStand(standBuilder)
                .build();
        Stand stand = boundedContext.getStand();
        for (Repository repository : repositories) {
            boundedContext.register(repository);
        }
        return stand;
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

    public static class MemoizeNotifySubscriptionAction implements Stand.NotifySubscriptionAction {

        private Any newEntityState = null;

        /**
         * {@inheritDoc}
         *
         * <p>Currently there is always exactly one {@code EntityStateUpdate} in a
         * {@code SubscriptionUpdate}.
         */
        @Override
        public void accept(SubscriptionUpdate update) {
            EntityStateUpdate entityStateUpdate = update.getEntityStateUpdatesList()
                                                        .get(0);
            newEntityState = entityStateUpdate.getState();
        }

        public @Nullable Any newEntityState() {
            return newEntityState;
        }
    }
}
