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

package io.spine.client;

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.base.MessageContext;
import io.spine.core.EmptyContext;
import io.spine.core.EventContext;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts a consumer of an event message to the {@link EventConsumer} interface.
 */
abstract class DelegatingConsumer<M extends Message, C extends MessageContext>
        implements MessageConsumer<M, C> {

    private final Consumer<M> delegate;

    static <E extends EventMessage> EventConsumer<E> ofEvent(Consumer<E> consumer) {
        checkNotNull(consumer);
        return new DelegatingEventConsumer<>(consumer);
    }

    static <S extends Message> StateConsumer<S> ofState(Consumer<S> consumer) {
        checkNotNull(consumer);
        return new DelegatingStateConsumer<>(consumer);
    }

    private DelegatingConsumer(Consumer<M> delegate) {
        this.delegate = checkNotNull(delegate);
    }

    /**
     * Obtains the reference to the consumer of a message, which will be used for
     * diagnostic purposes.
     *
     * <p>If the passed instance is a {@code DelegatingConsumer}, its delegate will be returned.
     * Otherwise, the method returns the passed instance.
     */
    static Object toRealConsumer(MessageConsumer<?, ?> consumer) {
        return consumer instanceof DelegatingConsumer
               ? ((DelegatingConsumer) consumer).delegate()
               : consumer;
    }

    /** Obtains the consumer of the event message to which this consumer delegates. */
    final Consumer<M> delegate() {
        return delegate;
    }

    /** Passes the event message to the associated consumer. */
    @Override
    public void accept(M m, C context) {
        delegate.accept(m);
    }

    /**
     * Adapts a consumer of an event message to the {@link EventConsumer} interface.
     */
    private static final class DelegatingEventConsumer<E extends EventMessage>
            extends DelegatingConsumer<E, EventContext>
            implements EventConsumer<E> {

        private DelegatingEventConsumer(Consumer<E> delegate) {
            super(delegate);
        }
    }

    private static final class DelegatingStateConsumer<S extends Message>
            extends DelegatingConsumer<S, EmptyContext>
            implements StateConsumer<S> {

        private DelegatingStateConsumer(Consumer<S> delegate) {
            super(delegate);
        }

        @Override
        public void accept(S s) {
            accept(s, EmptyContext.getDefaultInstance());
        }
    }
}
