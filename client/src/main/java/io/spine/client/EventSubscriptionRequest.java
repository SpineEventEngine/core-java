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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventContext;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Allows to subscribe to events using filtering conditions.
 *
 * <p>Similarly to subscriptions to entity states, event subscriptions may use filtering by
 * values of the proto types of subscribed messages:
 * <pre>{@code
 * clientRequest.subscribeToEvent(MyEventMessage.class)
 *              .where(eq("my_proto_field"), fieldValue)
 *              .observe((event, context) -> {...})
 *              .post();
 * }</pre>
 *
 * <p>In addition to regular filtering conditions, event subscription requests may also reference
 * fields of {@code "spine.core.EventContext"} using {@code "context."} notation. For example,
 * in order to filter events originate from commands of the given user, please use the following
 * code:
 * <pre>{@code
 * clientRequest.subscribeToEvent(MyEventMessage.class)
 *              .where(eq("context.past_message.actor_context.actor"), userId)
 *              .observe((event, context) -> {...})
 *              .post();
 * }</pre>
 *
 * @param <E>
 *         the type of the event messages
 */
public final class EventSubscriptionRequest<E extends EventMessage>
        extends SubscribingRequest<E, EventContext, Event, EventSubscriptionRequest<E>> {

    private final EventConsumers.Builder<E> consumers;

    EventSubscriptionRequest(ClientRequest parent, Class<E> type) {
        super(parent, type);
        this.consumers = EventConsumers.newBuilder();
    }

    @Override
    EventConsumers.Builder<E> consumers() {
        return consumers;
    }

    @Override
    MessageConsumer<E, EventContext> toMessageConsumer(Consumer<E> consumer) {
        return EventConsumer.from(consumer);
    }

    @CanIgnoreReturnValue
    public EventSubscriptionRequest<E> observe(EventConsumer<E> consumer) {
        consumers().add(consumer);
        return self();
    }

    @Override
    Function<ActorRequestFactory, TopicBuilder> builderFn() {
        return (factory) -> factory.topic().select(messageType());
    }

    @Override
    EventSubscriptionRequest<E> self() {
        return this;
    }
}
