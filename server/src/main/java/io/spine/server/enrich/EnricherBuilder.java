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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.type.MessageClass.interfacesOf;

/**
 * The {@code Builder} allows to register enrichment functions used by
 * the {@code Enricher}.
 */
public final class EnricherBuilder {

    private static final String SUGGEST_REMOVAL = " Please call `remove(Class, Class)` first.";

    /**
     * Maps a pair of [source class, enrichment class] to a function which produces this enrichment.
     */
    private final Map<Key, EnrichmentFn<?, ?, ?>> functions = new HashMap<>();

    /** Creates new instance. */
    EnricherBuilder() {
    }

    /**
     * Adds event enrichment function to the builder.
     *
     * @param <M>
     *         the type of the event message
     * @param <T>
     *         the type of the enrichment message
     * @param eventClass
     *         the class of the events the passed function enriches
     * @param enrichmentClass
     *         the class of the enrichments the passed function produces
     * @param func
     *         the enrichment function
     * @return {@code this} builder
     * @throws IllegalStateException
     *         if the builder already contains a function for this event class
     * @see #remove(Class, Class)
     */
    @CanIgnoreReturnValue
    public <M extends EventMessage, T extends Message>
    EnricherBuilder add(Class<M> eventClass, Class<T> enrichmentClass,
                        EventEnrichmentFn<M, T> func) {
        checkNotNull(eventClass);
        checkNotNull(enrichmentClass);
        checkNotNull(func);
        Key key = new Key(eventClass, enrichmentClass);
        checkDirectDuplication(key);
        checkInterfaceDuplication(key);
        functions.put(key, func);
        return this;
    }

    private void checkDirectDuplication(Key key) {
        checkArgument(
                !functions.containsKey(key),
                "The function which enriches `%s` with `%s` is already added." + SUGGEST_REMOVAL,
                key.sourceClass().getCanonicalName(),
                key.enrichmentClass().getCanonicalName()
        );
    }

    private void checkInterfaceDuplication(Key key) {
        Class<? extends Message> sourceClass = key.sourceClass();
        Class<? extends Message> enrichmentClass = key.enrichmentClass();
        ImmutableSet<Class<? extends Message>> interfaces = interfacesOf(sourceClass);
        interfaces.forEach(i -> {
            Key keyByInterface = new Key(i, enrichmentClass);
            checkArgument(
                    !functions.containsKey(keyByInterface),
                    "Enrichment message of the class `%s` is already available via a function" +
                            " which accepts `%s`, which is a super interface of the class `%s`." +
                            SUGGEST_REMOVAL,
                    enrichmentClass, i.getCanonicalName(), sourceClass.getCanonicalName()
            );
        });
    }

    /**
     * Removes the enrichment function for the passed event class.
     *
     * <p>If the function for this class was not added, the call has no effect.
     */
    public <M extends EventMessage, T extends Message>
    EnricherBuilder remove(Class<M> eventClass, Class<T> enrichmentClass) {
        functions.remove(new Key(eventClass, enrichmentClass));
        return this;
    }

    /** Creates a new {@code Enricher}. */
    public Enricher build() {
        Enricher result = new Enricher(this);
        return result;
    }

    /**
     * Obtains immutable functions of functions added to the builder by the time of the call.
     */
    ImmutableMap<Key, EnrichmentFn<?, ?, ?>> functions() {
        return ImmutableMap.copyOf(functions);
    }

    /**
     * A pair of source message class and enrichment message class, which is used to match
     * the pair to a function which produces the enrichment.
     */
    @Immutable
    static class Key {

        private final Class<? extends Message> sourceClass;
        private final Class<? extends Message> enrichmentClass;

        Key(Class<? extends Message> sourceClass, Class<? extends Message> enrichmentClass) {
            this.sourceClass = checkNotNull(sourceClass);
            this.enrichmentClass = checkNotNull(enrichmentClass);
        }

        Class<? extends Message> sourceClass() {
            return sourceClass;
        }

        Class<? extends Message> enrichmentClass() {
            return enrichmentClass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceClass, enrichmentClass);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key) obj;
            return Objects.equals(this.sourceClass, other.sourceClass)
                    && Objects.equals(this.enrichmentClass, other.enrichmentClass);
        }
    }
}
