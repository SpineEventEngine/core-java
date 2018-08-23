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

package io.spine.server.event.model.given.reactor;

import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.server.event.EventReactor;
import io.spine.testing.server.model.ModelTests;

import java.lang.reflect.Method;

/**
 * Abstract base for test environment classes that allows to obtain a method reference.
 *
 * <p>The derived classes must declare a method named {@link #REACTOR_METHOD_NAME "react()"}
 * with appropriate arguments.
 *
 * @author Alexander Yevsyukov
 */
public class TestEventReactor implements EventReactor {

    private static final String REACTOR_METHOD_NAME = "react";

    private final Any producerId = Identifier.pack(getClass().getName());

    @Override
    public Any getProducerId() {
        return producerId;
    }

    @Override
    public Version getVersion() {
        return Version.getDefaultInstance();
    }


    public Method getMethod() {
        return ModelTests.getMethod(getClass(), REACTOR_METHOD_NAME);
    }
}
