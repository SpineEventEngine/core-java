/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event.enrich;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * The default mechanism for enriching messages based on {@code FieldOptions} of Protobuf message definitions.
 *
 * @param <M> a type of the message of the event to enrich
 * @param <E> a type of the enrichment message
 *
 * @author Alexander Yevsyukov
 */
/* package */ class EventMessageEnricher<M extends Message, E extends Message> extends EnrichmentFunction<M, E> {

    /**
     * A parent instance holding this instance and its siblings.
     */
    private final EventEnricher enricher;

    @Nullable
    private ImmutableMap<Class<?>, EnrichmentFunction<?, ?>> fieldFunctions;
    @Nullable
    private ImmutableBiMap<Descriptors.FieldDescriptor, Descriptors.FieldDescriptor> fieldMap;

    /* package */ EventMessageEnricher(EventEnricher enricher, Class<M> sourceClass, Class<E> targetClass) {
        super(sourceClass, targetClass);
        this.enricher = enricher;
    }

    /**
     * Creates a new instance for enriching events.
     *
     * @param source a class of the event message to enrich
     * @param target a class of the target enrichment
     * @param <M> the type of the event message
     * @param <E> the type of the enrichment message
     * @return new enrichment function with {@link EventMessageEnricher}
     */
    /* package */ static <M extends Message, E extends Message>
    EnrichmentFunction<M, E> unboundInstance(Class<M> source, Class<E> target) {
        final EnrichmentFunction<M, E> result = new Unbound<>(source, target);
        return result;
    }

    @Override
    public Function<M, E> getFunction() {
        return this;
    }

    @Override
    /* package */void validate() {
        final ReferenceValidator referenceValidator = new ReferenceValidator(enricher,
                                                                             getSourceClass(),
                                                                             getTargetClass());
        final List<EnrichmentFunction<?, ?>> fieldFunctions = referenceValidator.validate();

        final ImmutableMap.Builder<Class<?>, EnrichmentFunction<?, ?>> map = ImmutableMap.builder();
        for (EnrichmentFunction<?, ?> fieldFunction : fieldFunctions) {
            map.put(fieldFunction.getSourceClass(), fieldFunction);
        }

        this.fieldFunctions = map.build();
        this.fieldMap = referenceValidator.fieldMap();
    }

    @SuppressWarnings("unchecked") // We control the type safety during initialization and validation.
    @Nullable
    @Override
    public E apply(@Nullable M message) {
        if (message == null) {
            return null;
        }
        checkNotNull(this.fieldMap, "fieldMap");
        checkNotNull(this.fieldFunctions, "fieldFunctions");

        checkState(!fieldMap.isEmpty(), "fieldMap is empty");
        checkState(!fieldFunctions.isEmpty(), "fieldFunctions is empty");

        final E defaultTarget = Internal.getDefaultInstance(getTargetClass());
        final Message.Builder builder = defaultTarget.toBuilder();
        for (Descriptors.FieldDescriptor srcField : fieldMap.keySet()) {
            final Object srcValue = message.getField(srcField);
            final EnrichmentFunction function = fieldFunctions.get(srcValue.getClass());
            final Object targetValue = function.apply(srcValue);
            final Descriptors.FieldDescriptor targetField = fieldMap.get(srcField);
            if (targetValue != null) {
                builder.setField(targetField, targetValue);
            }
        }
        return (E) builder.build();
    }
}
