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

package io.spine.server.event.model;

import com.google.common.collect.ImmutableSet;
import io.spine.core.EventClass;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.ModelClass;

import java.util.Set;

/**
 * Helper object for storing information about methods and events of an
 * {@linkplain EventReceiverClass event receiving class}.
 *
 * @author Alexander Yevsyukov
 */
public class EventReceivingClassDelegate<T, M extends HandlerMethod<?, EventClass, ?, ?>>
        extends ModelClass<T> {

    private static final long serialVersionUID = 0L;
    private final MessageHandlerMap<EventClass, M> events;
    private final ImmutableSet<EventClass> domesticEvents;
    private final ImmutableSet<EventClass> externalEvents;

    /**
     * Creates new instance for the passed raw class with methods obtained
     * though the passed factory.
     */
    public EventReceivingClassDelegate(Class<? extends T> rawClass, MethodFactory<M> factory) {
        super(rawClass);
        this.events = new MessageHandlerMap<>(rawClass, factory);
        this.domesticEvents = events.getMessageClasses(HandlerMethod::isDomestic);
        this.externalEvents = events.getMessageClasses(HandlerMethod::isExternal);
    }

    public boolean contains(EventClass eventClass) {
        return events.containsClass(eventClass);
    }

    /**
     * Obtains domestic event classes handled by the class.
     */
    public Set<EventClass> getEventClasses() {
        return domesticEvents;
    }

    /**
     * Obtains external event classes handled by the class.
     */
    public Set<EventClass> getExternalEventClasses() {
        return externalEvents;
    }

    /**
     * Obtains the method which handles the passed event class.
     *
     * @throws IllegalStateException if there is such method in the class
     */
    public M getMethod(EventClass eventClass) {
        return events.getMethod(eventClass);
    }
}
