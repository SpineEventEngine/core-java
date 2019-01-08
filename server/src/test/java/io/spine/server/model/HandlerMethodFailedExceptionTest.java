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

package io.spine.server.model;

import io.spine.core.CommandEnvelope;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("HandlerMethodFailedException should")
class HandlerMethodFailedExceptionTest {

    private final TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());

    private HandlerMethodFailedException exception;

    private CommandEnvelope commandEnvelope;
    private Exception cause;

    @BeforeEach
    void setUp() {
        commandEnvelope = factory.generateEnvelope();
        cause = new IllegalStateException(newUuid());

        exception = new HandlerMethodFailedException(this, commandEnvelope.getMessage(),
                                                     commandEnvelope.getCommandContext(),
                                                     cause);
    }

    @Test
    @DisplayName("return target")
    void returnTarget() {
        // We passed `this` as the failed object, so we expect its `toString()` as `target`.
        assertEquals(this.toString(), exception.getTarget());
    }

    @Test
    @DisplayName("return DispatchedMessage")
    void returnDispatchedMessage() {
        assertEquals(commandEnvelope.getMessage(), exception.getDispatchedMessage());
    }

    @Test
    @DisplayName("return MessageContext")
    void returnMessageContext() {
        assertEquals(commandEnvelope.getCommandContext(), exception.getMessageContext());
    }

    @Test
    @DisplayName("have cause")
    void haveCause() {
        assertEquals(cause, exception.getCause());
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
