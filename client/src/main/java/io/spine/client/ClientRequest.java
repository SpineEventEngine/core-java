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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
import io.spine.core.UserId;
import io.spine.query.EntityQuery;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * Entry point for creating client requests.
 *
 * <p>An instance of this class is obtained via
 * {@link Client#onBehalfOf(UserId)} or {@link Client#asGuest()} methods and then used for creating
 * a specific client request e.g. for {@linkplain ClientRequest#command(CommandMessage) posting
 * a command}.
 *
 * <p>A client request may be customized using fluent API provided by the deriving classes.
 *
 * <p>Some features such as running an {@link EntityQuery} are available
 * {@linkplain #run(EntityQuery) directly from this class}.
 *
 * @see Client
 */
@SuppressWarnings("ClassReferencesSubclass")
// we want to have DSL for calls encapsulated in this class.
public class ClientRequest {

    private final UserId user;
    private final Client client;

    private @Nullable ErrorHandler streamingErrorHandler;
    private @Nullable ServerErrorHandler serverErrorHandler;

    ClientRequest(UserId user, Client client) {
        checkNotDefaultArg(user);
        this.user = user;
        this.client = checkNotNull(client);
    }

    ClientRequest(ClientRequest parent) {
        this(parent.user, parent.client);
        this.streamingErrorHandler = parent.streamingErrorHandler;
        this.serverErrorHandler = parent.serverErrorHandler;
    }

    /**
     * Creates a builder for customizing command request.
     */
    public CommandRequest command(CommandMessage c) {
        checkNotNull(c);
        return new CommandRequest(this, c);
    }

    /**
     * Creates a builder for customizing subscription for the passed entity state type.
     */
    public <S extends EntityState<?>> SubscriptionRequest<S> subscribeTo(Class<S> type) {
        checkNotNull(type);
        return new SubscriptionRequest<>(this, type);
    }

    /**
     * Creates a builder for customizing subscription for the passed event type.
     */
    public <E extends EventMessage> EventSubscriptionRequest<E> subscribeToEvent(Class<E> type) {
        checkNotNull(type);
        return new EventSubscriptionRequest<>(this, type);
    }

    /**
     * Runs the {@link EntityQuery} and returns the matched entity states.
     *
     * <p>Usage example:
     * <pre>
     *
     * Customer.Query query = Customer.query()
     *              .id().in(westCoastCustomerIds())
     *              .type().is(CustomerType.PERMANENT)
     *              .discountPercent().is(10)
     *              .companySize().is(Company.Size.SMALL)
     *              .withMask(nameAddressAndEmail)
     *              .sortAscendingBy(name())
     *              .limit(20)
     *              .build();
     *{@literal ImmutableList<Customer> customers = client.onBehalfOf(currentUser).run(query);}
     * </pre>
     *
     * @param <S>
     *         the type of the entity state for which the query is run
     */
    public <S extends EntityState<?>> ImmutableList<S> run(EntityQuery<?, S, ?> query) {
        QueryRequest<S> request = new QueryRequest<>(this, query);
        ImmutableList<S> results = request.run();
        return results;
    }

    /**
     * Obtains the ID of the user of the request.
     */
    protected final UserId user() {
        return user;
    }

    /**
     * Obtains the client instance that will perform the request.
     */
    protected final Client client() {
        return client;
    }

    /**
     * Assigns a handler for errors occurred when delivering messages from the server.
     *
     * <p>If such an error occurs, no more results are expected from the server.
     */
    @CanIgnoreReturnValue
    @OverridingMethodsMustInvokeSuper
    public ClientRequest onStreamingError(ErrorHandler handler) {
        this.streamingErrorHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Assigns a handler for an error occurred on the server-side (such as validation error)
     * in response to posting a request.
     */
    @CanIgnoreReturnValue
    @OverridingMethodsMustInvokeSuper
    public ClientRequest onServerError(ServerErrorHandler handler) {
        checkNotNull(handler);
        this.serverErrorHandler = handler;
        return this;
    }

    final @Nullable ErrorHandler streamingErrorHandler() {
        return streamingErrorHandler;
    }

    final @Nullable ServerErrorHandler serverErrorHandler() {
        return serverErrorHandler;
    }
}
