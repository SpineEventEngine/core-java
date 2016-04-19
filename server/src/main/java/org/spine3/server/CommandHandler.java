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

package org.spine3.server;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.server.event.EventBus;
import org.spine3.server.reflect.CommandHandlerMethod;
import org.spine3.server.reflect.MethodRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * The abstract base for classes that expose command handling methods
 * and post their results to {@link EventBus}.
 *
 * <p>A command handler is responsible for:
 * <ol>
 *     <li>Changing the state of the business model</li>
 *     <li>Producing corresponding events.</li>
 *     <li>Posting events to {@code EventBus}.</li>
 * </ol>
 *
 * <p>Event messages are returned as values of command handling methods.
 *
 * <h2>Command handling methods</h2>
 * <p>A command handling method is a {@code public} method that accepts two parameters.
 * The first parameter is a command message. The second parameter is {@link CommandContext}.
 *
 * <p>The method returns an event message of the specific type, or {@code List} of messages
 * if it produces more than one event.
 *
 * <p>The method may throw one or more throwables derived from {@link FailureThrowable}.
 * Throwing a {@code FailureThrowable} indicates that the passed command cannot be handled
 * because of a business failure, which can be obtained via {@link FailureThrowable#getFailure()}.
 *
 * @author Alexander Yevsyukov
 * @see CommandDispatcher
 */
public abstract class CommandHandler {

    private final EventBus eventBus;

    protected CommandHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void handle(Message commandMessage, CommandContext context) throws InvocationTargetException {
        final CommandHandlerMethod method = getHandlerMethod(commandMessage.getClass());

        final List<? extends Message> eventMessages = method.invoke(this, commandMessage, context);

        final List<Event> events = toEvents(eventMessages, context);
        postEvents(events);
    }

    public CommandHandlerMethod getHandlerMethod(Class<? extends Message> commandClass) {
        return MethodRegistry.getInstance().get(getClass(), commandClass, CommandHandlerMethod.factory());
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
