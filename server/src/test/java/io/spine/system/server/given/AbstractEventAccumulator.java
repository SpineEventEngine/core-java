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

package io.spine.system.server.given;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.event.EventDispatcher;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An {@link EventDispatcher} which can {@linkplain #remember(Message) remember} received events.
 *
 * @author Dmytro Dashenkov
 */
abstract class AbstractEventAccumulator implements EventDispatcher<String> {

    private final String id = getClass().getName();

    private final List<Message> events = newLinkedList();

    private @Nullable Iterator<? extends Message> eventIterator;

    AbstractEventAccumulator() {
        super();
    }

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
    public final Set<String> dispatch(EventEnvelope envelope) {
        Message event = envelope.getMessage();
        remember(event);
        return singleton(id);
    }

    @Override
    public final Set<EventClass> getMessageClasses() {
        Collection<Class<? extends Message>> messageClasses = getEventClasses();
        return messageClasses.stream()
                             .map(EventClass::of)
                             .collect(toSet());
    }

    /**
     * Obtains the Java classes of the dispatched events.
     */
    protected abstract Collection<Class<? extends Message>> getEventClasses();

    /**
     * {@linkplain org.junit.jupiter.api.Assertions#fail(Throwable) Fails} with the given exception.
     */
    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        fail(exception);
    }

    public void assertEventCount(int expectedCount) {
        assertEquals(expectedCount, events.size(), errorMessage());
    }

    public void forgetEvents() {
        events.clear();
        eventIterator = null;
    }

    @CanIgnoreReturnValue
    public <E extends Message> E nextEvent(Class<E> eventType) {
        if (eventIterator == null) {
            eventIterator = copyOf(events).iterator();
        }
        assertTrue(eventIterator.hasNext(), errorMessage());
        Message next = eventIterator.next();
        assertThat(next, instanceOf(eventType));
        @SuppressWarnings("unchecked")
        E result = (E) next;
        return result;
    }

    private void remember(Message event) {
        events.add(event);
    }

    private String errorMessage() {
        return format("Actual events are: %s", events.stream()
                                                     .map(Object::getClass)
                                                     .map(Class::getSimpleName)
                                                     .collect(joining(" -> ")));
    }
}
