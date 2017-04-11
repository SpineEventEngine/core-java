/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Event;
import org.spine3.base.EventContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;

/**
 * The factory for creating import events.
 *
 * @author Alexander Yevsyukov
 */
public class ImportEventFactory {

    private ImportEventFactory() {}

    /**
     * Creates {@code Event} instance for import or integration operations.
     *
     * @param event      the event message
     * @param producerId the ID of an entity which is generating the event packed into {@code Any}
     * @return event with data from an external source
     */
    public static Event createEvent(Message event, Any producerId) {
        checkNotNull(event);
        checkNotNull(producerId);
        final EventContext context = createEventContext(producerId);
        final Event result = EventFactory.createEvent(event, context);
        return result;
    }

    /**
     * Creates {@code EventContext} instance for an event generated during data import.
     *
     * <p>The method does not set {@code CommandContext} because there was no command.
     *
     * <p>The {@code version} attribute is not populated either.
     * It is the responsibility of the target aggregate to populate the missing fields.
     *
     * @param producerId the ID of the producer which generates the event packed into {@code Any}
     * @return new instance of {@code EventContext} for the imported event
     */
    public static EventContext createEventContext(Any producerId) {
        checkNotNull(producerId);
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(EventFactory.generateId())
                                                         .setTimestamp(getCurrentTime())
                                                         .setProducerId(producerId);
        return builder.build();
    }
}
