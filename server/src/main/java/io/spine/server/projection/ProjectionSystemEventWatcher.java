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

package io.spine.server.projection;

import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.delivery.SystemEventWatcher;
import io.spine.server.event.DuplicateEventException;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EventDispatchedToSubscriber;
import io.spine.system.server.HistoryRejections;

/**
 * An {@link io.spine.server.event.AbstractEventSubscriber EventSubscriber} for system events
 * related to dispatching events to {@link Projection}s of a given type.
 *
 * @see SystemEventWatcher
 */
final class ProjectionSystemEventWatcher<I> extends SystemEventWatcher<I> {

    private final ProjectionRepository<I, ?, ?> repository;

    ProjectionSystemEventWatcher(ProjectionRepository<I, ?, ?> repository) {
        super(repository.getEntityStateType());
        this.repository = repository;
    }

    @Subscribe
    public void on(EventDispatchedToSubscriber event) {
        EntityHistoryId receiver = event.getReceiver();
        I id = extract(receiver);
        EventEnvelope envelope = EventEnvelope.of(event.getPayload());
        repository.dispatchNowTo(id, envelope);
    }

    @Subscribe
    public void on(HistoryRejections.CannotDispatchEventTwice event) {
        onError(event.getPayload());
    }

    private void onError(Event event) {
        RuntimeException exception = new DuplicateEventException(event);
        EventEnvelope envelope = EventEnvelope.of(event);
        repository.onError(envelope, exception);
    }

    @Override // Exposes the method to this package.
    protected void registerIn(BoundedContext context) {
        super.registerIn(context);
    }
}
