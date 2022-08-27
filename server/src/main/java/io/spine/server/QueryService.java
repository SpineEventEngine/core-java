/*
 * Copyright 2022, TeamDev. All rights reserved.
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
package io.spine.server;

import com.google.common.collect.ImmutableMap;
import io.grpc.stub.StreamObserver;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.grpc.QueryServiceGrpc;
import io.spine.logging.Logging;
import io.spine.server.model.UnknownEntityTypeException;
import io.spine.server.stand.InvalidRequestException;
import io.spine.type.TypeUrl;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.flogger.LazyArgs.lazy;
import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.server.transport.Statuses.invalidArgumentWithCause;

/**
 * The {@code QueryService} provides a synchronous way to fetch read-side state from the server.
 *
 * <p>For asynchronous read-side updates please see {@link SubscriptionService}.
 */
public final class QueryService
        extends QueryServiceGrpc.QueryServiceImplBase
        implements Logging {

    private final ImmutableMap<TypeUrl, BoundedContext> typeToContextMap;

    private QueryService(Map<TypeUrl, BoundedContext> map) {
        super();
        this.typeToContextMap = ImmutableMap.copyOf(map);
    }

    /**
     * Creates a new builder for the service.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builds the service with a single Bounded Context.
     */
    public static QueryService withSingle(BoundedContext context) {
        checkNotNull(context);
        var result = newBuilder().add(context).build();
        return result;
    }

    /**
     * Executes the passed query returning results to the passed observer.
     */
    @Override
    public void read(Query query, StreamObserver<QueryResponse> responseObserver) {
        _debug().log("Incoming query: `%s`.", lazy(() -> shortDebugString(query)));

        var type = query.targetType();
        var context = typeToContextMap.get(type);
        if (context == null) {
            handleUnsupported(type, responseObserver);
        } else {
            handleQuery(context, query, responseObserver);
        }
    }

    private void handleQuery(BoundedContext context,
                             Query query,
                             StreamObserver<QueryResponse> responseObserver) {
        var stand = context.stand();
        try {
            stand.execute(query, responseObserver);
        } catch (InvalidRequestException e) {
            _error().log("Invalid request. `%s`", e.asError());
            var exception = invalidArgumentWithCause(e);
            responseObserver.onError(exception);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error processing query.");
            responseObserver.onError(e);
        }
    }

    private void handleUnsupported(TypeUrl type, StreamObserver<QueryResponse> observer) {
        var exception = new UnknownEntityTypeException(type);
        _error().withCause(exception)
                .log("Unknown type encountered.");
        observer.onError(exception);
    }

    /**
     * The builder for a {@code QueryService}.
     */
    public static class Builder extends AbstractServiceBuilder<QueryService, Builder> {

        @Override
        Builder self() {
            return this;
        }

        /**
         * Builds the {@link QueryService}.
         *
         * @throws IllegalStateException if no bounded contexts were added.
         */
        @Override
        public QueryService build() throws IllegalStateException {
            if (isEmpty()) {
                var message = "Query service must have at least one `BoundedContext`.";
                throw new IllegalStateException(message);
            }
            var map = createMap();
            var result = new QueryService(map);
            return result;
        }

        private ImmutableMap<TypeUrl, BoundedContext> createMap() {
            ImmutableMap.Builder<TypeUrl, BoundedContext> map = ImmutableMap.builder();
            for (var context : contexts()) {
                putExposedTypes(context, map);
            }
            return map.build();
        }

        private static void putExposedTypes(BoundedContext context,
                                            ImmutableMap.Builder<TypeUrl, BoundedContext> map) {
            var stand = context.stand();
            var exposedTypes = stand.exposedTypes();
            for (var type : exposedTypes) {
                map.put(type, context);
            }
        }
    }
}
