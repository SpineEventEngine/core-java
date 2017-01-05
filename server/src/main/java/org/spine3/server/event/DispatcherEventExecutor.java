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
import org.spine3.SPI;
import org.spine3.base.Event;
import org.spine3.server.event.EventBus.DispatcherProvider;
import org.spine3.server.type.EventClass;

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Delivers the {@code Event}s from the {@link EventBus} to the matching {@link EventDispatcher}s.
 *
 * @author Alex Tymchenko
 */
@SPI
@SuppressWarnings("WeakerAccess")   // Part of API.
public abstract class DispatcherEventExecutor extends EventExecutor {

    private DispatcherProvider dispatcherProvider;

    /**
     * Creates an instance of event executor with an {@link Executor} used for event dispatching.
     *
     * @param delegate the instance of {@code Executor} used to dispatch events.
     */
    protected DispatcherEventExecutor(Executor delegate) {
        super(delegate);
    }

    /**
     * Creates an instance of event executor with a {@link MoreExecutors#directExecutor()} used for event dispatching.
     *
     */
    protected DispatcherEventExecutor() {
        super(MoreExecutors.directExecutor());
    }

    /**
     * Postpones the event dispatching if applicable to the given {@code event} and {@code dispatcher}.
     *
     * <p>This method should be implemented in descendants to define whether the event dispatching is postponed.
     *
     * @param event      the event to potentially postpone
     * @param dispatcher the target dispatcher for the event
     * @return {@code true}, if the event dispatching should be postponed, {@code false} otherwise.
     */
    protected abstract boolean maybePostponeDispatch(Event event, EventDispatcher dispatcher);

    /**
     * Dispatches the event immediately.
     *
     * <p>Typically used to dispatch the previously postponed events.
     *
     * <p>As the execution context may be lost since then, this method uses to specified dispatcher {@code class}
     * to choose which {@link EventDispatcher#dispatch(Event)} should be invoked.
     *
     * @param event           the event to dispatch
     * @param dispatcherClass the class of the target dispatcher
     */
    public void dispatchNow(final Event event, final Class<? extends EventDispatcher> dispatcherClass) {
        final Set<EventDispatcher> dispatchers = dispatchersFor(event);
        final Iterable<EventDispatcher> matching = Iterables.filter(dispatchers, matchClass(dispatcherClass));

        for (final EventDispatcher eventDispatcher : matching) {
            execute(new Runnable() {
                @Override
                public void run() {
                    eventDispatcher.dispatch(event);
                }
            });
        }
    }

    /**
     * Passes the event to the matching dispatchers and invokes {@link EventDispatcher#dispatch(Event)} using
     * the {@code executor} configured for this instance of {@code DispatcherEventExecutor}.
     *
     * <p>The dispatching of the event to each of the dispatchers may be postponed according to
     * {@link #maybePostponeDispatch(Event, EventDispatcher)} invocation result.
     *
     * @param event the event to dispatch
     */
    /* package */ void dispatch(Event event) {
        final Set<EventDispatcher> eventDispatchers = dispatchersFor(event);
        for (EventDispatcher dispatcher : eventDispatchers) {
            final boolean shouldPostpone = maybePostponeDispatch(event, dispatcher);
            if (!shouldPostpone) {
                dispatchNow(event, dispatcher.getClass());
            }
        }
    }

    /**
     * Obtains a pre-defined instance of the {@code DispatcherEventExecutor}, which does NOT postpone any
     * event dispatching and uses {@link MoreExecutors#directExecutor()} for operation.
     *
     * @return the pre-configured default executor.
     */
    public static DispatcherEventExecutor directExecutor() {
        return PredefinedExecutors.DIRECT_EXECUTOR;
    }

    /** Used by the instance of {@link EventBus} to inject the knowledge about up-to-date event dispatchers */
    /* package */ void setDispatcherProvider(DispatcherProvider dispatcherProvider) {
        this.dispatcherProvider = dispatcherProvider;
    }

    private Set<EventDispatcher> dispatchersFor(Event event) {
        final EventClass eventClass = EventClass.of(event);
        return dispatcherProvider.apply(eventClass);
    }

    /** Utility wrapper class for predefined executors designed to be constants */
    private static class PredefinedExecutors {

        /**
         * A pre-defined instance of the {@code DispatcherEventExecutor}, which does not postpone any event dispatching
         * and uses {@link MoreExecutors#directExecutor()} for operation.
         */
        private static final DispatcherEventExecutor DIRECT_EXECUTOR = new DispatcherEventExecutor() {
            @Override
            public boolean maybePostponeDispatch(Event event, EventDispatcher dispatcher) {
                return false;
            }
        };
    }
}
