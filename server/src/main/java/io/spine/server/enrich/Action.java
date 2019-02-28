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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.EnrichableMessageContext;
import io.spine.core.Enrichment;
import io.spine.core.Enrichment.Container;
import io.spine.protobuf.AnyPacker;
import io.spine.type.MessageClass;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Performs enrichment operation for an enrichable message.
 */
final class Action {

    /** The enricher for which this action works. */
    private final Enricher parent;

    /** The message to be enriched. */
    private final Message message;

    /** The class of the source message. */
    private final Class<? extends Message> sourceClass;

    /** The context of the enrichable message. */
    private final EnrichableMessageContext context;

    /** Active functions applicable to the enrichable message. */
    private final Set<EnrichmentFn> functions = new HashSet<>();

    /** The builder of the container of produced enrichments. */
    private final Container.Builder container = Container.newBuilder();

    Action(Enricher parent, Message message, EnrichableMessageContext context) {
        this.parent = parent;
        this.message = message;
        this.sourceClass = message.getClass();
        this.context = context;
        collectFunctions();
    }

    private void collectFunctions() {
        collectForClass(sourceClass);
        collectForInterfacesOf(sourceClass);
    }

    private void collectForClass(Class<? extends Message> cls) {
        functions.addAll(parent.enrichmentOf(cls));
    }

    private void collectForInterfacesOf(Class<? extends Message> cls) {
        ImmutableSet<Class<? extends Message>> superInterfaces = MessageClass.interfacesOf(cls);

        superInterfaces.forEach(i -> {
            collectForClass(i);
            collectForInterfacesOf(i);
        });
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
        for (EnrichmentFn function : functions) {
            @SuppressWarnings("unchecked") // OK since we cast to most common interface.
            Message enrichment = (Message) function.apply(message, context);
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

    private void checkResult(@Nullable Message enrichment, EnrichmentFn function) {
        checkNotNull(
            enrichment,
            "EnrichmentFn `%s` produced `null` for the source message `%s`.",
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
