/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.SPI;
import org.spine3.base.Event;
import org.spine3.base.EventContext;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;

import static org.spine3.base.Events.getMessage;

/**
 * Delivers the {@code Event}s from the {@link EventBus} to the matching {@link EventSubscriber}s.
 *
 * <p>If a subscriber method throws an exception (which in general should be avoided), the exception is logged.
 * No other processing occurs.
 *
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess")   // Part of API.
public abstract class SubscriberEventDelivery extends EventDelivery<EventSubscriber> {

    /**
     * Creates an instance of event executor with an {@link Executor} used to invoke the subscriber methods passing
     * the events to them.
     *
     * @param delegate the instance of {@code Executor} used to pass events to the subscribers.
     */
    protected SubscriberEventDelivery(Executor delegate) {
        super(delegate);
    }

    /**
     * Creates an instance of event executor with a {@link MoreExecutors#directExecutor()} used to invoke
     * the subscriber methods passing the events to them.
     */
    protected SubscriberEventDelivery() {
        super(MoreExecutors.directExecutor());
    }

    @Override
    protected Runnable getDeliveryAction(final EventSubscriber consumer, final Event event) {
        return new Runnable() {
            @Override
            public void run() {
                final Message message = getMessage(event);
                final EventContext context = event.getContext();
                try {
                    consumer.handle(message, context);
                } catch (InvocationTargetException e) {
                    handleSubscriberException(e, event, context);
                }
            }
        };
    }

    private static void handleSubscriberException(InvocationTargetException e,
                                                  Message eventMessage,
                                                  EventContext eventContext) {
        log().error("Exception handling event. Event message: {}, context: {}, cause: {}",
                    eventMessage, eventContext, e.getCause());
    }

    /**
     * Obtains a pre-defined instance of the {@code SubscriberEventDelivery}, which does NOT postpone any
     * event delivery and uses {@link MoreExecutors#directExecutor()} for operation.
     *
     * @return the pre-configured default executor.
     */
    public static SubscriberEventDelivery directDelivery() {
        return PredefinedDeliveryStrategies.DIRECT_DELIVERY;
    }

    /** Utility wrapper class for predefined executors designed to be constants. */
    private static final class PredefinedDeliveryStrategies {

        /**
         * A pre-defined instance of the {@code SubscriberEventDelivery}, which does not postpone any event delivery
         * and uses {@link MoreExecutors#directExecutor()} for operation.
         */
        private static final SubscriberEventDelivery DIRECT_DELIVERY = new SubscriberEventDelivery() {
            @Override
            protected boolean shouldPostponeDelivery(Event event, EventSubscriber subscriber) {
                return false;
            }
        };
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SubscriberEventDelivery.class);
    }

    protected static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
