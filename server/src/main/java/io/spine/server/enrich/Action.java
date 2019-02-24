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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.EnrichableMessageContext;
import io.spine.core.Enrichment;
import io.spine.core.Enrichment.Container;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Performs enrichment operation for an enrichable message.
 */
final class Action {

    /** The message to be enriched. */
    private final Message message;

    /** The context of the enrichable message. */
    private final EnrichableMessageContext context;

    /** Active functions applicable to the enrichable message. */
    private final ImmutableList<MessageEnrichment> functions;

    /** The builder of the container of produced enrichments. */
    private final Container.Builder container = Container.newBuilder();

    Action(Enricher parent, Message message, EnrichableMessageContext context) {
        this.message = message;
        this.context = context;
        Class<? extends Message> sourceClass = message.getClass();
        this.functions = parent.schema()
                               .get(sourceClass)
                               .stream()
                               .map(MessageEnrichment.class::cast)
                               .collect(toImmutableList());
    }

    /**
     * Creates a new instance of {@code Enrichment} containing all enrichments of the passed
     * message.
     */
    Enrichment perform() {
        createEnrichments();
        Enrichment result = wrap();
        return result;
    }

    /**
     * Creates enrichments by invoking all the {@link #functions}, and puts the created enrichment
     * messages into the {@link #container}.
     */
    private void createEnrichments() {
        for (MessageEnrichment function : functions) {
            @SuppressWarnings("unchecked") // OK since we cast to most common interface.
            Message enrichment = function.apply(message, context);
            checkResult(enrichment, function);
            put(enrichment);
        }
    }

    /**
     * Puts the passed enrichment message into the {@link #container}.
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    private void put(Message enrichment) {
        String typeName = TypeName.of(enrichment)
                                  .value();
        container.putItems(typeName, AnyPacker.pack(enrichment));
    }

    private void checkResult(@Nullable Message enriched, MessageEnrichment function) {
        checkNotNull(
            enriched,
            "MessageEnrichment `%s` produced `null` for the source message `%s`.",
            function, message
        );
    }

    /**
     * Creates a new {@link Enrichment} instance wrapping recently created enrichments.
     */
    private Enrichment wrap() {
        Enrichment result = Enrichment
                .newBuilder()
                .setContainer(container)
                .build();
        return result;
    }
}
