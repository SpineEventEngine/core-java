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
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;

/**
 * Contains enrichment functions.
 */
final class Schema {

    private final ImmutableMap<Key, EnrichmentFn<?, ?, ?>> functions;

    Schema(ImmutableMap<Key, EnrichmentFn<?, ?, ?>> functions) {
        this.functions = functions;
    }

    Set<EnrichmentFn<?, ?, ?>> enrichmentOf(Class<? extends Message> cls) {
        //TODO:2019-02-28:alexander.yevsyukov: Compactify functions on creation to avoid this
        // search every time.
        Set<EnrichmentFn<?, ?, ?>> found =
                functions.entrySet()
                         .stream()
                         .filter(e -> cls.equals(e.getKey().sourceClass))
                         .map(Map.Entry::getValue)
                         .collect(toSet());
        return found;
    }

    /**
     * The key in the schema map.
     */
    @Immutable
    static class Key {

        private final Class<?> sourceClass;
        private final Class<?> enrichmentClass;

        Key(Class<?> sourceClass, Class<?> enrichmentClass) {
            this.sourceClass = checkNotNull(sourceClass);
            this.enrichmentClass = checkNotNull(enrichmentClass);
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
