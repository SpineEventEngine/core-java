/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.Command;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows to post a command optionally subscribing to events that are immediate results
 * of handling this command.
 *
 * <p>Usage example:
 * <pre>{@code
 * Subscription loginSubscription =
 *     client.asGuest()
 *           .command(logInUser)
 *           .observe(UserLoggedIn.class, (event, context) -> { ... })
 *           .observe(UserAlreadyLoggedIn.class, (rejection, context) -> { ... })
 *           .onStreamingError((throwable) -> { ... })
 *           .post();
 * }</pre>
 *
 * <p>The subscription obtained from the {@link #post()} should must be cancelled to preserve
 * both client-side and server-side resources. The moment of cancelling the subscriptions
 * depends on the nature of the posted command and the outcome expected by the client application.
 */
public final class CommandRequest extends ClientRequest {

    private final CommandMessage message;
    private final MultiEventConsumers.Builder builder;
    private @Nullable ErrorHandler streamingErrorHandler;

    CommandRequest(ClientRequest parent, CommandMessage c) {
        super(parent);
        this.message = c;
        this.builder = MultiEventConsumers.newBuilder();
    }

    /**
     * Adds the passed consumer to the subscribers of the event of the passed type.
     *
     * @param type
     *         the type of the event message to be received by the consumer
     * @param consumer
     *         the consumer
     * @param <E>
     *         the type of the event
     */
    @CanIgnoreReturnValue
    public <E extends EventMessage> CommandRequest
    observe(Class<E> type, Consumer<E> consumer) {
        checkNotNull(consumer);
        builder.observe(type, consumer);
        return this;
    }

    /**
     * Adds the passed event consumer to the subscribers of the event of the passed type.
     *
     * @param type
     *         the type of the event message to be received by the consumer
     * @param consumer
     *         the consumer of the event message and its context
     * @param <E>
     *         the type of the event
     */
    @CanIgnoreReturnValue
    public <E extends EventMessage> CommandRequest
    observe(Class<E> type, EventConsumer<E> consumer) {
        checkNotNull(consumer);
        builder.observe(type, consumer);
        return this;
    }

    /**
     * Assigns a handler for errors occurred when delivering events.
     *
     * <p>If such an error occur, no more events resulting from the posted command will be
     * delivered to the consumers.
     */
    @CanIgnoreReturnValue
    public CommandRequest onStreamingError(ErrorHandler handler) {
        this.streamingErrorHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Subscribes the consumers to events to receive events resulting from the command as
     * they happen, then sends the command to the server.
     *
     * <p>The returned {@code Subscription} instances should be
     * {@linkplain Client#cancel(Subscription) canceled} after the requesting code receives
     * expected events, or after a reasonable timeout.
     *
     * @return subscription to the events
     * @implNote Subscriptions should be cancelled to free up client and server resources
     *         required for their maintenance. It is not possible to cancel the returned
     *         subscription in an automatic way because of the following.
     *         Subscriptions by nature are asynchronous and infinite requests.
     *         Even that we know expected types of the events produced by the command, only the
     *         client code "knows" how many of them it expects. Also, some events may not arrive
     *         because of communication or business logic reasons. That's why the returned
     *         subscriptions should be cancelled by the client code when it no longer needs it.
     */
    public ImmutableSet<Subscription> post() {
        MultiEventConsumers consumers = builder.build();
        Client client = client();
        Command command =
                client.requestOf(user())
                      .command()
                      .create(message);
        ImmutableSet<Subscription> result =
                EventsAfterCommand.subscribe(client, command, consumers, streamingErrorHandler);
        client().post(command);
        return result;
    }

    @VisibleForTesting
    CommandMessage message() {
        return message;
    }
}
