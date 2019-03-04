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
import com.google.protobuf.Message;
import io.spine.core.EnrichableMessageContext;
import io.spine.logging.Logging;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Contains enrichment functions.
 */
final class Schema<M extends Message, C extends EnrichableMessageContext> implements Logging {

    private final ImmutableMap<Class<? extends M>, SchemaFn<? extends M, C>> map;

    private final int size;

    static Schema newInstance(EnricherBuilder eBuilder) {
        Factory factory = new Factory(eBuilder);
        Schema result = factory.create();
        return result;
    }

    private Schema(Factory factory) {
        this.map = ImmutableMap.copyOf(factory.schemaMap);
        this.size = factory.functions.size();
        _debug("Created enrichment schema with {} entries.", this.size);
    }

    boolean isEmpty() {
        return size == 0;
    }

    <M extends Message> @Nullable SchemaFn<M, ? extends EnrichableMessageContext>
    enrichmentOf(Class<M> cls) {
        @SuppressWarnings("unchecked") // The type is ensured when we put entries.
        SchemaFn<M, ?> fn = (SchemaFn<M, ?>) map.get(cls);
        return fn;
    }

    /**
     * Creates new {@code Schema}.
     *
     * <p>Transforms functions obtained from {@link EnricherBuilder} into functions
     * used by {@code Schema}, and then creates the instance.
     */
    private static class Factory<M extends Message, C extends EnrichableMessageContext> {

        /** Functions we got from {@link EnricherBuilder}. */
        private final ImmutableMap<EnricherBuilder.Key, EnrichmentFn<M, C, ?>> functions;

        /** The types of messages that these functions enrich. */
        private final ImmutableSet<Class<? extends Message>> sourceTypes;

        /** The map from a class of the enrichable message to the schema function. */
        private final Map<Class<? extends M>, SchemaFn<? extends M, ?>> schemaMap =
                new HashMap<>();

        private Factory(EnricherBuilder<M, C, ?> eBuilder) {
            checkNotNull(eBuilder);
            this.functions = ImmutableMap.copyOf(eBuilder.functions());
            this.sourceTypes =
                    functions.keySet()
                             .stream()
                             .map(EnricherBuilder.Key::sourceClass)
                             .collect(toImmutableSet());
        }

        Schema create() {
            for (Class<? extends Message> sourceType : sourceTypes) {
                SchemaFn<?, ?> fn = createFn(sourceType);
                schemaMap.put(sourceType, fn);
            }

            return new Schema(this);
        }

        private <M extends Message, C extends EnrichableMessageContext>
        SchemaFn<M, C> createFn(Class<M> sourceType) {
            @SuppressWarnings("unchecked") // we check the type when filtering.
            ImmutableSet<EnrichmentFn<M, C, ?>> fns =
                    functions.entrySet()
                             .stream()
                             .filter(e -> sourceType.equals(e.getKey().sourceClass()))
                             .map(e -> (EnrichmentFn<M, C, ?>) e.getValue())
                             .collect(toImmutableSet());
            if (fns.size() == 1) {
                return new SingularFn<>(fns.iterator().next());
            } else {
                return new CompositeFn<M, C>(fns);
            }
        }
    }
}
