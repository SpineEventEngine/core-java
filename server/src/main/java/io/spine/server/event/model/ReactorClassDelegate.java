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
import io.spine.server.model.ModelClass;
import io.spine.type.MessageClass;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.copyOf;

/**
 * The helper class for holding messaging information on behalf of another model class.
 *
 * @param <T> the type of the raw class for obtaining messaging information
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public final class ReactorClassDelegate<T> extends ModelClass<T> implements ReactorClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<EventClass, EventReactorMethod> eventReactions;

    private final ImmutableSet<EventClass> domesticEventReactions;
    private final ImmutableSet<EventClass> externalEventReactions;

    public ReactorClassDelegate(Class<T> cls) {
        super(cls);
        this.eventReactions = new MessageHandlerMap<>(cls, EventReactorMethod.factory());

        this.domesticEventReactions = eventReactions.getMessageClasses(HandlerMethod::isDomestic);
        this.externalEventReactions = eventReactions.getMessageClasses(HandlerMethod::isExternal);
    }

    @Override
    public Set<EventClass> getEventReactions() {
        return copyOf(domesticEventReactions);
    }

    @Override
    public Set<EventClass> getExternalEventReactions() {
        return copyOf(externalEventReactions);
    }

    @Override
    public EventReactorMethod getReactor(EventClass eventClass, MessageClass originClass) {
        return eventReactions.getMethod(eventClass, originClass);
    }
}
