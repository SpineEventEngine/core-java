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

package io.spine.server.trace;

import io.spine.annotation.SPI;
import io.spine.core.BoundedContextName;
import io.spine.core.Signal;

/**
 * A factory of {@link Tracer}s of signal messages.
 *
 * <p>Each tracer is created for a single signal and should not be reused.
 *
 * <p>Implementations may choose to perform I/O operations (i.e. send traces via network) on
 * {@code close()}. It is expected nin general that each call to {@link #trace} results in some
 * tracing data produced, however, implementations may ignore some signals.
 */
@SPI
public interface TracerFactory extends AutoCloseable {

    /**
     * Creates a new factory for tracing signals of the given bounded context.
     *
     * <p>The method may return a new factory with the same behaviour as this one but suited for
     * the given context, or simply this instance. The method may NOT change the state of this
     * instance to suite the given context.
     *
     * @param context
     *         the name of the context to trace
     * @return new instance
     */
    TracerFactory inContext(BoundedContextName context);

    /**
     * Creates a new instance of {@link Tracer} for the given signal.
     *
     * <p>The tracer will be closed externally once it is no longer needed.
     *
     * @param signalMessage the message to trace
     * @return new {@code Tracer}
     */
    Tracer trace(Signal<?, ?, ?> signalMessage);
}
