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

package io.spine.testing.server.expected;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;

import java.util.List;
import java.util.function.Consumer;

/**
 * Assertions for a commanding method results.
 *
 * @param <S> the type of the tested entity state
 * @author Alexander Yevsyukov
 */
public class CommanderExpected<S extends Message>
    extends MessageProducingExpected<S, CommanderExpected<S>>{

    public CommanderExpected(List<? extends Message> events,
                             S initialState,
                             S state,
                             List<Message> interceptedCommands) {
        super(events, initialState, state, interceptedCommands);
    }

    @Override
    protected CommanderExpected<S> self() {
        return this;
    }

    @CanIgnoreReturnValue
    @Override
    public CommanderExpected<S> producesCommands(Class<?>... messageClass) {
        return super.producesCommands(messageClass);
    }

    /**
     * A commander method may ignore incoming <em>event</em> message.
     * It must process incoming command message.
     */
    @CanIgnoreReturnValue
    @Override
    public CommanderExpected<S> ignoresMessage() {
        return super.ignoresMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote Overrides to expose the method.
     */
    @CanIgnoreReturnValue
    @Override
    public <M extends Message>
    CommanderExpected<S> producesCommand(Class<M> messageClass, Consumer<M> validator) {
        return super.producesCommand(messageClass, validator);
    }
}
