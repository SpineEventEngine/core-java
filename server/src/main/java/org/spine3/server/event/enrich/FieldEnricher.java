/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event.enrich;

import com.google.common.base.Function;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class performs enrichment conversion using the passed function.
 *
 * @param <S> the type of the field in the source event message
 * @param <T> the type of the field in the target enrichment message
 * @author Alexander Yevsyukov
 */
class FieldEnricher<S, T> extends EnrichmentFunction<S, T> {

    /** A function, which performs the enrichment. */
    private final Function<S, T> function;

    private FieldEnricher(Class<S> eventClass, Class<T> enrichmentClass, Function<S, T> function) {
        super(eventClass, enrichmentClass);
        this.function = checkNotNull(function);
    }

    /**
     * Creates a new instance.
     *
     * @param eventFieldClass      a class of the field in the event message
     * @param enrichmentFieldClass a class of the field in the enrichment message
     * @param translator           a conversion function
     * @return a new instance
     */
    static <S, T> FieldEnricher<S, T> newInstance(Class<S> eventFieldClass,
                                                                Class<T> enrichmentFieldClass,
                                                                Function<S, T> translator) {
        final FieldEnricher<S, T> result = new FieldEnricher<>(eventFieldClass, enrichmentFieldClass, translator);
        return result;
    }

    /**
     * Do nothing. Field enrichment relies only on the aggregated function.
     */
    @Override
    void activate() {}

    /**
     * The instances of {@code FieldEnricher} are always active,
     * as no special actions are required for the activation.
     */
    @Override
    boolean isActive() {
        return true;
    }

    @Override
    public Function<S, T> getFunction() {
        return function;
    }

    @Override
    @Nullable
    public T apply(@Nullable S message) {
        ensureActive();
        if (message == null) {
            return null;
        }
        final T result = function.apply(message);
        return result;
    }
}
