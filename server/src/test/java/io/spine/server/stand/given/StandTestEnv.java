/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.server.Given;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.stand.Stand;
import io.spine.server.stand.given.Given.StandTestProjectionRepository;
import io.spine.system.server.NoOpSystemGateway;
import io.spine.system.server.SystemGateway;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Kuzmin
 */
public class StandTestEnv {

    /** Prevents instantiation of this utility class. */
    private StandTestEnv() {
    }

    public static Stand standWithStates(Collection<Any> states, boolean multitenant) {
        SystemGateway gateway = new FakeSystemGateway(states);
        Stand stand = Stand
                .newBuilder()
                .setMultitenant(multitenant)
                .setSystemGateway(gateway)
                .build();
        return stand;
    }

    public static Stand newStand(boolean multitenant) {
        Stand stand = Stand
                .newBuilder()
                .setMultitenant(multitenant)
                .setSystemGateway(NoOpSystemGateway.INSTANCE)
                .build();
        CustomerAggregateRepository customerAggregateRepo = new CustomerAggregateRepository();
        stand.registerTypeSupplier(customerAggregateRepo);
        StandTestProjectionRepository projectProjectionRepo = new StandTestProjectionRepository();
        stand.registerTypeSupplier(projectProjectionRepo);
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

    public static class MemoizeEntityUpdateCallback implements Stand.EntityUpdateCallback {

        private Any newEntityState = null;

        @Override
        public void onStateChanged(EntityStateUpdate newEntityState) {
            this.newEntityState = newEntityState.getState();
        }

        public @Nullable Any newEntityState() {
            return newEntityState;
        }
    }
}
