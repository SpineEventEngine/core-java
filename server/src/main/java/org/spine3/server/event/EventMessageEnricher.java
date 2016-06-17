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

package org.spine3.server.event;

import com.google.common.base.Function;
import com.google.protobuf.Message;

import javax.annotation.Nullable;

/**
 * The default mechanism for enriching messages based on {@code FieldOptions} of
 * Protobuf message definitions.
 *
 * @param <M> a type of the message of the event to enrich
 * @param <E> a type of the enrichment message
 *
 * @author Alexander Yevsyukov
 */
/* package */ class EventMessageEnricher<M extends Message, E extends Message> extends EnrichmentFunction<M, E> {

    private EventMessageEnricher(EventEnricher enricher, Class<M> sourceClass, Class<E> targetClass) {
        super(sourceClass, targetClass, new Func<>(enricher, sourceClass, targetClass));
    }

    /**
     * Creates a new instance for enriching events.
     *
     * @param source a class of the event message to enrich
     * @param target a class of the target enrichment
     * @param <M> the type of the event message
     * @param <E> the type of the enrichment message
     * @return new enrichment function with {@link EventMessageEnricher}
     */
    /* package */ static <M extends Message, E extends Message>
    EnrichmentFunction<M, E> unboundInstance(Class<M> source, Class<E> target) {
        final EnrichmentFunction<M, E> result = new Unbound<>(source, target);
        return result;
    }

    @Override
    void validate() {
        //TODO:2016-06-17:alexander.yevsyukov: Implement checking
    }

    /**
     * An interim entry in the {@link EventEnricher.Builder} to hold information about the types.
     */
    /* package */ static class Unbound<M extends Message, E extends Message> extends EnrichmentFunction<M, E> {

        /* package */Unbound(Class<M> sourceClass, Class<E> targetClass) {
            super(sourceClass, targetClass, Unbound.<M, E>empty());
        }

        private static <M extends Message, E extends Message> Function<M, E> empty() {
            return new Function<M, E>() {
                @Nullable
                @Override
                public E apply(@Nullable M input) {
                    return null;
                }
            };
        }

        /**
         * Converts the instance into the {@link EventMessageEnricher}.
         */
        /* package */ EventMessageEnricher<M, E> toBound(EventEnricher enricher) {
            final EventMessageEnricher<M, E> result = new EventMessageEnricher<>(enricher, getSourceClass(), getTargetClass());
            return result;
        }

        /**
         * @throws IllegalStateException always to prevent the usage of instances in configured {@link EventEnricher}
         */
        @Override
        /* package */void validate() {
            throw new IllegalStateException("Unbound instance cannot be used for enrichments");
        }
    }

    private static class Func<M extends Message, E extends Message> implements Function<M, E> {

        private final EventEnricher enricher;
        private final Class<M> sourceClass;
        private final Class<E> targetClass;

        private Func(EventEnricher enricher, Class<M> sourceClass, Class<E> targetClass) {
            this.enricher = enricher;
            this.sourceClass = sourceClass;
            this.targetClass = targetClass;
        }

        @Nullable
        @Override
        public E apply(@Nullable M input) {
            //TODO:2016-06-17:alexander.yevsyukov: Implement
            return null;
        }

        /* package */ EventEnricher getEnricher() {
            return enricher;
        }
    }

    /**
     * Performs validation checking that all fields annotated in the enrichment message
     * can be created with the translation functions supplied in the parent enricher.
     *
     * @throws IllegalStateException if the parent {@code Enricher} does not have a function for a field enrichment
     */
    /* package */ void init() {


        //TODO:2016-06-17:alexander.yevsyukov: Implement validation
    }
}
