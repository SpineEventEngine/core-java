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

import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.ActorContext;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.Events;
import io.spine.core.MessageQualifier;
import io.spine.core.Origin;
import io.spine.core.RejectionEventContext;
import io.spine.core.TenantId;
import io.spine.server.enrich.EnrichmentService;
import io.spine.type.MessageClass;
import io.spine.type.TypeName;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * The holder of an {@code Event} which provides convenient access to its properties.
 */
public final class EventEnvelope
        extends AbstractMessageEnvelope<EventId, Event, EventContext>
        implements
        ActorMessageEnvelope<EventId, Event, EventContext>,
        EnrichableMessageEnvelope<EventId, Event, EventMessage, EventContext, EventEnvelope> {

    private final EventClass eventClass;
    private final boolean rejection;

    private EventEnvelope(Event event) {
        super(event);
        this.eventClass = EventClass.of(event.enclosedMessage());
        this.rejection = event.isRejection();
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
    public EventId id() {
        EventId result = outerObject().getId();
        return result;
    }

    /**
     * Obtains tenant ID of the event.
     */
    @Override
    public TenantId tenantId() {
        return actorContext().getTenantId();
    }

    /**
     * Obtains the event message.
     */
    @Override
    public EventMessage message() {
        return outerObject().enclosedMessage();
    }

    /**
     * Obtains the class of the event.
     */
    @Override
    public EventClass messageClass() {
        return this.eventClass;
    }

    @Override
    public ActorContext actorContext() {
        return outerObject().actorContext();
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
     * @param builder
     *         event context builder into which the origin related fields are set
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    @Override
    public void setOriginFields(EventContext.Builder builder) {
        MessageQualifier eventQualifier = MessageQualifier
                .newBuilder()
                .setMessageId(pack(id()))
                .setMessageTypeUrl(outerObject().typeUrl().value())
                .buildPartial();
        Origin origin = Origin
                .newBuilder()
                .setQualifier(eventQualifier)
                .setGrandOrigin(context().getPastMessage())
                .setActorContext(actorContext())
                .vBuild();
        builder.setPastMessage(origin);
    }

    /**
     * Obtains the context of the event.
     */
    @Override
    public EventContext context() {
        return outerObject().context();
    }

    /**
     * Obtains the type of the event message.
     */
    public TypeName messageTypeName() {
        TypeName result = TypeName.of(message());
        return result;
    }

    /**
     * Obtains the class of the origin message if available.
     *
     * <p>If this envelope represents a {@linkplain Event#isRejection() rejection}, returns
     * the type of rejected command.
     *
     * @return the class of origin message or {@link EmptyClass} if the origin message type is
     *         unknown
     */
    public MessageClass originClass() {
        if (isRejection()) {
            RejectionEventContext rejection = context().getRejection();
            CommandMessage commandMessage = rejection.getCommand()
                                                     .enclosedMessage();
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
        boolean external = Events.isExternal(context());
        return external;
    }

    @Override
    public EventEnvelope toEnriched(EnrichmentService<EventMessage, EventContext> service) {
        if (!isEnrichmentEnabled()) {
            return this;
        }
        Optional<Enrichment> enrichment = service.createEnrichment(message(), this.context());
        EventEnvelope result = enrichment.map(this::withEnrichment)
                                         .orElse(this);
        return result;
    }

    /**
     * Verifies if the enrichment of the message is enabled.
     */
    public final boolean isEnrichmentEnabled() {
        boolean result = enrichment().getModeCase() != Enrichment.ModeCase.DO_NOT_ENRICH;
        return result;
    }

    private Enrichment enrichment() {
        return context().getEnrichment();
    }

    private EventEnvelope withEnrichment(Enrichment enrichment) {
        EventContext context =
                this.context()
                    .toBuilder()
                    .setEnrichment(enrichment)
                    .build();
        Event enrichedCopy =
                outerObject().toBuilder()
                             .setContext(context)
                             .build();
        return of(enrichedCopy);
    }
}
