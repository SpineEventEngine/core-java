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

package io.spine.server.aggregate.imports;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.logging.Logging;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.event.EventDispatcher;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.string.Stringifiers;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dispatches events to be imported to the associated aggregate repository.
 *
 * @param <I> the type of aggregate IDs
 * @author Alexander Yevsyukov
 */
public final class EventImportDispatcher<I> implements EventDispatcher<I>, Logging {

    private final AggregateRepository<I, ?> repository;

    private EventImportDispatcher(AggregateRepository<I, ?> repository) {
        this.repository = repository;
    }

    public static <I> EventImportDispatcher<I> of(AggregateRepository<I, ?> repository) {
        checkNotNull(repository);
        return new EventImportDispatcher<>(repository);
    }

    @Override
    public Set<EventClass> getMessageClasses() {
        return repository.getImportableEventClasses();
    }

    /**
     * Always returns empty set because external events cannot be imported.
     */
    @Override
    public Set<EventClass> getExternalEventClasses() {
        return ImmutableSet.of();
    }

    @CanIgnoreReturnValue
    @Override
    public Set<I> dispatch(EventEnvelope envelope) {
        return null;
    }

    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        EventClass eventClass = envelope.getMessageClass();
        String id = Stringifiers.toString(envelope.getId());
        _error("Unable to import event class: `{}` id: {``} repository: `{}`",
               eventClass, id, repository);
    }

    @Override
    public Optional<ExternalMessageDispatcher<I>> createExternalDispatcher() {
        return Optional.empty();
    }
}
