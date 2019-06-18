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

package io.spine.server.trace.stackdriver;

import com.google.api.gax.grpc.GrpcCallContext;
import com.google.errorprone.annotations.Immutable;
import io.spine.core.BoundedContextName;
import io.spine.core.Signal;
import io.spine.server.trace.Tracer;
import io.spine.server.trace.TracerFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class StackdriverTracerFactory implements TracerFactory {

    private final @Nullable BoundedContextName context;
    @SuppressWarnings("Immutable") // Not annotated but in fact immutable.
    private final GrpcCallContext callContext;
    private final String gcpProjectName;

    private StackdriverTracerFactory(Builder builder) {
        this.context = builder.context;
        this.callContext = builder.callContext;
        this.gcpProjectName = builder.gcpProjectName;
    }

    @Override
    public TracerFactory inContext(BoundedContextName context) {
        return toBuilder().setContext(context)
                          .build();
    }

    @Override
    public TracerFactory outOfContext() {
        return toBuilder().clearContext()
                          .build();
    }

    @Override
    public Tracer trace(Signal<?, ?, ?> signalMessage) {
        return new StackdriverTracer(signalMessage, callContext, gcpProjectName, context);
    }

    private Builder toBuilder() {
        return newBuilder()
                .setContext(context)
                .setCallContext(callContext)
                .setGcpProjectName(gcpProjectName);
    }

    /**
     * Creates a new instance of {@code Builder} for {@code StackdriverTracerFactory} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code StackdriverTracerFactory} instances.
     */
    public static final class Builder {

        private @Nullable BoundedContextName context;
        private GrpcCallContext callContext;
        private String gcpProjectName;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        public Builder setContext(@Nullable BoundedContextName context) {
            this.context = context;
            return this;
        }

        public Builder clearContext() {
            return setContext(null);
        }

        public Builder setCallContext(GrpcCallContext callContext) {
            this.callContext = checkNotNull(callContext);
            return this;
        }

        public Builder setGcpProjectName(String gcpProjectName) {
            this.gcpProjectName = checkNotNull(gcpProjectName);
            return this;
        }

        /**
         * Creates a new instance of {@code StackdriverTracerFactory}.
         *
         * @return new instance of {@code StackdriverTracerFactory}
         */
        public StackdriverTracerFactory build() {
            return new StackdriverTracerFactory(this);
        }
    }
}
