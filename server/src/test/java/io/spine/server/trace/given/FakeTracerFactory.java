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

package io.spine.server.trace.given;

import com.google.protobuf.Message;
import io.spine.core.BoundedContextName;
import io.spine.core.Signal;
import io.spine.server.trace.Tracer;
import io.spine.server.trace.TracerFactory;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("Immutable")
public final class FakeTracerFactory implements TracerFactory {

    private final Map<Class<? extends Message>, FakeTracer> tracers = newHashMap();
    private boolean closed = false;

    @Override
    public TracerFactory inContext(BoundedContextName context) {
        return this;
    }

    @Override
    public TracerFactory outOfContext() {
        return this;
    }

    @Override
    public Tracer trace(Signal<?, ?, ?> signalMessage) {
        Class<? extends Message> messageType = signalMessage.enclosedMessage()
                                                            .getClass();
        FakeTracer tracer = tracers.computeIfAbsent(messageType,
                                                    cls -> new FakeTracer(signalMessage));
        return tracer;
    }

    @Override
    public void close() {
        closed = true;
        tracers.clear();
    }

    public boolean closed() {
        return closed;
    }

    public FakeTracer tracer(Class<? extends Message> messageType) {
        FakeTracer tracer = tracers.get(messageType);
        assertNotNull(tracer);
        return tracer;
    }
}
