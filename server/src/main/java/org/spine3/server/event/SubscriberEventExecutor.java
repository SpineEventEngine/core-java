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

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.SPI;
import org.spine3.base.EventContext;
import org.spine3.server.event.EventBus.SubscriberProvider;
import org.spine3.server.type.EventClass;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.Executor;

/**
 *  Delivers the {@code Event}s from the {@link EventBus} to the matching {@link EventSubscriber}s.
 *
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess")   // Part of API.
public abstract class SubscriberEventExecutor extends EventExecutor {

    private SubscriberProvider subscriberProvider;

    /**
     * Creates an instance of event executor with an {@link Executor} used to invoke the subscriber methods passing
     * the events to them.
     *
     * @param delegate the instance of {@code Executor} used to pass events to the subscribers.
     */
    protected SubscriberEventExecutor(Executor delegate) {
        super(delegate);
    }

    /**
     * Creates an instance of event executor with a {@link MoreExecutors#directExecutor()} used to invoke
     * the subscriber methods passing the events to them.
     */
    protected SubscriberEventExecutor() {
        super(MoreExecutors.directExecutor());
    }

    /**
     * Postpones the event delivery if applicable to the given {@code event} and {@code subscriber}.
     *
     * <p>This method should be implemented in descendants to define whether the event delivery is postponed.
     *
     * @param event      the event which delivery may potentially be postponed
     * @param subscriber the target subscriber for the event
     * @return {@code true}, if the event delivery should be postponed, {@code false} otherwise.
     */
    protected abstract boolean maybePostponeDelivery(Message event, EventContext context, EventSubscriber subscriber);

    /**
     * Delivers the event immediately.
     *
     * <p>Typically used to deliver the previously postponed events.
     *
     * <p>As the execution context may be lost since then, this method uses to specified subscriber {@code class}
     * to choose which {@link EventSubscriber} should be chosen as a target.
     *
     * @param event           the event to deliver
     * @param context         the context of the event
     * @param subscriberClass the class of the target subscriber
     */
    public void deliverNow(final Message event, final EventContext context,
                           final Class<? extends EventSubscriber> subscriberClass) {

        final Collection<EventSubscriber> dispatchers = subscribersFor(event);
        final Iterable<EventSubscriber> matching = Iterables.filter(dispatchers, matchClass(subscriberClass));

        for (final EventSubscriber eventSubscriber : matching) {
            execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        eventSubscriber.handle(event, context);
                    } catch (InvocationTargetException e) {
                        handleSubscriberException(e, event, context);
                    }
                }
            });
        }
    }

    /**
     * Passes the event to the matching subscribers using the {@code executor} configured for this instance
     * of {@code DispatcherEventExecutor}.
     *
     * <p>The delivery of the event to each of the subscribers may be postponed according to
     * {@link #maybePostponeDelivery(Message, EventContext, EventSubscriber)} invocation result.
     *
     * @param event   the event to deliver
     * @param context the context of the event
     */
    /* package */ void deliverToSubscribers(Message event, EventContext context) {
        final Collection<EventSubscriber> subscribers = subscribersFor(event);
        for (EventSubscriber subscriber : subscribers) {
            final boolean shouldPostpone = maybePostponeDelivery(event, context, subscriber);
            if (!shouldPostpone) {
                deliverNow(event, context, subscriber.getClass());
            }
        }
    }

    private Collection<EventSubscriber> subscribersFor(Message event) {
        final EventClass eventClass = EventClass.of(event);
        return subscriberProvider.apply(eventClass);
    }

    /* package */ void setSubscriberProvider(SubscriberProvider subscriberProvider) {
        this.subscriberProvider = subscriberProvider;
    }

    private static void handleSubscriberException(InvocationTargetException e,
                                                  Message eventMessage,
                                                  EventContext eventContext) {
        log().error("Exception handling event. Event message: {}, context: {}, cause: {}",
                    eventMessage, eventContext, e.getCause());
    }

    /**
     * Obtains a pre-defined instance of the {@code SubscriberEventExecutor}, which does NOT postpone any
     * event delivery and uses {@link MoreExecutors#directExecutor()} for operation.
     *
     * @return the pre-configured default executor.
     */
    public static SubscriberEventExecutor directExecutor() {
        return PredefinedExecutors.DIRECT_EXECUTOR;
    }

    /** Utility wrapper class for predefined executors designed to be constants */
    private static class PredefinedExecutors {

        /**
         * A pre-defined instance of the {@code SubscriberEventExecutor}, which does not postpone any event delivery
         * and uses {@link MoreExecutors#directExecutor()} for operation.
         */
        private static final SubscriberEventExecutor DIRECT_EXECUTOR = new SubscriberEventExecutor() {
            @Override
            protected boolean maybePostponeDelivery(Message event, EventContext context, EventSubscriber subscriber) {
                return false;
            }
        };
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SubscriberEventExecutor.class);
    }

    protected static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
