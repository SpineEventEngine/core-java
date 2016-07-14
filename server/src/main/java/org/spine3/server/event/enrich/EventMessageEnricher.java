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
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.protobuf.Messages;
import org.spine3.server.event.enrich.EventEnricher.SupportsFieldConversion;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * The default mechanism for enriching messages based on {@code FieldOptions} of Protobuf message definitions.
 *
 * @param <S> a type of the source event message to enrich
 * @param <T> a type of the target enrichment message
 *
 * @author Alexander Yevsyukov
 */
/* package */ class EventMessageEnricher<S extends Message, T extends Message> extends EnrichmentFunction<S, T> {

    /** A parent instance holding this instance and its siblings. */
    private final EventEnricher enricher;

    /** A map from source event field class to enrichment functions. */
    @Nullable
    private ImmutableMultimap<Class<?>, EnrichmentFunction> fieldFunctions;

    /** A map from source event/context field to target enrichment field descriptors. */
    @Nullable
    private ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap;

    /** Creates a new message enricher instance. */
    /* package */ static <S extends Message, T extends Message> EventMessageEnricher<S, T> newInstance(
            EventEnricher enricher,
            Class<S> eventClass,
            Class<T> enrichmentClass) {
        return new EventMessageEnricher<>(enricher, eventClass, enrichmentClass);
    }

    private EventMessageEnricher(EventEnricher enricher, Class<S> eventClass, Class<T> enrichmentClass) {
        super(eventClass, enrichmentClass);
        this.enricher = enricher;
    }

    @Override
    public Function<S, T> getFunction() {
        return this;
    }

    @Override
    /* package */ void validate() {
        final ReferenceValidator referenceValidator = new ReferenceValidator(enricher,
                                                                             getEventClass(),
                                                                             getEnrichmentClass());
        final ImmutableMultimap.Builder<Class<?>, EnrichmentFunction> map = ImmutableMultimap.builder();
        final List<EnrichmentFunction<?, ?>> fieldFunctions = referenceValidator.validate();
        for (EnrichmentFunction<?, ?> fieldFunction : fieldFunctions) {
            map.put(fieldFunction.getEventClass(), fieldFunction);
        }
        this.fieldFunctions = map.build();
        this.fieldMap = referenceValidator.fieldMap();
    }

    @Override
    public T apply(@Nullable S eventMsg) {
        checkNotNull(eventMsg);
        checkNotNull(fieldMap, "fieldMap");
        checkNotNull(fieldFunctions, "fieldFunctions");
        checkState(!fieldMap.isEmpty(), "fieldMap is empty");
        checkState(!fieldFunctions.isEmpty(), "fieldFunctions is empty");
        final T defaultTarget = Internal.getDefaultInstance(getEnrichmentClass());
        final Message.Builder builder = defaultTarget.toBuilder();
        setFields(builder, eventMsg);
        @SuppressWarnings("unchecked") // types are checked during the initialization and validation
        final T result = (T) builder.build();
        return result;
    }

    @SuppressWarnings({"ConstantConditions", "MethodWithMultipleLoops"}) // it is assured that collections are not null
    private void setFields(Message.Builder builder, S eventMsg) {
        for (FieldDescriptor srcField : fieldMap.keySet()) {
            final Object srcFieldValue = getSrcFieldValue(srcField, eventMsg);
            final Class<?> sourceFieldClass = srcFieldValue.getClass();
            final Collection<EnrichmentFunction> functions = fieldFunctions.get(sourceFieldClass);
            final Collection<FieldDescriptor> targetFields = fieldMap.get(srcField);
            for (FieldDescriptor targetField : targetFields) {
                final Optional<EnrichmentFunction> function = FluentIterable.from(functions)
                        .firstMatch(SupportsFieldConversion.of(sourceFieldClass, Messages.getFieldClass(targetField)));
                @SuppressWarnings("unchecked") // types are checked during the initialization and validation
                final Object targetValue = function.get().apply(srcFieldValue);
                if (targetValue != null) {
                    builder.setField(targetField, targetValue);
                }
            }
        }
    }

    private Object getSrcFieldValue(FieldDescriptor srcField, S eventMsg) {
        final boolean isContextField = srcField.getContainingType().equals(EventContext.getDescriptor());
        final Object result = isContextField ?
                getContext().getField(srcField) :
                eventMsg.getField(srcField);
        return result;
    }
}
