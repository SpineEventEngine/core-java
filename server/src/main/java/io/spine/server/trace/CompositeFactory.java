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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import io.spine.core.BoundedContextName;
import io.spine.core.Signal;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

@Immutable
final class CompositeFactory implements TracerFactory {

    private final ImmutableList<TracerFactory> delegateFactories;

    CompositeFactory(ImmutableList<TracerFactory> factories) {
        this.delegateFactories = checkNotNull(factories);
    }

    @Override
    public TracerFactory inContext(BoundedContextName context) {
        ImmutableList<TracerFactory> factoriesInContext = delegateFactories
                .stream()
                .map(factory -> factory.inContext(context))
                .collect(toImmutableList());
        return new CompositeFactory(factoriesInContext);
    }

    @Override
    public TracerFactory outOfContext() {
        ImmutableList<TracerFactory> factoriesInContext = delegateFactories
                .stream()
                .map(TracerFactory::outOfContext)
                .collect(toImmutableList());
        return new CompositeFactory(factoriesInContext);
    }

    @Override
    public Tracer trace(Signal<?, ?, ?> signalMessage) {
        ImmutableList<Tracer> tracers = delegateFactories
                .stream()
                .map(factory -> factory.trace(signalMessage))
                .collect(toImmutableList());
        return new CompositeTracer(signalMessage, tracers);
    }
}
