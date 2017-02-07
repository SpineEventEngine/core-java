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
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.server.entity.Entity;

import javax.annotation.CheckReturnValue;

import static org.spine3.base.Events.generateId;
import static org.spine3.base.Identifiers.idToAny;
import static org.spine3.protobuf.Timestamps.getCurrentTime;

/**
 * An entity that can handle commands.
 *
 * @author Alexander Yevsyukov
 */
public abstract class CommandHandlingEntity<I, S extends Message> extends Entity<I, S> {

    /** Cached value of the ID in the form of {@code Any} instance. */
    private final Any idAsAny;

    /**
     * {@inheritDoc}
     */
    protected CommandHandlingEntity(I id) {
        super(id);
        this.idAsAny = idToAny(id);
    }

    protected Any getIdAsAny() {
        return idAsAny;
    }

    /**
     * Creates a context for an event message.
     *
     * <p>The context may optionally have custom attributes added by
     * {@link #extendEventContext(EventContext.Builder, Message, CommandContext)}.
     *
     *
     * @param event          the event for which to create the context
     * @param commandContext the context of the command, execution of which produced the event
     * @return new instance of the {@code EventContext}
     * @see #extendEventContext(EventContext.Builder, Message, CommandContext)
     */
    @CheckReturnValue
    protected EventContext createEventContext(Message event, CommandContext commandContext) {
        final EventId eventId = generateId();
        final Timestamp whenModified = getCurrentTime();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setTimestamp(whenModified)
                                                         .setCommandContext(commandContext)
                                                         .setProducerId(getIdAsAny())
                                                         .setVersion(getVersion());
        extendEventContext(builder, event, commandContext);
        return builder.build();
    }

    /**
     * Adds custom attributes to {@code EventContext.Builder} during
     * the creation of the event context.
     *
     * <p>Does nothing by default. Override this method if you want to
     * add custom attributes to the created context.
     *
     * @param builder        a builder for the event context
     * @param event          the event message
     * @param commandContext the context of the command that produced the event
     * @see #createEventContext(Message, CommandContext)
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"}) // Have no-op method to avoid forced overriding.
    protected void extendEventContext(EventContext.Builder builder,
                                      Message event,
                                      CommandContext commandContext) {
        // Do nothing.
    }
}
