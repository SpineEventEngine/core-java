/*
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
 */
package org.spine3.server.event;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.spine3.Internal;
import org.spine3.base.Event;
import org.spine3.server.type.EventClass;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base functionality for the routines delivering the {@link org.spine3.base.Event}s to event consumers,
 * such as {@link EventDispatcher}s or {@link EventSubscriber}s.
 *
 * @param <C> the type of the consumer
 * @author Alex Tymchenko
 */
@Internal
abstract class EventDelivery<C> {

    private final Executor delegate;
    private Function<EventClass, ? extends Collection<C>> consumerProvider;

    /**
     * Creates an event delivery strategy with a specified {@link Executor} used for the operation.
     */
    EventDelivery(Executor delegate) {
        this.delegate = delegate;
    }

    /**
     * Determines whether the event delivery should be postponed for the given {@code event} and {@code consumer}.
     *
     * <p>This method must be implemented in descendants.
     *
     * @param event    the event which delivery may potentially be postponed
     * @param consumer the target consumer for the event
     * @return {@code true}, if the event delivery should be postponed, {@code false} otherwise.
     */
    protected abstract boolean shouldPostponeDelivery(Event event, C consumer);

    /**
     * Defines what should be done to deliver a given {@code event} to the {@code targetConsumer}.
     *
     * <p>Each {@code EventDelivery} implementation defines this behaviour depending on the type of the consumer.
     *
     * <p>The result is defined as {@link Runnable}, as this action will be executed by a delegate {@code Executor},
     * according to the strategy implementation regulated by {@link #shouldPostponeDelivery(Event, Object)} and
     * {@link #deliverNow(Event, Class)} methods.
     *
     * @param consumer the consumer to deliver the event
     * @param event    the event to be delivered
     * @return the action on how to deliver the given event to the consumer.
     */
    protected abstract Runnable getDeliveryAction(C consumer, Event event);

    /**
     * Passes the event to the matching consumers using the {@code executor} configured for this instance
     * of {@code EventDelivery}.
     *
     * <p>The delivery of the event to each of the consumers may be postponed according to
     * {@link #shouldPostponeDelivery(Event, Object)} invocation result.
     *
     * @param event the event to deliver
     */
    void deliver(Event event) {
        final Collection<C> consumers = consumersFor(event);
        for (C consumer : consumers) {
            final boolean shouldPostpone = shouldPostponeDelivery(event, consumer);
            if (!shouldPostpone) {
                deliverNow(event, consumer.getClass());
            }
        }
    }

    /** Used by the instance of {@link EventBus} to inject the knowledge about up-to-date consumers for the event */
    void setConsumerProvider(Function<EventClass, ? extends Collection<C>> consumerProvider) {
        this.consumerProvider = consumerProvider;
    }

    /**
     * Delivers the event immediately.
     *
     * @param event         the event, packaged for the delivery
     * @param consumerClass the class of the target consumer
     */
    @SuppressWarnings("WeakerAccess")       // Part of API.
    public void deliverNow(final Event event, final Class<?> consumerClass) {
        final Collection<C> consumers = consumersFor(event);
        final Iterable<C> matching = Iterables.filter(consumers, matchClass(consumerClass));

        for (final C targetConsumer : matching) {
            final Runnable deliveryAction = getDeliveryAction(targetConsumer, event);
            execute(deliveryAction);
        }
    }

    private Collection<C> consumersFor(Event event) {
        final EventClass eventClass = EventClass.of(event);
        return consumerProvider.apply(eventClass);
    }

    private void execute(Runnable command) {
        delegate.execute(command);
    }

    private static <T> Predicate<T> matchClass(final Class<? extends T> classToMatch) {
        return new Predicate<T>() {
            @Override
            public boolean apply(@Nullable T input) {
                checkNotNull(input);
                return classToMatch.equals(input.getClass());
            }
        };
    }
}
