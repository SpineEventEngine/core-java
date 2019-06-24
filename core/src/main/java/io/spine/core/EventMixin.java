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

package io.spine.core;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.base.EventMessage;
import io.spine.validate.Validate;

import java.util.Optional;

import static io.spine.core.EventContext.OriginCase.EVENT_CONTEXT;
import static io.spine.core.EventContext.OriginCase.PAST_MESSAGE;
import static io.spine.validate.Validate.isDefault;

/**
 * Mixin interface for event objects.
 */
@Immutable
public interface EventMixin extends Signal<EventId, EventMessage, EventContext> {

    /**
     * Obtains the ID of the tenant of the event.
     */
    @Override
    default TenantId tenant() {
        return actorContext().getTenantId();
    }

    @Override
    default Timestamp time() {
        return context().getTimestamp();
    }

    @Override
    default MessageId rootMessage() {
        EventContext.OriginCase origin = context().getOriginCase();
        return origin == PAST_MESSAGE
               ? context().getPastMessage().root()
               : messageId();
    }

    @Override
    default Optional<Origin> origin() {
        Origin parent = context().getPastMessage();
        return Optional.of(parent)
                       .filter(Validate::isNotDefault);
    }

    /**
     * Obtains the ID of the root command, which lead to this event.
     *
     * <p> In case the {@code Event} is a reaction to another {@code Event},
     * the identifier of the very first command in this chain is returned.
     *
     * @return the root command ID
     */
    default CommandId rootCommandId() {
        return context().getPastMessage()
                        .root()
                        .asCommandId();
    }

    /**
     * Checks if this event is a rejection.
     *
     * @return {@code true} if the given event is a rejection, {@code false} otherwise
     */
    default boolean isRejection() {
        EventContext context = context();
        boolean result = context.hasRejection() || !isDefault(context.getRejection());
        return result;
    }

    /**
     * Creates a copy of this instance without enrichments.
     *
     * <p>Use this method to decrease a size of an event, if enrichments aren't important.
     *
     * <p>A result won't contain:
     * <ul>
     *     <li>the enrichment from the event context;</li>
     *     <li>the enrichment from the first-level origin.</li>
     * </ul>
     *
     * <p>Enrichments will not be removed from second-level and deeper origins,
     * because it's a heavy performance operation.
     *
     * @return the event without enrichments
     */
    @SuppressWarnings({
            "ClassReferencesSubclass", //`Event` is the only case of this mixin.
            "deprecation" // Uses the `event_context` field to be sure to clean up old data.
    })
    @Internal
    default Event clearEnrichments() {
        EventContext context = context();
        EventContext.OriginCase originCase = context.getOriginCase();
        EventContext.Builder resultContext = context.toBuilder()
                                                    .clearEnrichment();
        if (originCase == EVENT_CONTEXT) {
            resultContext.setEventContext(context.getEventContext()
                                                 .toBuilder()
                                                 .clearEnrichment()
                                                 .build());
        }
        Event thisEvent = (Event) this;
        Event result = thisEvent.toBuilder()
                                .setContext(resultContext.build())
                                .build();
        return result;
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
     * @return the actor context of the wrapped event
     */
    @Override
    @Internal
    default ActorContext actorContext() {
        EventContext eventContext = context();
        ActorContext result = eventContext.actorContext();
        return result;
    }

    @Override
    default MessageId messageId() {
        return Signal.super.messageId()
                           .toBuilder()
                           .setVersion(context().getVersion())
                           .vBuild();
    }
}
