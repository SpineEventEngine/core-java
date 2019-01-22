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

package io.spine.server.event.enrich;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Descriptors.FieldDescriptor;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper DTO for the validation result.
 */
final class ValidationResult {

    private final ImmutableList<EnrichmentFunction<?, ?, ?>> functions;
    private final ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap;

    ValidationResult(ImmutableList<EnrichmentFunction<?, ?, ?>> functions,
                     ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap) {
        this.functions = checkNotNull(functions);
        this.fieldMap = checkNotNull(fieldMap);
    }

    /**
     * Returns the validated list of {@code EnrichmentFunction}s that may be used for
     * the conversion in scope of the validated {@code Enricher}.
     */
    List<EnrichmentFunction<?, ?, ?>> functions() {
        return functions;
    }

    /**
     * Returns a map from the descriptor of a source event event message or event context field
     * to the descriptor of the  target enrichment field descriptors, which is valid in the scope of
     * the {@code Enricher}.
     */
    ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap() {
        return fieldMap;
    }

    ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> functionMap() {
        ImmutableMultimap.Builder<Class<?>, EnrichmentFunction<?, ?, ?>> map =
                ImmutableMultimap.builder();
        for (EnrichmentFunction<?, ?, ?> fieldFunction : this.functions) {
            map.put(fieldFunction.sourceClass(), fieldFunction);
        }
        return map.build();
    }
}
