/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.type;

import com.google.protobuf.Any;
import io.spine.base.EventMessage;
import io.spine.core.ActorContext;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.Events;
import io.spine.core.RejectionEventContext;
import io.spine.core.TenantId;
import io.spine.server.enrich.EnrichmentService;
import io.spine.type.MessageClass;
import io.spine.type.TypeName;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The holder of an {@code Event} which provides convenient access to its properties.
 */
public final class EventEnvelope
        extends AbstractMessageEnvelope<EventId, Event, EventContext>
        implements ActorMessageEnvelope<EventId, Event, EventContext> {

    private final EventMessage eventMessage;
    private final EventClass eventClass;
    private final EventContext eventContext;
    private final boolean rejection;

    private EventEnvelope(Event event) {
        super(event);
        this.eventMessage = Events.getMessage(event);
        this.eventClass = EventClass.of(this.eventMessage);
        this.eventContext = event.getContext();
        this.rejection = Events.isRejection(event);
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
        EventId result = outerObject().getId();
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
    public EventMessage message() {
        return this.eventMessage;
    }

    /**
     * Obtains the class of the event.
     */
    @Override
    public EventClass messageClass() {
        return this.eventClass;
    }

    @Override
    public EventContext messageContext() {
        return getEventContext();
    }

    @Override
    public ActorContext getActorContext() {
        return Events.getActorContext(outerObject());
    }

    /**
     * Sets the origin fields of the event context being built using the data of the enclosed
     * event.
     *
     * <p>In particular:
     * <ul>
     *     <li>the root command identifier replicates the one defined in the enclosed event;
     *     <li>the context of the enclosed event is set as the origin.
     * </ul>
     *
     * @param builder event context builder into which the origin related fields are set
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    @Override
    public void setOriginFields(EventContext.Builder builder) {
        EventContext context = getEventContext();
        builder.setEventContext(context)
               .setRootCommandId(context.getRootCommandId())
               .setEventId(getId());
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

    /**
     * Obtains the class of the origin message if available.
     *
     * <p>If this envelope represents a {@linkplain Events#isRejection(Event) rejection}, returns
     * the type of rejected command.
     *
     * @return the class of origin message or {@link EmptyClass} if the origin message type is
     * unknown
     */
    public MessageClass getOriginClass() {
        if (isRejection()) {
            RejectionEventContext rejection = eventContext.getRejection();
            Any commandMessage = rejection.getCommandMessage();
            return CommandClass.of(commandMessage);
        } else {
            return EmptyClass.instance();
        }
    }

    /**
     * Returns {@code true} if the wrapped event is a rejection, {@code false} otherwise.
     */
    public boolean isRejection() {
        return rejection;
    }

    /**
     * Returns {@code true} is the wrapped event is external, {@code false} otherwise.
     */
    public boolean isExternal() {
        boolean external = Events.isExternal(getEventContext());
        return external;
    }

    public EventEnvelope toEnriched(EnrichmentService service) {
        if (!isEnrichmentEnabled()) {
            return this;
        }
        Optional<Enrichment> enrichment = service.createEnrichment(message(), messageContext());
        EventEnvelope result = enrichment.map(this::withEnrichment)
                                         .orElse(this);
        return result;
    }

    /**
     * Verifies if the enrichment of the message is enabled.
     */
    public final boolean isEnrichmentEnabled() {
        boolean result = getEnrichment().getModeCase() != Enrichment.ModeCase.DO_NOT_ENRICH;
        return result;
    }

    public Enrichment getEnrichment() {
        return getEventContext().getEnrichment();
    }

    private EventEnvelope withEnrichment(Enrichment enrichment) {
        EventContext context = messageContext().toBuilder()
                                               .setEnrichment(enrichment)
                                               .build();
        Event.Builder enrichedCopy = outerObject().toBuilder()
                                                  .setContext(context);
        return of(enrichedCopy.build());
    }
}
