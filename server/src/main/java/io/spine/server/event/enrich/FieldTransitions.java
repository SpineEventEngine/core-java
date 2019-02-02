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
import com.google.protobuf.Message;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matches source fields to enrichment fields, and provides functions for the transitions.
 */
final class FieldTransitions {

    private final ImmutableList<EnrichmentFunction<?, ?, ?>> functions;
    private final ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap;

    static FieldTransitions create(Enricher enricher,
                                   Class<? extends Message> sourceClass,
                                   Class<? extends Message> enrichmentClass) {

        Linker linker = new Linker(enricher, sourceClass, enrichmentClass);
        FieldTransitions result = linker.createTransitions();
        return result;
    }

    FieldTransitions(ImmutableList<EnrichmentFunction<?, ?, ?>> functions,
                     ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap) {
        checkNotNull(functions);
        checkNotNull(fieldMap);
        //TODO:2019-02-02:alexander.yevsyukov: Enable the below checks when enrichment schemas are generated per bounded context.
        //checkArgument(!functions.isEmpty(), "No transition functions provided.");
        //checkArgument(!fieldMap.isEmpty(), "`fieldMap` cannot be empty");
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
