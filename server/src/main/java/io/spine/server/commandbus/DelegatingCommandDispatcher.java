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
package io.spine.server.commandbus;

import com.google.common.base.MoreObjects;
import io.spine.annotation.Internal;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import java.util.Set;

/**
 * A {@link CommandDispatcher}, that delegates the responsibilities to an aggregated
 * {@linkplain CommandDispatcherDelegate delegate instance}.
 *
 * @see CommandDispatcherDelegate
 */
@Internal
public class DelegatingCommandDispatcher<I> implements CommandDispatcher<I> {

    /**
     * A target delegate.
     */
    private final CommandDispatcherDelegate<I> delegate;

    private DelegatingCommandDispatcher(CommandDispatcherDelegate<I> delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a new instance of {@code DelegatingCommandDispatcher}, proxying the calls
     * to the passed {@code delegate}.
     *
     * @param delegate a delegate to pass the dispatching duties to
     */
    public static <I> DelegatingCommandDispatcher<I> of(CommandDispatcherDelegate<I> delegate) {
        return new DelegatingCommandDispatcher<>(delegate);
    }

    @Override
    public final Set<CommandClass> messageClasses() {
        return delegate.commandClasses();
    }

    @Override
    public final I dispatch(CommandEnvelope envelope) {
        return delegate.dispatchCommand(envelope);
    }

    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        delegate.onError(envelope, exception);
    }

    /**
     * Returns the string representation of this dispatcher.
     *
     * <p>Includes an FQN of the {@code delegate} in order to allow distinguish
     * {@code DelegatingCommandDispatcher} instances with different delegates.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("commandDelegate", delegate.getClass())
                          .toString();
    }
}
