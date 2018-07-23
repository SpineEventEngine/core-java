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

package io.spine.server.event;

import com.google.common.base.Converter;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.integration.IntegrationEvent;
import io.spine.server.integration.IntegrationEventContext;

import java.io.Serializable;

import static io.spine.protobuf.TypeConverter.toAny;
import static io.spine.util.Exceptions.unsupported;

/**
 * Converts {@code IntegrationEvent} into {@code Event}.
 *
 * <p>Does not support backward conversion.
 *
 * @author Alexander Yevsyukov
 */
class IntegrationEventConverter extends Converter<IntegrationEvent, Event> implements Serializable {

    private static final long serialVersionUID = 0L;
    private static final IntegrationEventConverter INSTANCE = new IntegrationEventConverter();

    static Converter<IntegrationEvent, Event> getInstance() {
        return INSTANCE;
    }

    @Override
    protected Event doForward(IntegrationEvent input) {
        IntegrationEventContext sourceContext = input.getContext();
        EventContext context = toEventContext(sourceContext);
        Any eventMessage = input.getMessage();
        Event result = EventFactory.createEvent(sourceContext.getEventId(), eventMessage, context);
        return result;
    }

    @Override
    protected IntegrationEvent doBackward(Event event) {
        throw unsupported("Conversion of `Event` to `IntegrationEvent` is not supported");
    }

    private static EventContext toEventContext(IntegrationEventContext value) {
        Timestamp timestamp = value.getTimestamp();
        Any producerId = toAny(value.getBoundedContextName());
        EventContext.Builder result = EventContext
                .newBuilder()
                .setTimestamp(timestamp)
                .setProducerId(producerId);
        return result.build();
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
