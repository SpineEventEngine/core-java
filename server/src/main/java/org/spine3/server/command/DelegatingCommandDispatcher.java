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
package org.spine3.server.command;

import org.spine3.Internal;
import org.spine3.base.CommandClass;
import org.spine3.base.CommandEnvelope;

import java.util.Set;

/**
 * A {@link CommandDispatcher}, that delegates the responsibilities to a wrapped instance of any
 * type.
 *
 * <p>Should be used to declare that execute dutues of a {@code CommandDispatcher}, but aren't able
 * to conform its contract directly.
 *
 * @author Alex Tymchenko
 * @see CommandDispatcherDelegate
 */
@Internal
public class DelegatingCommandDispatcher implements CommandDispatcher {

    /**
     * A target delegate.
     */
    private final CommandDispatcherDelegate delegate;

    private DelegatingCommandDispatcher(CommandDispatcherDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a new instance of {@code DelegatingCommandDispatcher}, proxying the calls
     * to the passed {@code delegate}.
     *
     * @param delegate a delegate to pass the dispatching duties to
     */
    public static DelegatingCommandDispatcher of(CommandDispatcherDelegate delegate) {
        return new DelegatingCommandDispatcher(delegate);
    }

    @Override
    public final Set<CommandClass> getMessageClasses() {
        return delegate.getCommandClasses();
    }

    @Override
    public final void dispatch(CommandEnvelope envelope) {
        delegate.dispatchCommand(envelope);
    }
}
