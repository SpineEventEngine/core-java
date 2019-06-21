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

package io.spine.server.command.model.given.handler;

import io.spine.server.BoundedContextBuilder;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.testing.server.model.ModelTests;

import java.lang.reflect.Method;

/**
 * Abstract base for test environment command handlers.
 *
 * <p>Derived classes must declare a method named {@linkplain #HANDLER_METHOD_NAME handleTest}
 * so that the method can be {@linkplain #getHandler() obtained} by the code of tests.
 */
public abstract class TestCommandHandler extends AbstractCommandHandler {

    private static final String HANDLER_METHOD_NAME = "handleTest";

    protected TestCommandHandler() {
        super(BoundedContextBuilder
                      .assumingTests(true)
                      .build()
                      .eventBus());
    }

    public Method getHandler() {
        return ModelTests.getMethod(getClass(), HANDLER_METHOD_NAME);
    }
}
