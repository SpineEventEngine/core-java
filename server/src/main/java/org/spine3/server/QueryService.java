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
package org.spine3.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.client.Query;
import org.spine3.client.QueryResponse;
import org.spine3.client.grpc.QueryServiceGrpc;
import org.spine3.protobuf.KnownTypes;
import org.spine3.protobuf.TypeUrl;
import org.spine3.type.ClassName;

import java.util.Set;

/**
 * The {@code QueryService} provides a synchronous way to fetch read-side state from the server.
 *
 * <p> For asynchronous read-side updates please see {@code SubscriptionService}.
 *
 * @author Alex Tymchenko
 */
public class QueryService
        extends QueryServiceGrpc.QueryServiceImplBase {

    private final ImmutableMap<TypeUrl, BoundedContext> typeToContextMap;

    private QueryService(Builder builder) {
        this.typeToContextMap = builder.getBoundedContextMap();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @SuppressWarnings("RefusedBequest") // as we override default implementation with `unimplemented` status.
    @Override
    public void read(Query request, StreamObserver<QueryResponse> responseObserver) {
        final String typeAsString = request.getTarget()
                                           .getType();

        // TODO[alex.tymchenko]: too complex  
        final ClassName typeClassName = ClassName.of(typeAsString);
        final TypeUrl type = KnownTypes.getTypeUrl(typeClassName);
        final BoundedContext boundedContext = typeToContextMap.get(type);

        try {
            boundedContext.getStand()
                          .execute(request, responseObserver);

        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            log().error("Error processing query", e);
            responseObserver.onError(e);
        }
    }

    public static class Builder {
        private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
        private ImmutableMap<TypeUrl, BoundedContext> typeToContextMap;


        public Builder addBoundedContext(BoundedContext boundedContext) {
            // Save it to a temporary set so that it is easy to remove it if needed.
            boundedContexts.add(boundedContext);
            return this;
        }

        public Builder removeBoundedContext(BoundedContext boundedContext) {
            boundedContexts.remove(boundedContext);
            return this;
        }


        @SuppressWarnings("ReturnOfCollectionOrArrayField") // the collection returned is immutable
        public ImmutableMap<TypeUrl, BoundedContext> getBoundedContextMap() {
            return typeToContextMap;
        }

        /**
         * Builds the {@link QueryService}.
         */
        public QueryService build() {
            this.typeToContextMap = createBoundedContextMap();
            final QueryService result = new QueryService(this);
            return result;
        }

        private ImmutableMap<TypeUrl, BoundedContext> createBoundedContextMap() {
            final ImmutableMap.Builder<TypeUrl, BoundedContext> builder = ImmutableMap.builder();
            for (BoundedContext boundedContext : boundedContexts) {
                addBoundedContext(builder, boundedContext);
            }
            return builder.build();
        }

        private static void addBoundedContext(ImmutableMap.Builder<TypeUrl, BoundedContext> mapBuilder,
                BoundedContext boundedContext) {

            final ImmutableSet<TypeUrl> availableTypes = boundedContext.getStand()
                                                                       .getAvailableTypes();

            for (TypeUrl availableType : availableTypes) {
                mapBuilder.put(availableType, boundedContext);
            }
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(QueryService.class);
    }

}
