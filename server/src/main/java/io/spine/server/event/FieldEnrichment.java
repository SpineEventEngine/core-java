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

package io.spine.server.event;

import com.google.protobuf.Message;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class performs enrichment conversion using the passed function.
 *
 * @param <S> the type of the field in the source event message
 * @param <T> the type of the field in the target enrichment message
 *
 * @author Alexander Yevsyukov
 */
final class FieldEnrichment<S, T, C extends Message> extends EnrichmentFunction<S, T, C> {

    /** A function, which performs the translation. */
    private final Function<S, T> function;

    private FieldEnrichment(Class<S> eventClass, Class<T> enrichmentClass, Function<S, T> func) {
        super(eventClass, enrichmentClass);
        this.function = checkNotNull(func);
    }

    /**
     * Creates a new instance.
     *
     * @param  messageFieldClass
     *         a class of the field in the source message
     * @param  enrichmentFieldClass
     *         a class of the field in the enrichment message
     * @param  func
     *         a conversion function
     * @return a new instance
     */
    static <S, T, C extends Message>
    FieldEnrichment<S, T, C> of(Class<S> messageFieldClass,
                                Class<T> enrichmentFieldClass,
                                Function<S, T> func) {
        FieldEnrichment<S, T, C> result =
                new FieldEnrichment<>(messageFieldClass, enrichmentFieldClass, func);
        return result;
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
    public T apply(S message, C context) {
        ensureActive();
        T result = function.apply(message);
        return result;
    }
}
