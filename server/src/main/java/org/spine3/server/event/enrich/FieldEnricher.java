/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Message;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class performs enrichment conversion using the passed function.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class FieldEnricher<M extends Message, E extends Message> extends EnrichmentFunction<M, E> {

    /**
     * A function, which performs the enrichment.
     */
    private final Function<M, E> function;

    private FieldEnricher(Class<M> sourceClass, Class<E> targetClass, Function<M, E> function) {
        super(sourceClass, targetClass);
        this.function = checkNotNull(function);
    }

    /**
     * Creates a new instance.
     *
     * @param source a class of the field in the event message
     * @param target a class of the field in the enrichment message
     * @param translator a conversion function
     * @param <M> the type of the field in the event message
     * @param <E> the type of the field in the enrichment message
     * @return new instance
     */
    /* package */ static <M extends Message, E extends Message>
    FieldEnricher<M, E> newInstance(Class<M> source, Class<E> target, Function<M, E> translator) {
        final FieldEnricher<M, E> result = new FieldEnricher<>(source, target, translator);
        return result;
    }

    @Override
    /* package */ void validate() {
        // Do nothing. Field enrichment relies only on the aggregated function.
    }

    @Override
    public Function<M, E> getFunction() {
        return function;
    }

    @Override
    @Nullable
    public E apply(@Nullable M message) {
        if (message == null) {
            return null;
        }
        final E result = function.apply(message);
        return result;
    }


}
