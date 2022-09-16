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

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.grpc.QueryServiceGrpc;
import io.spine.logging.Logging;
import io.spine.protobuf.Messages;
import io.spine.server.model.UnknownEntityStateTypeException;
import io.spine.server.stand.InvalidRequestException;
import io.spine.type.TypeUrl;
import io.spine.type.UnpublishedLanguageException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.flogger.LazyArgs.lazy;
import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.server.transport.Statuses.invalidArgumentWithCause;
import static io.spine.type.MessageExtensions.isInternal;

/**
 * The {@code QueryService} provides a synchronous way to fetch read-side state from the server.
 *
 * <p>For asynchronous read-side updates please see {@link SubscriptionService}.
 */
public final class QueryService
        extends QueryServiceGrpc.QueryServiceImplBase
        implements Logging {

    private final QueryServiceImpl impl;

    private QueryService(TypeDictionary types) {
        super();
        this.impl = new QueryServiceImpl(this, types);
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
    public void read(Query query, StreamObserver<QueryResponse> observer) {
        _debug().log("Incoming query: `%s`.", lazy(() -> shortDebugString(query)));
        impl.serve(query, observer);
    }

    private static final class QueryServiceImpl extends ServiceDelegate<Query, QueryResponse> {

        QueryServiceImpl(BindableService service, TypeDictionary types) {
            super(service, types);
        }

        @Override
        protected TypeUrl enclosedMessageType(Query request) {
            return request.targetType();
        }

        @Override
        protected void serve(BoundedContext context,
                             Query query,
                             StreamObserver<QueryResponse> observer) {
            try {
                var stand = context.stand();
                stand.execute(query, observer);
            } catch (InvalidRequestException e) {
                _error().log("Invalid request. `%s`", e.asError());
                var exception = invalidArgumentWithCause(e);
                observer.onError(exception);
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
                _error().withCause(e)
                        .log("Error processing query.");
                observer.onError(e);
            }
        }

        @Override
        protected boolean detectInternal(Query query) {
            var msgClass = enclosedMessageType(query).getMessageClass();
            var result = isInternal(msgClass);
            return result;
        }

        @Override
        protected void handleInternal(Query request, StreamObserver<QueryResponse> observer) {
            var targetType = enclosedMessageType(request);
            var defTarget = Messages.defaultInstance(targetType.getMessageClass());
            var unpublishedLanguage = new UnpublishedLanguageException(defTarget);
            _error().withCause(unpublishedLanguage)
                    .log("A query to an unpublished type posted to `%s`.", serviceName());
            observer.onError(unpublishedLanguage);
        }

        @Override
        protected void serveAllContexts(Query query, StreamObserver<QueryResponse> observer) {
            var exception = new UnknownEntityStateTypeException(query.targetType());
            _error().withCause(exception)
                    .log();
            observer.onError(exception);
        }
    }

    /**
     * The builder for a {@code QueryService}.
     */
    public static class Builder extends AbstractServiceBuilder<QueryService, Builder> {

        /**
         * Builds the {@link QueryService}.
         *
         * @throws IllegalStateException if no bounded contexts were added.
         */
        @Override
        public QueryService build() throws IllegalStateException {
            var dictionary = TypeDictionary.newBuilder();
            contexts().forEach(
                    context -> dictionary.putAll(context, (c) -> c.stand().exposedTypes())
            );

            var service = new QueryService(dictionary.build());
            warnIfEmpty(service);
            return service;
        }

        @Override
        Builder self() {
            return this;
        }
    }
}
