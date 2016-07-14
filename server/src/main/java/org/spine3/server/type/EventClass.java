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

package org.spine3.server.type;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.Event;
import org.spine3.base.Events;
import org.spine3.protobuf.TypeUrl;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object holding a class of events.
 *
 * @author Alexander Yevsyukov
 */
public final class EventClass extends MessageClass {

    private EventClass(Class<? extends Message> value) {
        super(value);
    }

    /**
     * Creates a new instance of the event class.
     * @param value a value to hold
     * @return new instance
     */
    public static EventClass of(Class<? extends Message> value) {
        return new EventClass(checkNotNull(value));
    }

    /**
     * Creates a new instance of the event class by passed event instance.
     *
     * <p>If an instance of {@link Event} (which implements {@code Message}) is passed to this method,
     * enclosing event message will be un-wrapped to determine the class of the event.
     *
     * @param event an event instance
     * @return new instance
     */
    public static EventClass of(Message event) {
        final Message message = checkNotNull(event);
        if (message instanceof Event) {
            final Event eventRecord = (Event) event;
            final Message enclosed = Events.getMessage(eventRecord);
            return of(enclosed.getClass());
        }
        final EventClass result = of(message.getClass());
        return result;
    }

    /**
     * Creates immutable set of {@code EventClass} from the passed set.
     */
    public static ImmutableSet<EventClass> setOf(Set<Class<? extends Message>> classes) {
        final ImmutableSet.Builder<EventClass> builder = ImmutableSet.builder();
        for (Class<? extends Message> cls : classes) {
            builder.add(of(cls));
        }
        return builder.build();
    }

    /**
     * Creates immutable set of {@code EventClass} from the passed classes.
     */
    @SafeVarargs
    public static ImmutableSet<EventClass> setOf(Class<? extends Message>... classes) {
        final ImmutableSet.Builder<EventClass> builder = ImmutableSet.builder();
        for (Class<? extends Message> cls : classes) {
            builder.add(of(cls));
        }
        return builder.build();
    }

    /**
     * Obtains a type URL corresponding to the event class.
     */
    public TypeUrl toTypeUrl() {
        TypeUrl result = TypeUrl.of(value());
        return result;
    }
}
