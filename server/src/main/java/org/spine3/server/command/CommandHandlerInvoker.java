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

package org.spine3.server.command;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.server.event.EventBus;
import org.spine3.server.internal.CommandHandlerMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Method object for calling a {@code CommandHandlerMethod} and posting events to {@code EventBus}.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class CommandHandlerInvoker {

    private final EventBus eventBus;

    /* package */ CommandHandlerInvoker(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /* package */ void call(CommandHandlerMethod method, Message msg, CommandContext context)
            throws InvocationTargetException {
        final List<? extends Message> eventMessages = method.invoke(msg, context);
        final List<Event> events = toEvents(eventMessages, context);
        postEvents(events);
    }

    private static List<Event> toEvents(Iterable<? extends Message> eventMessages, CommandContext context) {
        final ImmutableList.Builder<Event> builder = ImmutableList.builder();
        for (Message eventMessage : eventMessages) {
            final EventContext eventContext = createEventContext(context);
            final Event event = Events.createEvent(eventMessage, eventContext);
            builder.add(event);
        }
        return builder.build();
    }

    private static EventContext createEventContext(CommandContext commandContext) {
        final EventId eventId = Events.generateId();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setCommandContext(commandContext)
                                                         .setTimestamp(TimeUtil.getCurrentTime());
        //TODO:2016-04-14:alexander.yevsyukov: Populate other fields.
        return builder.build();
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    private void postEvents(Iterable<Event> events) {
        for (Event event : events) {
            eventBus.post(event);
        }
    }
}
