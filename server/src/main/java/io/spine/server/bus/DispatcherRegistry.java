/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.bus;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * A registry of message dispatchers.
 *
 * @param <C> the type of the class of dispatched messages
 * @param <D> the type of the message dispatchers
 */
public abstract class DispatcherRegistry<C extends MessageClass<? extends Message>,
                                         E extends MessageEnvelope<?, ?, ?>,
                                         D extends MessageDispatcher<C, E, ?>> {

    /**
     * The map from a message class to one or more dispatchers of
     * messages of this class.
     *
     * <p>Some implementations like {@link io.spine.server.commandbus.CommandBus CommandBus}
     * will allow only one dispatcher per message class. This should be handled
     * when registering dispatchers.
     */
    private final Multimap<C, D> dispatchers = synchronizedMultimap(HashMultimap.create());

    public void register(D dispatcher) {
        checkDispatcher(dispatcher);
        Set<C> messageClasses = dispatcher.getMessageClasses();
        for (C messageClass : messageClasses) {
            dispatchers.put(messageClass, dispatcher);
        }
    }

    public void unregister(D dispatcher) {
        checkNotNull(dispatcher);
        checkNotEmpty(dispatcher);

        Set<C> messageClasses = dispatcher.getMessageClasses();
        for (C messageClass : messageClasses) {
            dispatchers.remove(messageClass, dispatcher);
        }
    }

    /**
     * Unregisters all dispatchers.
     */
    protected void unregisterAll() {
        dispatchers.clear();
    }

    /**
     * Obtains message classes from all registered dispatchers.
     */
    protected Set<C> getRegisteredMessageClasses() {
        return dispatchers.keySet();
    }

    /**
     * Obtains dispatchers for the passed message class.
     *
     * @param envelope
     *         the message envelope to find dispatchers for
     * @return a set of dispatchers or an empty set if no dispatchers are registered
     */
    protected Set<D> getDispatchers(E envelope) {
        checkNotNull(envelope);
        C messageClass = classOf(envelope);
        Set<D> dispatchers = this.dispatchers
                .get(messageClass)
                .stream()
                .filter(dispatcher -> dispatcher.canDispatch(envelope))
                .collect(toImmutableSet());
        return dispatchers;
    }

    /**
     * Obtains a single dispatcher (if available) for the passed message.
     *
     * @param envelope
     *         the message to find a dispatcher for
     * @throws IllegalStateException
     *         if more than one dispatcher is found
     * @apiNote This method must be called only for serving {@link UnicastBus}es.
     */
    protected Optional<? extends D> getDispatcher(E envelope) {
        checkNotNull(envelope);
        Set<D> dispatchers = getDispatchers(envelope);
        checkNotMoreThanOne(dispatchers, classOf(envelope));
        Optional<D> result = dispatchers.stream()
                                        .findFirst();
        return result;
    }

    /**
     * Obtains all the dispatchers for the passed message class.
     *
     * @param messageClass
     *         the target message class
     * @return all the registered dispatchers of the given class
     */
    protected Set<D> getDispatchersForType(C messageClass) {
        checkNotNull(messageClass);
        Collection<D> dispatchersForType = dispatchers.get(messageClass);
        return ImmutableSet.copyOf(dispatchersForType);
    }

    /**
     * Obtains a single dispatcher (if available) for the passed message.
     *
     * @param messageClass
     *         the class of the message to find a dispatcher for
     * @throws IllegalStateException
     *         if more than one dispatcher is found
     */
    protected Optional<? extends D> getDispatcherForType(C messageClass) {
        Collection<D> dispatchersOfClass = dispatchers.get(messageClass);
        checkNotMoreThanOne(dispatchersOfClass, messageClass);
        Optional<D> dispatcher = dispatchersOfClass.stream()
                                                   .findFirst();
        return dispatcher;
    }

    private void checkNotMoreThanOne(Collection<D> dispatchers, C messageClass) {
        int size = dispatchers.size();
        checkState(size <= 1,
                   "More than one (%s) dispatchers found for the message class `%s`.",
                   size, messageClass);
    }

    private C classOf(E envelope) {
        @SuppressWarnings("unchecked") // Logically valid.
                C messageClass = (C) envelope.getMessageClass();
        return messageClass;
    }

    /**
     * Ensures that the passed dispatcher is valid.
     *
     * <p>The passed dispatcher must {@linkplain MessageDispatcher#getMessageClasses() expose}
     * at least one message class.
     *
     * @param dispatcher
     *         the dispatcher to check
     * @throws IllegalArgumentException
     *         if the check is failed
     */
    protected void checkDispatcher(D dispatcher) throws IllegalArgumentException {
        checkNotNull(dispatcher);
        checkNotEmpty(dispatcher);
    }

    private static <D extends MessageDispatcher> void checkNotEmpty(D dispatcher) {
        Set<?> messageClasses = dispatcher.getMessageClasses();
        checkArgument(!messageClasses.isEmpty(),
                      "The dispatcher (%s) has empty message class set.",
                      dispatcher);
    }
}
