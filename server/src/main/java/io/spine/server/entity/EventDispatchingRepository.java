/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.entity;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.base.EntityState;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventDispatcher;
import io.spine.server.route.EventRouting;
import io.spine.server.type.EventEnvelope;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract base for repositories that deliver events to entities they manage.
 */
public abstract class EventDispatchingRepository<I,
                                                 E extends AbstractEntity<I, S>,
                                                 S extends EntityState>
        extends DefaultRecordBasedRepository<I, E, S>
        implements EventDispatcher {

    private final EventRouting<I> eventRouting;

    protected EventDispatchingRepository() {
        super();
        this.eventRouting = EventRouting.withDefaultByProducerId();
    }

    /**
     * Obtains the {@link EventRouting} schema used by the repository for calculating identifiers
     * of event targets.
     */
    private EventRouting<I> eventRouting() {
        return eventRouting;
    }

    /**
     * Registers itself as an event dispatcher with the parent {@code BoundedContext}.
     *
     * @param context
     *         the {@code BoundedContext} of this repository
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    public void registerWith(BoundedContext context) {
        super.registerWith(context);
        context.internalAccess()
               .registerEventDispatcher(this);
        setupEventRouting(eventRouting());
    }

    /**
     * A callback for derived repository classes to customize routing schema for events.
     *
     * <p>Default routing returns the ID of the entity which
     * {@linkplain io.spine.core.EventContext#getProducerId() produced} the event.
     * This allows to “link” different kinds of entities by having the same class of IDs.
     * More complex scenarios (e.g. one-to-many relationships) may require custom routing schemas.
     *
     * @param routing
     *         the routing schema to customize
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // see Javadoc
    protected void setupEventRouting(EventRouting<I> routing) {
        // Do nothing.
    }

    /**
     * Dispatches the event to the corresponding entities.
     *
     * <p>If there is no stored entity with such an ID, a new one is created and stored after it
     * handles the passed event.
     *
     * @param event the event to dispatch
     */
    @Override
    @CanIgnoreReturnValue
    public final void dispatch(EventEnvelope event) {
        checkNotNull(event);
        doDispatch(event);
    }

    private void doDispatch(EventEnvelope event) {
        Set<I> targets = route(event);
        Event outerObject = event.outerObject();
        targets.forEach(id -> dispatchTo(id, outerObject));
    }

    /**
     * Dispatches the given event to an entity with the given ID.
     *
     * @param id
     *         the target entity ID
     * @param event
     *         the event to dispatch
     */
    protected abstract void dispatchTo(I id, Event event);

    /**
     * Determines the targets of the given event.
     *
     * @param event the event to find targets for
     * @return a set of IDs of projections to dispatch the given event to
     */
    protected Set<I> route(EventEnvelope event) {
        return route(eventRouting(), event)
                .orElse(ImmutableSet.of());
    }
}
