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

import io.spine.core.EnrichableMessageContext;

import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class performs enrichment conversion using the passed function.
 *
 * @param <S> the type of the field in the source event message
 * @param <T> the type of the field in the target enrichment message
 * @param <C> the type of the event context
 */
final class FieldEnrichment<S, C extends EnrichableMessageContext, T>
        extends EnrichmentFunction<S, C, T> {

    /** A function, which performs the translation. */
    private final BiFunction<S, C, T> function;

    private FieldEnrichment(Class<S> source, Class<T> target, BiFunction<S, C, T> func) {
        super(source, target);
        this.function = checkNotNull(func);
    }

    /**
     * Creates a new instance.
     *
     * @param source
     *         a class of the field in the source message
     * @param target
     *         a class of the field in the enrichment message
     * @param func
     *         a conversion function
     * @return a new instance
     */
    static <S, T, C extends EnrichableMessageContext>
    FieldEnrichment<S, C, T> of(Class<S> source, Class<T> target, BiFunction<S, C, T> func) {
        return new FieldEnrichment<>(source, target, func);
    }

    /**
     * Does nothing. Field enrichment relies only on the aggregated function.
     */
    @Override
    void activate() {
        // Do nothing.
    }

    /**
     * The instances of {@code FieldEnricher} are always active,
     * as no special actions are required for the activation.
     */
    @Override
    boolean isActive() {
        return true;
    }

    @Override
    public T apply(S sourceField, C context) {
        ensureActive();
        T result = function.apply(sourceField, context);
        return result;
    }
}
