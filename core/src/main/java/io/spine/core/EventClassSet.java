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
import com.google.errorprone.annotations.Immutable;
import io.spine.base.ThrowableMessage;
import io.spine.code.proto.MessageType;
import io.spine.code.proto.ref.TypeRef;
import io.spine.type.KnownTypes;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static io.spine.core.Events.isRejection;

/**
 * A set of combined event and rejection classes.
 *
 * <p>In the {@link io.spine.type.MessageClass} hierarchy events and rejections are independent,
 * but in Spine Model {@linkplain io.spine.base.EventMessage event messages} include both of these.
 *
 * <p>This class thus offers a convenient way for working with a combined event and rejection
 * set.
 */
@Immutable
public final class EventClassSet implements Serializable {

    private static final long serialVersionUID = 0L;

    private final ImmutableSet<EventClass> eventClasses;
    private final ImmutableSet<RejectionClass> rejectionClasses;

    private EventClassSet(ImmutableSet<EventClass> eventClasses,
                          ImmutableSet<RejectionClass> rejectionClasses) {
        this.eventClasses = eventClasses;
        this.rejectionClasses = rejectionClasses;
    }

    /**
     * Creates an empty instance of {@code EventClassSet}.
     */
    public static EventClassSet empty() {
        return new EventClassSet(ImmutableSet.of(), ImmutableSet.of());
    }

    /**
     * Creates an instance from the types referenced by a given {@code TypeRef}.
     *
     * <p>Non-event or rejection types are ignored.
     */
    public static EventClassSet parse(TypeRef typeRef) {
        ImmutableSet<MessageType> types = KnownTypes.instance()
                                                    .allMatching(typeRef);
        return new EventClassSet(events(types), rejections(types));
    }

    private static ImmutableSet<EventClass> events(Collection<MessageType> types) {
        return types.stream()
                    .filter(MessageType::isEvent)
                    .map(EventClass::of)
                    .collect(toImmutableSet());
    }

    private static ImmutableSet<RejectionClass> rejections(Collection<MessageType> types) {
        return types.stream()
                    .filter(MessageType::isRejection)
                    .map(RejectionClass::of)
                    .collect(toImmutableSet());
    }

    /**
     * Checks if any of the specified events or rejections are contained in this set.
     */
    public boolean containsAnyOf(Iterable<Event> events) {
        return containsAnyEvent(events) || containsAnyRejection(events);
    }

    private boolean containsAnyEvent(Iterable<Event> events) {
        Optional<EventClass> matchedEvent =
                stream(events)
                        .filter(event -> !isRejection(event))
                        .map(EventClass::from)
                        .filter(eventClasses::contains)
                        .findAny();
        return matchedEvent.isPresent();
    }

    private boolean containsAnyRejection(Iterable<Event> events) {
        Optional<RejectionClass> matchedRejection =
                stream(events)
                        .filter(Events::isRejection)
                        .map(RejectionClass::from)
                        .filter(rejectionClasses::contains)
                        .findAny();
        return matchedRejection.isPresent();
    }

    /**
     * Checks if rejection's enclosed message type is contained among the {@code rejectionClasses}.
     */
    public boolean contains(ThrowableMessage rejection) {
        RejectionClass rejectionClass = RejectionClass.of(rejection);
        boolean result = rejectionClasses.contains(rejectionClass);
        return result;
    }
}
