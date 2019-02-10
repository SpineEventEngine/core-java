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
import io.spine.type.TypeName;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Utility class for working with event enrichments.
 */
public final class Enrichments {

    /** Prevents instantiation of this utility class. */
    private Enrichments() {
    }

    /**
     * Returns all enrichments from the context.
     *
     * @param context a context to get enrichments from
     * @return an optional of enrichments
     * @deprecated
     * To verify having enrichments at all, please use {@link #hasEnrichments(EventContext)}.
     * To obtain an enrichment please use {@link io.spine.base.EnrichmentContainer}.
     */
    @Deprecated
    public static Optional<Container> getEnrichments(EventContext context) {
        checkNotNull(context);
        Enrichment enrichment = context.getEnrichment();
        return container(enrichment);
    }

    /**
     * Verifies if the passed event context has at least one enrichment.
     */
    public static boolean hasEnrichments(EventContext context) {
        Optional<Container> optional = container(context.getEnrichment());
        if (!optional.isPresent()) {
            return false;
        }
        Container container = optional.get();
        boolean result = !container.getItemsMap()
                                   .isEmpty();
        return result;
    }

    private static Optional<Container> container(Enrichment enrichment) {
        if (enrichment.getModeCase() == Enrichment.ModeCase.CONTAINER) {
            return Optional.of(enrichment.getContainer());
        }
        return Optional.empty();
    }

    /**
     * Return a specific enrichment from the context.
     *
     * @param  enrichmentClass a class of the event enrichment
     * @param  context         a context to get an enrichment from
     * @param  <E>             a type of the event enrichment
     * @return an optional of the enrichment
     */
    public static <E extends EnrichmentMessage>
    Optional<E> getEnrichment(Class<E> enrichmentClass, EventContext context) {
        checkNotNull(enrichmentClass);
        checkNotNull(context);
        if (!hasEnrichments(context)) {
            return Optional.empty();
        }
        Optional<Container> container = container(context.getEnrichment());
        if (!container.isPresent()) {
            return Optional.empty();
        }
        Optional<E> result = find(enrichmentClass, container.get());
        return result;
    }

    private static <E extends EnrichmentMessage>
    Optional<E> find(Class<E> enrichmentClass, Container enrichments) {
        String typeName = TypeName.of(enrichmentClass)
                                  .value();
        Any any = enrichments.getItemsMap()
                             .get(typeName);
        Optional<E> result = Optional.ofNullable(any)
                                     .map(packed -> unpack(packed, enrichmentClass));
        return result;
    }

    /**
     * Creates a new {@link Enrichment} instance from the passed map.
     */
    static Enrichment createEnrichment(Map<String, Any> enrichments) {
        Enrichment.Builder enrichment =
                Enrichment.newBuilder()
                          .setContainer(Container.newBuilder()
                                                 .putAllItems(enrichments));
        return enrichment.build();
    }
}
