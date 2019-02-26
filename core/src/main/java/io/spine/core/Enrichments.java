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

package io.spine.core;

import com.google.protobuf.Any;
import io.spine.base.EnrichmentMessage;
import io.spine.core.Enrichment.Container;
import io.spine.core.Enrichment.ModeCase;
import io.spine.type.TypeName;

import java.util.Optional;

import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Utility class for working with event enrichments.
 */
final class Enrichments {

    /** Prevents instantiation of this utility class. */
    private Enrichments() {
    }

    /**
     * Obtains the container of enrichments from the passed enclosing instance,
     * if it its {@link ModeCase} allows for having enrichments.
     *
     * <p>Otherwise, empty {@code Optional} is returned.
     */
    static Optional<Container> container(Enrichment enrichment) {
        if (enrichment.getModeCase() == ModeCase.CONTAINER) {
            return Optional.of(enrichment.getContainer());
        }
        return Optional.empty();
    }

    /**
     * Obtains enrichment from the passed container.
     */
    static <E extends EnrichmentMessage>
    Optional<E> find(Class<E> enrichmentClass, Container enrichments) {
        String typeName = TypeName.of(enrichmentClass)
                                  .value();
        Any any = enrichments.getItemsMap()
                             .get(typeName);
        Optional<E> result = Optional.ofNullable(any)
                                     .map(packed -> unpack(packed, enrichmentClass));
        return result;
    }
}
