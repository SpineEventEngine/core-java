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

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The implementation base for the assertions of message handling results.
 *
 * @param <S> the state type of the tested entity
 * @param <E> the type of the tested entity
 * @author Dmytro Dashenkov
 * @author Vladyslav Lubenskyi
 * @see MessageProducingExpected
 */
public abstract class AbstractExpected<S extends Message, E extends AbstractExpected<S, E>> {

    private final S initialState;
    private final S state;

    protected AbstractExpected(S initialState, S state) {
        this.initialState = initialState;
        this.state = state;
    }

    /**
     * Applies the given assertions to the given entity.
     *
     * @param validator a {@link Consumer} that performs all required assertions for the state
     */
    @CanIgnoreReturnValue
    public E hasState(Consumer<S> validator) {
        validator.accept(state);
        return self();
    }

    /**
     * Asserts that the message was ignored by the entity.
     */
    @CanIgnoreReturnValue
    protected E ignoresMessage() {
        assertEquals(initialState, state);
        return self();
    }

    /**
     * @return {@code this} with the required compile-time type
     */
    protected abstract E self();
}
