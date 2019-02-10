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

package io.spine.core;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.type.MessageClass;
import io.spine.type.TypeUrl;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Events.ensureMessage;

/**
 * A value object holding a class of events.
 */
public final class EventClass extends MessageClass<EventMessage> {

    private static final long serialVersionUID = 0L;

    private EventClass(Class<? extends EventMessage> value) {
        super(value);
    }

    /**
     * Creates a new instance of the event class.
     *
     * @param value a value to hold
     * @return new instance
     */
    public static EventClass from(Class<? extends EventMessage> value) {
        return new EventClass(checkNotNull(value));
    }

    /**
     * Creates a new {@code EventClass} instance from the passed type URL.
     *
     * @throws IllegalArgumentException
     *         if the passed {@code TypeUrl} does not represent an event type
     */
    @SuppressWarnings("unchecked") // Logically checked.
    public static EventClass from(TypeUrl typeUrl) {
        Class<? extends Message> messageClass = typeUrl.getMessageClass();
        checkArgument(EventMessage.class.isAssignableFrom(messageClass),
                      "Event class constructed from non-EventMessage type URL: %s",
                      typeUrl.value());
        return from((Class<? extends EventMessage>) messageClass);
    }

    /**
     * Creates a new instance of the event class by passed event instance.
     *
     * <p>If an instance of {@link Event} is passed to this method, enclosing event message will be
     * un-wrapped to determine the class of the event.
     *
     * <p>If an instance of {@link Any} is passed, it will be unpacked, and the class of the wrapped
     * message will be used.
     *
     * @param eventOrMessage an event message, or {@link Any}, {@link Event}
     * @return new instance
     */
    public static EventClass of(Message eventOrMessage) {
        EventMessage eventMessage = ensureMessage(eventOrMessage);
        return from(eventMessage.getClass());
    }

    /** Creates immutable set of {@code EventClass} from the passed set. */
    public static ImmutableSet<EventClass> setOf(Iterable<Class<? extends EventMessage>> classes) {
        ImmutableSet.Builder<EventClass> builder = ImmutableSet.builder();
        for (Class<? extends EventMessage> cls : classes) {
            builder.add(from(cls));
        }
        return builder.build();
    }

    /** Creates immutable set of {@code EventClass} from the passed classes. */
    @SafeVarargs
    public static ImmutableSet<EventClass> setOf(Class<? extends EventMessage>... classes) {
        return setOf(Arrays.asList(classes));
    }
}
