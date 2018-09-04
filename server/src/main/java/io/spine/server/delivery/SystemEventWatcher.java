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

package io.spine.server.delivery;

import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.system.server.EntityHistoryId;
import io.spine.type.TypeUrl;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.type.TypeUrl.parse;
import static java.util.Collections.emptySet;

/**
 * An {@link AbstractEventSubscriber EventSubscriber} for system events related to message
 * dispatching.
 *
 * <p>It is expected that a {@code SystemEventWatcher} performs actions only for events related
 * to a certain type of entity.
 *
 * <p>It is also expected that this subscriber is used <b>only</b> to subscribe to
 * {@linkplain io.spine.core.Subscribe#external() external} events.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public abstract class SystemEventWatcher<I> extends AbstractEventSubscriber {

    private final TypeUrl targetType;

    /**
     * Crates a new instance of {@code SystemEventWatcher}.
     *
     * @param targetType
     *         the type of the entity to watch the events for
     */
    protected SystemEventWatcher(TypeUrl targetType) {
        super();
        this.targetType = checkNotNull(targetType);
    }

    @Override
    public Set<String> dispatch(EventEnvelope envelope) {
        return producerOfCorrectType(envelope)
               ? super.dispatch(envelope)
               : emptySet();
    }

    /**
     * Checks if the given system event belongs to an entity history of an entity with
     * the {@link #targetType}.
     *
     * @param event
     *         the event to check
     * @return {@code true} if the given event producer ID is an {@link EntityHistoryId} and
     *         the {@linkplain EntityHistoryId#getTypeUrl() domain entity type} is the target type
     *         of this watcher
     */
    private boolean producerOfCorrectType(EventEnvelope event) {
        EventContext context = event.getEventContext();
        Any anyId = context.getProducerId();
        Object producerId = Identifier.unpack(anyId);
        boolean idTypeMatches = producerId instanceof EntityHistoryId;
        if (idTypeMatches) {
            EntityHistoryId historyId = (EntityHistoryId) producerId;
            String typeUrlRaw = historyId.getTypeUrl();
            TypeUrl typeUrl = parse(typeUrlRaw);
            return typeUrl.equals(targetType);
        } else {
            _warn("Event %s is produced by an entity with ID '%s'.",
                  event.getMessageClass(), producerId);
            return false;
        }
    }

    /**
     * Unpacks the generic entity ID from the given {@link io.spine.system.server.EntityHistoryId
     * EntityHistoryId}.
     */
    protected I idFrom(EntityHistoryId historyId) {
        Any id = historyId.getEntityId()
                          .getId();
        return Identifier.unpack(id);
    }

    /**
     * Registers itself in the given domain {@link BoundedContext}.
     *
     * <p>Registers this {@link AbstractEventSubscriber} in
     * the {@link io.spine.server.integration.IntegrationBus} of the given context.
     *
     * @param context
     *         the domain bounded context to register the subscriber in
     */
    protected void registerIn(BoundedContext context) {
        context.getIntegrationBus()
               .register(this);
    }
}
