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

package io.spine.server.delivery;

import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.EntityHistoryId;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.entity.EntityHistoryIds.unwrap;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.Collections.emptySet;

/**
 * Subscribes to system events related to entity message dispatching.
 *
 * <p>It is expected that a {@code SystemEventWatcher} performs actions only for events related
 * to a certain type of entity.
 *
 * <p>It is also expected that this subscriber is used <b>only</b> to subscribe to
 * {@linkplain Subscribe#external() external} events.
 *
 * @param <I> the type of the entity identifier
 */
@Internal
public abstract class SystemEventWatcher<I> extends AbstractEventSubscriber {

    private static final String SYSTEM_TYPE_PACKAGE = "spine.system.server";

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
    public boolean canDispatch(EventEnvelope envelope) {
        return producerOfCorrectType(envelope) && super.canDispatch(envelope);
    }

    @Override
    public final Set<EventClass> getMessageClasses() {
        Set<EventClass> classes = super.getMessageClasses();
        Optional<String> invalidEventTypeName =
                classes.stream()
                       .map(EventClass::typeName)
                       .map(TypeName::value)
                       .filter(typeName -> !typeName.startsWith(SYSTEM_TYPE_PACKAGE))
                       .findAny();
        if (invalidEventTypeName.isPresent()) {
            throw newIllegalStateException(
                    "A %s should only subscribe to system events. %s is not a system event type.",
                    SystemEventWatcher.class, invalidEventTypeName.get()
            );
        }
        return classes;
    }

    @Override
    public final Set<EventClass> getExternalEventClasses() {
        Set<EventClass> classes = super.getExternalEventClasses();
        checkState(classes.isEmpty(),
                   "A %s subclass cannot subscribe to external events.",
                   SystemEventWatcher.class.getSimpleName());
        return emptySet();
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
        EntityHistoryId historyId = (EntityHistoryId) producerId;
        String typeUrlRaw = historyId.getTypeUrl();
        return typeUrlRaw.equals(targetType.value());
    }

    /**
     * Extracts receiver ID and casts it to the type of the generic parameter {@code <I>}.
     */
    @SuppressWarnings("unchecked")
    protected final I extract(EntityHistoryId receiver) {
        return (I) unwrap(receiver);
    }

    /**
     * Registers itself in the given domain {@link BoundedContext}.
     *
     * <p>Registers this {@link AbstractEventSubscriber} in
     * the {@link IntegrationBus} of the given context.
     *
     * @param context
     *         the domain bounded context to register the subscriber in
     */
    protected void registerIn(BoundedContext context) {
        context.getSystemClient()
               .readSide()
               .register(this);
    }
}
