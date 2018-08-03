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

package io.spine.core;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.type.TypeName;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.EventContext.OriginCase.COMMAND_CONTEXT;
import static io.spine.core.Events.isRejection;

/**
 * The holder of an {@code Event} which provides convenient access to its properties.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
public final class EventEnvelope extends EnrichableMessageEnvelope<EventId, Event, EventContext> {

    private final Message eventMessage;
    private final EventClass eventClass;
    private final EventContext eventContext;

    private EventEnvelope(Event event) {
        super(event);
        this.eventMessage = Events.getMessage(event);
        this.eventClass = EventClass.of(this.eventMessage);
        this.eventContext = event.getContext();
    }

    /**
     * Creates instance for the passed event.
     */
    public static EventEnvelope of(Event event) {
        checkNotNull(event);
        return new EventEnvelope(event);
    }

    /**
     * Obtains the event ID.
     */
    @Override
    public EventId getId() {
        EventId result = getOuterObject().getId();
        return result;
    }

    /**
     * Obtains tenant ID of the event.
     */
    @Override
    public TenantId getTenantId() {
        return getActorContext().getTenantId();
    }

    /**
     * Obtains the event message.
     */
    @Override
    public Message getMessage() {
        return this.eventMessage;
    }

    /**
     * Obtains the class of the event.
     */
    @Override
    public EventClass getMessageClass() {
        return this.eventClass;
    }

    @Override
    public EventContext getMessageContext() {
        return getEventContext();
    }

    @Override
    public ActorContext getActorContext() {
        return getEventContext().getCommandContext()
                                .getActorContext();
    }
    
    /**
     * Sets the origin fields of the event context being built using the data of the enclosed 
     * event.
     * 
     * <p>In particular: 
     * <ul>
     *     <li>the root command identifier replicates the one defined in the enclosed event;</li>
     *     <li>the context of the enclosed event is set as the origin.</li>
     * </ul>
     *
     * @param builder event context builder into which the origin related fields are set
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    @Override
    public void setOriginFields(EventContext.Builder builder) {
        EventContext context = getEventContext();
        builder.setEventContext(context);
        builder.setRootCommandId(context.getRootCommandId());
    }

    /**
     * Obtains the context of the event.
     */
    public EventContext getEventContext() {
        return this.eventContext;
    }

    /**
     * Obtains the type of the event message.
     */
    public TypeName getTypeName() {
        TypeName result = TypeName.of(eventMessage);
        return result;
    }

    public DispatchedCommand getRejectionOrigin() {
        checkState(isRejection(getOuterObject()));
        checkState(eventContext.getOriginCase() == COMMAND_CONTEXT);

        RejectionEventContext rejection = eventContext.getRejection();
        Any commandMessage = rejection.getCommandMessage();
        CommandContext context = eventContext.getCommandContext();
        DispatchedCommand result = DispatchedCommand
                .newBuilder()
                .setMessage(commandMessage)
                .setContext(context)
                .build();
        return result;
    }

    @Override
    public Enrichment getEnrichment() {
        return getEventContext().getEnrichment();
    }

    @Override
    protected EventEnvelope enrich(Enrichment enrichment) {
        Event.Builder enrichedCopy =
                getOuterObject().toBuilder()
                                .setContext(getMessageContext().toBuilder()
                                                               .setEnrichment(enrichment));
        return of(enrichedCopy.build());
    }
}
