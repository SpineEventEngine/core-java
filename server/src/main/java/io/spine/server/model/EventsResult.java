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

package io.spine.server.model;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.core.Version;
import io.spine.server.event.EventFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Abstract base for method results that generate events.
 *
 * @author Alexander Yevsyukov
 */
public abstract class EventsResult extends MethodResult<Message> {

    protected EventsResult(@Nullable Object output) {
        super(output);
    }

    /**
     * Transforms the messages of the result into a list of events.
     */
    public
    List<Event> createEvents(MessageEnvelope origin, Any producerId, @Nullable Version version) {
        checkNotNull(producerId);
        EventFactory eventFactory = EventFactory.on(origin, producerId);
        List<? extends Message> messages = asMessages();
        List<Event> result =
                messages.stream()
                        .map(eventMessage -> eventFactory.createEvent(eventMessage, version))
                        .collect(toList());
        return result;
    }
}
