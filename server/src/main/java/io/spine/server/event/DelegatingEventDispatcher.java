/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.event;

import io.spine.annotation.Internal;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link EventDispatcher} which delegates the responsibilities to an aggregated {@link
 * EventDispatcherDelegate delegate instance}.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Yevsyukov
 * @see EventDispatcherDelegate
 */
@Internal
public final class DelegatingEventDispatcher<I> implements EventDispatcher<I> {

    /**
     * A target delegate.
     */
    private final EventDispatcherDelegate<I> delegate;

    /**
     * Creates a new instance of {@code DelegatingCommandDispatcher}, proxying the calls
     * to the passed {@code delegate}.
     *
     * @param delegate a delegate to pass the dispatching duties to
     * @return new instance
     */
    public static <I> DelegatingEventDispatcher<I> of(EventDispatcherDelegate<I> delegate) {
        checkNotNull(delegate);
        return new DelegatingEventDispatcher<>(delegate);
    }

    private DelegatingEventDispatcher(EventDispatcherDelegate<I> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<EventClass> getMessageClasses() {
        return delegate.getEventClasses();
    }

    @Override
    public Set<I> dispatch(EventEnvelope envelope) {
        return delegate.dispatchEvent(envelope);
    }
}
