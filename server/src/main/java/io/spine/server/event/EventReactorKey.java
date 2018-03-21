/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.event;

import com.google.common.base.MoreObjects;
import io.spine.core.EventClass;
import io.spine.server.model.HandlerKey;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A key for {@link EventReactorMethod}.
 *
 * @author Dmytro Grankin
 */
public final class EventReactorKey implements HandlerKey<EventClass> {

    private final EventClass eventClass;

    private EventReactorKey(EventClass eventClass) {
        this.eventClass = checkNotNull(eventClass);
    }

    public static EventReactorKey of(EventClass eventClass) {
        return new EventReactorKey(eventClass);
    }

    @Override
    public EventClass getHandledMessageCls() {
        return eventClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventReactorKey that = (EventReactorKey) o;
        return Objects.equals(eventClass, that.eventClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventClass);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("eventClass", eventClass)
                          .toString();
    }
}
