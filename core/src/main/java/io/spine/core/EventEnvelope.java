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
import io.spine.base.EventMessage;
import io.spine.type.MessageClass;
import io.spine.type.TypeName;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Enrichments.createEnrichment;

/**
 * The holder of an {@code Event} which provides convenient access to its properties.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
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
    public EventMessage getMessage() {
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

    /**
     * Obtains the actor context of the event.
     *
     * <p>The {@code ActorContext} is retrieved by traversing {@code Event}s context
     * and can be retrieved from the following places:
     * <ul>
     *     <li>the import context of the event;
     *     <li>the actor context of the command context of this event;
     *     <li>the actor context of the command context of the origin event of any depth.
     * </ul>
     *
     * @return a tenant ID available by traversing event context back to original command
     *         context or a default empty tenant ID if no tenant ID is found this way
     */
    @Override
    public ActorContext getActorContext() {
        Optional<CommandContext> commandContext = findCommandContext(getEventContext());
        if (commandContext.isPresent()) {
            return commandContext.get()
                                 .getActorContext();
        }
        return getEventContext().getImportContext();
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

    /**
     * Verifies if the enrichment of the message is enabled.
     *
     * @see Enrichment.Builder#setDoNotEnrich(boolean)
     */
    public final boolean isEnrichmentEnabled() {
        boolean result = getEnrichment().getModeCase() != Enrichment.ModeCase.DO_NOT_ENRICH;
        return result;
    }

    public Enrichment getEnrichment() {
        return getEventContext().getEnrichment();
    }

    /**
     * Creates a new version of the message with the enrichments applied.
     *
     * @param enrichments the enrichments to apply
     * @return new enriched envelope
     */
    public EventEnvelope toEnriched(Map<String, Any> enrichments) {
        checkNotNull(enrichments);
        Enrichment enrichment = createEnrichment(enrichments);
        EventEnvelope result = enrich(enrichment);
        return result;
    }

    private EventEnvelope enrich(Enrichment enrichment) {
        EventContext context = getMessageContext().toBuilder()
                                                  .setEnrichment(enrichment)
                                                  .build();
        Event.Builder enrichedCopy = getOuterObject().toBuilder()
                                                     .setContext(context);
        return of(enrichedCopy.build());
    }

    /**
     * Obtains a context of the command, which lead to this event.
     *
     * <p> The context is obtained by traversing the events origin for a valid context source.
     * There can be two sources for the command context:
     * <ol>
     *     <li>The command context set as the event origin.</li>
     *     <li>The command set as a field of a rejection context if an event was generated in a
     *     response to a rejection.</li>
     * </ol>
     *
     * <p>If at some point the event origin is not set the {@link Optional#empty()} is returned.
     */
    private static Optional<CommandContext> findCommandContext(EventContext eventContext) {
        CommandContext commandContext = null;
        EventContext ctx = eventContext;

        while (commandContext == null) {
            switch (ctx.getOriginCase()) {
                case EVENT_CONTEXT:
                    ctx = ctx.getEventContext();
                    break;
                case COMMAND_CONTEXT:
                    commandContext = ctx.getCommandContext();
                    break;
                case IMPORT_CONTEXT:
                case ORIGIN_NOT_SET:
                default:
                    return Optional.empty();
            }
        }

        return Optional.of(commandContext);
    }
}
