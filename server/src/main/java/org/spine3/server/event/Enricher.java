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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import org.spine3.base.Event;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code Enricher} extends information of an event basing on its type and content.
 *
 * <p>The interface implements
 * <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/DataEnricher.html">ContentEnricher</a>
 * Enterprise Integration pattern.
 *
 * <p>There is one instance of an {@code Enricher} per {@code BoundedContext}. This instance is called by an
 * {@link EventBus} of to enrich a new event before it is passed to further processing by dispatchers or handlers.
 *
 * <p>The event is passed to enrichment <em>after</em> it was passed to the {@link EventStore}.
 *
 * @author Alexander Yevsyukov
 */
public interface Enricher {

    /**
     * Verifies if the passed event class can be enriched.
     *
     * <p>An event can be enriched if the following conditions are met:
     *
     * <ol>
     *     <li>There is one or more functions registered for an {@link EnrichmentFunction} where
     *     the passed class is the {@code source}.
     *     <li>The flag {@code do_not_enrich} is not set in the {@code EventContext} of the passed event.
     * </ol>
     *
     * @return {@code true} if the enrichment for the event is possible, {@code false} otherwise
     */
    boolean canBeEnriched(Event event);

    /**
     * Enriches the passed event.
     *
     * @throws IllegalArgumentException if the passed event cannot be enriched
     * @see #canBeEnriched(Event)
     */
    Event enrich(Event event);

    /**
     * The {@code Builder} allows to register {@link EnrichmentFunction}s handled by the {@code Enricher}
     * and set a custom translation function, if needed.
     */
    class Builder {
        /**
         * A map from an enrichment type to a translation function which performs the enrichment.
         */
        private final Set<EnrichmentFunction<? extends Message, ? extends Message>> functions = Sets.newHashSet();

        /**
         * Adds an {@code EnrichmentType} with a custom translation function.
         */
        public <M extends Message, E extends Message> Builder add(EnrichmentFunction<M, E> function) {
            checkNotNull(function);
            checkDuplicate(function);
            functions.add(function);
            return this;
        }

        /**
         * @throws IllegalArgumentException if the builder already has a function, which has the same couple of
         * source and target classes
         */
        private <M extends Message, E extends Message> void checkDuplicate(EnrichmentFunction<M, E> function) {
            final Optional<EnrichmentFunction<? extends Message, ? extends Message>> duplicate =
                    FluentIterable.from(functions)
                                  .firstMatch(new SameTransition(function));
            if (duplicate.isPresent()) {
                final String msg = String.format("Enrichment from %s to %s already added with function: %s ",
                        function.getSourceClass(),
                        function.getTargetClass(),
                        duplicate.get().getTranslator());
                throw new IllegalArgumentException(msg);
            }
        }

        /**
         * A helper predicate that allows to find functions with the same transition from
         * source to target class
         *
         * <p>Such functions are not necessarily equal because they may have different translators.
         * @see EnrichmentFunction
         */
        private static class SameTransition implements Predicate<EnrichmentFunction> {

            private final EnrichmentFunction function;

            private SameTransition(EnrichmentFunction function) {
                this.function = checkNotNull(function);
            }

            @Override
            public boolean apply(@Nullable EnrichmentFunction input) {
                if (input == null) {
                    return false;
                }
                final boolean sameSourceClass = function.getSourceClass()
                                                        .equals(input.getSourceClass());
                final boolean sameTargetClass = function.getTargetClass()
                                                        .equals(input.getTargetClass());
                return sameSourceClass && sameTargetClass;
            }
        }

        /**
         * Removes a translation for the passed type.
         */
        public Builder remove(EnrichmentFunction entry) {
            functions.remove(entry);
            return this;
        }

        /**
         * Creates new {@code Enricher}.
         */
        public Enricher build() {
            final EnricherImpl result = new EnricherImpl(this);
            // Inject EnricherImpl into default function instances.

            return result;
        }

        /* package */ Set<EnrichmentFunction<?, ?>> getFunctions() {
            return Collections.unmodifiableSet(functions);
        }
    }
}
