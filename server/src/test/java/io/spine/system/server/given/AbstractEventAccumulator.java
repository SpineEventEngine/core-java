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

package io.spine.system.server.given;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.event.EventDispatcher;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An {@link EventDispatcher} which can {@linkplain #remember(Message) remember} received events.
 */
public abstract class AbstractEventAccumulator implements EventDispatcher<String> {

    private final String id = getClass().getName();

    private final List<EventMessage> events = newArrayList();
    private final List<EventMessage> nonCheckedEvents = newArrayList();

    /**
     * {@inheritDoc}
     *
     * <p>Remembers the dispatched event.
     *
     * <p>The returned ID set is always a single-item set. The item is the fully-qualified name of
     * the class of this dispatcher.
     */
    @CanIgnoreReturnValue
    @Override
    public final Set<String> dispatch(EventEnvelope event) {
        EventMessage msg = event.message();
        remember(msg);
        return singleton(id);
    }

    @Override
    public final Set<EventClass> messageClasses() {
        return eventClasses();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return ImmutableSet.of();
    }

    @Override
    public Optional<ExternalMessageDispatcher<String>> createExternalDispatcher() {
        return Optional.empty();
    }

    /**
     * {@linkplain org.junit.jupiter.api.Assertions#fail(Throwable) Fails} with the given exception.
     */
    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        fail(exception);
    }

    public void assertEventCount(int expectedCount) {
        assertEquals(expectedCount, events.size(), errorMessage());
    }

    public void forgetEvents() {
        events.clear();
        nonCheckedEvents.clear();
    }

    @CanIgnoreReturnValue
    public <E extends Message> E assertExistEvent(Class<E> eventType) {
        assertFalse(nonCheckedEvents.isEmpty(), errorMessage());
        EventMessage event = nonCheckedEvents
                .stream()
                .filter(eventType::isInstance)
                .findFirst()
                .orElseGet(() -> fail(errorMessage()));
        nonCheckedEvents.remove(event);
        @SuppressWarnings("unchecked")
        E result = (E) event;
        return result;
    }

    private void remember(EventMessage event) {
        events.add(event);
        nonCheckedEvents.add(event);
    }

    private String errorMessage() {
        return format("Actual events are: %s", events.stream()
                                                     .map(Object::getClass)
                                                     .map(Class::getSimpleName)
                                                     .collect(joining(" -> ")));
    }
}
