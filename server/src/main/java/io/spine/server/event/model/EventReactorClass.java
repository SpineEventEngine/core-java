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

package io.spine.server.event.model;

import com.google.common.collect.ImmutableSet;
import io.spine.server.event.EventReactor;
import io.spine.server.model.HandlerMap;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.ModelClass;
import io.spine.server.type.EventClass;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides information on an {@link EventReactor} class.
 *
 * @param <S>
 *         the type of event reactors
 */
public final class EventReactorClass<S extends EventReactor> extends ModelClass<S>
        implements ReactingClass {

    private static final long serialVersionUID = 0L;

    private final HandlerMap<EventClass, EventClass, EventReactorMethod> reactors;
    private final ImmutableSet<EventClass> events;
    private final ImmutableSet<EventClass> domesticEvents;
    private final ImmutableSet<EventClass> externalEvents;

    private EventReactorClass(Class<? extends S> cls) {
        super(cls);
        this.reactors = HandlerMap.create(cls, new EventReactorSignature());
        this.events = reactors.messageClasses();
        this.domesticEvents = reactors.messageClasses((h) -> !h.isExternal());
        this.externalEvents = reactors.messageClasses(HandlerMethod::isExternal);
    }

    /** Creates new instance for the given raw class. */
    public static <S extends EventReactor> EventReactorClass<S>
    asReactorClass(Class<S> cls) {
        checkNotNull(cls);
        @SuppressWarnings("unchecked")
        EventReactorClass<S> result = (EventReactorClass<S>)
                get(cls, EventReactorClass.class, () -> new EventReactorClass<>(cls));
        return (result);
    }

    @Override
    public EventReactorMethod reactorOf(EventClass eventClass, MessageClass<?> originClass) {
        return reactors.handlerOf(eventClass, originClass);
    }

    @Override
    public ImmutableSet<EventClass> reactionOutput() {
        return reactors.producedTypes();
    }

    @Override
    public ImmutableSet<EventClass> events() {
        return events;
    }

    @Override
    public ImmutableSet<EventClass> externalEvents() {
        return externalEvents;
    }

    @Override
    public ImmutableSet<EventClass> domesticEvents() {
        return domesticEvents;
    }
}
