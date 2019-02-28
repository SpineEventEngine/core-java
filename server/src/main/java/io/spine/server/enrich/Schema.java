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
import com.google.protobuf.Message;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Contains enrichment functions.
 */
final class Schema {

    private final ImmutableMap<EnricherBuilder.Key, EnrichmentFn<?, ?, ?>> functions;

    Schema(ImmutableMap<EnricherBuilder.Key, EnrichmentFn<?, ?, ?>> functions) {
        this.functions = functions;
    }

    Set<EnrichmentFn<?, ?, ?>> enrichmentOf(Class<? extends Message> cls) {
        //TODO:2019-02-28:alexander.yevsyukov: Compactify functions on creation to avoid this
        // search every time.
        Set<EnrichmentFn<?, ?, ?>> found =
                functions.entrySet()
                         .stream()
                         .filter(e -> cls.equals(e.getKey().sourceClass()))
                         .map(Map.Entry::getValue)
                         .collect(toSet());
        return found;
    }
}
