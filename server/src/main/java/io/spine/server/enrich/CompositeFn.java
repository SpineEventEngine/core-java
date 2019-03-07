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

package io.spine.server.enrich;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.EnrichableMessageContext;
import io.spine.core.Enrichment.Container;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A schema function which contains two or more functions on the same enrichable message type.
 */
final class CompositeFn<M extends Message, C extends EnrichableMessageContext>
        extends SchemaFn<M, C> {

    private final ImmutableSet<EnrichmentFn<M, C, ? extends Message>> functions;

    CompositeFn(Iterable<EnrichmentFn<M, C, ? extends Message>> functions) {
        super();
        checkNotNull(functions);
        ImmutableSet<EnrichmentFn<M, C, ?>> fns = ImmutableSet.copyOf(functions);
        int size = fns.size();
        checkArgument(
                size >= 2,
                "Composite enrichment function must have at least two items. Passed: %s.",
                size
        );
        this.functions = fns;
    }

    @Override
    protected void applyAndPut(Container.Builder container, M m, C c) {
        for (EnrichmentFn<M, C, ?> function : functions) {
            Message enrichment = function.apply(m, c);
            checkResult(enrichment, m, c, function);
            put(container, enrichment);
        }
    }
}
