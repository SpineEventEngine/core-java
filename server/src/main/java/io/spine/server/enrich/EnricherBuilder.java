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
import io.spine.base.EventMessage;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * The {@code Builder} allows to register enrichment functions used by
 * the {@code Enricher}.
 */
public final class EnricherBuilder {

    private final Map<Class<? extends Message>, EnrichmentFn<?, ?, ?>> functions = new HashMap<>();

    /** Creates new instance. */
    EnricherBuilder() {
    }

    /**
     * Adds event enrichment function to the builder.
     *
     * @param eventClass
     *         the class of the events the passed function enriches
     * @param func
     *         the enrichment function
     * @param <M>
     *         the type of the event message
     * @param <T>
     *         the type of the enrichment message
     * @return {@code this} builder
     * @throws IllegalStateException
     *         if the builder already contains a function for this event class
     * @see #remove(Class)
     */
    public <M extends EventMessage, T extends Message>
    EnricherBuilder add(Class<M> eventClass, EventEnrichmentFn<M, T> func) {
        checkNotNull(eventClass);
        checkNotNull(func);
        checkState(
                !functions.containsKey(eventClass),
                "The event class `%s` already has enrichment function." +
                        " If you want to provide another function, please call `remove()` first.",
                eventClass.getCanonicalName()
        );
        functions.put(eventClass, func);
        return this;
    }

    /**
     * Removes the enrichment function for the passed event class.
     *
     * <p>If the function for this class was not added, the call has no effect.
     */
    public <M extends EventMessage> EnricherBuilder remove(Class<M> eventClass) {
        functions.remove(eventClass);
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
    ImmutableMap<Class<? extends Message>, EnrichmentFn<?, ?, ?>> functions() {
        return ImmutableMap.copyOf(functions);
    }
}
