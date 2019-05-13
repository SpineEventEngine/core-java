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

package io.spine.server.event.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import io.spine.server.entity.model.StateClass;
import io.spine.server.event.EventReceiver;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.model.ModelClass;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.type.EventClass;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Helper object for storing information about methods and handlers of an
 * {@linkplain EventReceiverClass event receiving class}.
 *
 * @param <T>
 *         the type of target objects that handle messages
 * @param <P>
 *         the type of message classes produced by handler methods
 * @param <M>
 *         the type of handler method objects
 */
@Immutable(containerOf = "M")
public class EventReceivingClassDelegate<T extends EventReceiver,
                                         P extends MessageClass<?>,
                                         M extends HandlerMethod<?, EventClass, ?, P, ?>>
        extends ModelClass<T> {

    private static final long serialVersionUID = 0L;
    private final MessageHandlerMap<EventClass, P, M> handlers;
    private final ImmutableSet<EventClass> domesticEvents;
    private final ImmutableSet<EventClass> externalEvents;
    private final ImmutableSet<StateClass> domesticStates;
    private final ImmutableSet<StateClass> externalStates;

    /**
     * Creates new instance for the passed raw class with methods obtained
     * though the passed factory.
     */
    public EventReceivingClassDelegate(Class<T> delegatingClass, MethodSignature<M, ?> signature) {
        super(delegatingClass);
        this.handlers = MessageHandlerMap.create(delegatingClass, signature);
        this.domesticEvents = handlers.messageClasses(HandlerMethod::isDomestic);
        this.externalEvents = handlers.messageClasses(HandlerMethod::isExternal);
        this.domesticStates = extractStates(false);
        this.externalStates = extractStates(true);
    }

    public boolean contains(EventClass eventClass) {
        return handlers.containsClass(eventClass);
    }

    /**
     * Obtains domestic event classes handled by the delegating class.
     */
    public Set<EventClass> domesticEvents() {
        return domesticEvents;
    }

    /**
     * Obtains external event classes handled by the delegating class.
     */
    public Set<EventClass> externalEvents() {
        return externalEvents;
    }

    /**
     * Obtains domestic entity states to which the delegating class is subscribed.
     */
    public Set<StateClass> domesticStates() {
        return domesticStates;
    }

    /**
     * Obtains external entity states to which the delegating class is subscribed.
     */
    public Set<StateClass> externalStates() {
        return externalStates;
    }

    /**
     * Obtains the classes of messages produced by handler methods of this class.
     */
    public Set<P> producedTypes() {
        return handlers.producedTypes();
    }

    /**
     * Obtains the method which handles the passed event class.
     *
     * @throws IllegalStateException if there is such method in the class
     */
    public Collection<M> handlersOf(EventClass eventClass, MessageClass originClass) {
        return handlers.handlersOf(eventClass, originClass);
    }

    /**
     * Obtains the method which handles the passed event class.
     *
     * @throws IllegalStateException if there is such method in the class
     */
    public M handlerOf(EventClass eventClass, MessageClass originClass) {
        return handlers.handlerOf(eventClass, originClass);
    }

    /**
     * Obtains the classes of entity state messages from the passed handlers.
     */
    private ImmutableSet<StateClass> extractStates(boolean external) {
        EventClass updateEvent = StateClass.updateEvent();
        if (!handlers.containsClass(updateEvent)) {
            return ImmutableSet.of();
        }
        ImmutableCollection<M> stateHandlers = handlers.handlersOf(updateEvent);
        ImmutableSet<StateClass> result =
                stateHandlers.stream()
                        .filter(h -> h instanceof StateSubscriberMethod)
                        .map(h -> (StateSubscriberMethod) h)
                        .filter(external ? HandlerMethod::isExternal : HandlerMethod::isDomestic)
                        .map(StateSubscriberMethod::entityType)
                        .map(StateClass::from)
                        .collect(toImmutableSet());
        return result;
    }
}
