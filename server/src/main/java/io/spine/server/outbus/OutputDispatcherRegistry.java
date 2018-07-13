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
package io.spine.server.outbus;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.bus.MessageDispatcher;
import io.spine.type.MessageClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base registry of the dispatchers, responsible for dispatching the command output to the
 * corresponding objects.
 *
 * <p>The registries of this kind are managed by the descendants of {@link CommandOutputBus}.
 *
 * @author Alex Tymchenko
 * @see CommandOutputBus
 */
@Internal
public class OutputDispatcherRegistry<C extends MessageClass,
                                      D extends MessageDispatcher<C, ?, ?>>
                                extends DispatcherRegistry<C, D> {
    /**
     * {@inheritDoc}
     *
     * <p>Overrides in order to expose itself to
     * {@link CommandOutputBus#post(Message, StreamObserver) CommandOutputBus}.
     */
    @Override
    protected Set<D> getDispatchers(C messageClass) {
        return super.getDispatchers(messageClass);
    }

    @Override
    protected void register(D dispatcher) {
        checkNotNull(dispatcher);
        Set<C> eventClasses = dispatcher.getMessageClasses();
        checkNotEmpty(dispatcher, eventClasses);

        super.register(dispatcher);
    }

    @Override
    protected void unregister(D dispatcher) {
        checkNotNull(dispatcher);
        Set<C> eventClasses = dispatcher.getMessageClasses();
        checkNotEmpty(dispatcher, eventClasses);

        super.unregister(dispatcher);
    }

    protected boolean hasDispatchersFor(C eventClass) {
        Set<D> dispatchers = getDispatchers(eventClass);
        boolean result = !dispatchers.isEmpty();
        return result;
    }

    /**
     * Ensures that the dispatcher forwards at least one event.
     *
     * @throws IllegalArgumentException if the dispatcher returns empty set of event classes
     * @throws NullPointerException     if the dispatcher returns null set
     */
    private void checkNotEmpty(D dispatcher, Set<C> messageClasses) {
        checkArgument(!messageClasses.isEmpty(),
                      "%s: No message types are forwarded by this dispatcher: %s",
                      getClass().getName(),  dispatcher);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides in order to expose itself to {@link CommandOutputBus#close() CommandOutputBus}.
     */
    @SuppressWarnings("RedundantMethodOverride")
    @Override
    protected void unregisterAll() {
        super.unregisterAll();
    }
}
