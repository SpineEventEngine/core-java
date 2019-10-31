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

package io.spine.client;

import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.UserId;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * Entry point for creating client requests.
 *
 * <p>An instance of this class is obtained via
 * {@link Client#onBehalfOf(UserId)} or {@link Client#asGuest()} methods and then used for creating
 * a specific client request e.g. for {@linkplain ClientRequest#command(CommandMessage) posting
 * a command} or {@linkplain ClientRequest#select(Class) running a query}.
 *
 * <p>A client request is customizing using fluent API, which is provided by the derived classes.
 *
 * @see Client
 */
@SuppressWarnings("ClassReferencesSubclass")
// we want to have DSL for calls encapsulated in this class.
public class ClientRequest {

    private final UserId user;
    private final Client client;

    ClientRequest(UserId user, Client client) {
        checkNotDefaultArg(user);
        this.user = user;
        this.client = checkNotNull(client);
    }

    ClientRequest(ClientRequest parent) {
        this(parent.user, parent.client);
    }

    /**
     * Creates a builder for customizing command request.
     */
    public CommandRequest command(CommandMessage c) {
        return new CommandRequest(this, c);
    }

    /**
     * Creates a builder for customizing subscription for the passed entity state type.
     */
    public <M extends Message> SubscriptionRequest subscribeTo(Class<M> type) {
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
     * Creates a builder for constructing a query for messages of the specified type.
     */
    public <M extends Message> QueryRequest<M> select(Class<M> type) {
        return new QueryRequest<>(this, type);
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
}
