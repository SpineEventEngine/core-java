/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3;

import com.google.protobuf.Message;
import org.spine3.base.EventRecord;
import org.spine3.util.MessageValue;
import org.spine3.protobuf.Messages;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object for holding an event instance.
 *
 * @author Alexander Yevsyukov
 */
public final class Event extends MessageValue {

    private Event(Message value) {
        super(value);
    }

    /**
     * Creates a new instance of the event value object.
     * @param value the event message
     * @return new instance
     */
    public static Event of(Message value) {
        return new Event(checkNotNull(value));
    }

    @SuppressWarnings("TypeMayBeWeakened") // Restrict API to already built instances.
    public static Event from(EventRecord record) {
        Message value = Messages.fromAny(record.getEvent());
        return of(value);
    }

    /**
     * {@inheritDoc}
     * Opens access for classes in this package.
     * @return non-null value of the event message
     */
    @Nonnull
    @Override
    protected Message value() {
        Message value = super.value();
        assert value != null; // because we don't allow null input.
        return value;
    }

    /**
     * @return the class of the event object
     */
    public EventClass getEventClass() {
        final Message value = value();
        return EventClass.of(value.getClass());
    }
}
