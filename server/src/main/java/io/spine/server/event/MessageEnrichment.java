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

package io.spine.server.event;

import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.server.reflect.Field;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * The default mechanism for enriching messages based on {@code FieldOptions}
 * of Protobuf message definitions.
 *
 * @param <S> a type of the source message to enrich
 * @param <T> a type of the target enrichment message
 *
 * @author Alexander Yevsyukov
 */
class MessageEnrichment<S extends Message, T extends Message, C extends Message>
        extends EnrichmentFunction<S, T, C> {

    /** A parent instance holding this instance and its siblings. */
    private final Enricher enricher;

    /** Tells, whether this instance is active or not. */
    private boolean active = false;

    /** A map from source message field class to enrichment functions. */
    private @Nullable ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> fieldFunctions;

    /** A map from source message/context field to target enrichment field descriptors. */
    private @Nullable ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap;

    /** Creates a new message enricher instance. */
    static <S extends Message, T extends Message, C extends Message>
    MessageEnrichment<S, T, C> create(Enricher enricher,
                                      Class<S> messageClass,
                                      Class<T> enrichmentClass) {
        return new MessageEnrichment<>(enricher, messageClass, enrichmentClass);
    }

    private MessageEnrichment(Enricher enricher,
                              Class<S> eventClass,
                              Class<T> enrichmentClass) {
        super(eventClass, enrichmentClass);
        this.enricher = enricher;
    }

    @Override
    void activate() {
        ReferenceValidator referenceValidator =
                new ReferenceValidator(enricher, getSourceClass(), getEnrichmentClass());
        ImmutableMultimap.Builder<Class<?>, EnrichmentFunction<?, ?, ?>> map =
                                                                      ImmutableMultimap.builder();
        ReferenceValidator.ValidationResult validationResult = referenceValidator.validate();
        List<EnrichmentFunction<?, ?, ?>> fieldFunctions = validationResult.getFunctions();
        for (EnrichmentFunction<?, ?, ?> fieldFunction : fieldFunctions) {
            map.put(fieldFunction.getSourceClass(), fieldFunction);
        }
        this.fieldFunctions = map.build();
        this.fieldMap = validationResult.getFieldMap();

        markActive();
    }

    @Override
    boolean isActive() {
        return active;
    }

    @Override
    public T apply(S eventMsg, C context) {
        checkNotNull(eventMsg);
        checkNotNull(context);
        ensureActive();
        verifyOwnState();

        T defaultTarget = Internal.getDefaultInstance(getEnrichmentClass());
        Message.Builder builder = defaultTarget.toBuilder();
        setFields(builder, eventMsg, context);
        @SuppressWarnings("unchecked") // types are checked during the initialization and validation
        T result = (T) builder.build();
        return result;
    }

    private void markActive() {
        try {
            verifyOwnState();
            active = true;
        } catch (RuntimeException ignored) {
            active = false;
        }
    }

    private void verifyOwnState() {
        checkNotNull(fieldMap, "fieldMap");
        checkNotNull(fieldFunctions, "fieldFunctions");
        checkState(!fieldMap.isEmpty(), "fieldMap is empty");
        checkState(!fieldFunctions.isEmpty(), "fieldFunctions is empty");
    }

    @SuppressWarnings({
            "ConstantConditions" /* it is assured that collections are not null, and
                                    after validation maps have required entries. */,
            "MethodWithMultipleLoops"}
    )
    private void setFields(Message.Builder builder, S sourceMessage, C context) {
        for (FieldDescriptor srcField : fieldMap.keySet()) {
            Object srcFieldValue = getSrcFieldValue(srcField, sourceMessage, context);
            Class<?> sourceFieldClass = srcFieldValue.getClass();
            Collection<EnrichmentFunction<?, ?, ?>> functions =
                    fieldFunctions.get(sourceFieldClass);
            Collection<FieldDescriptor> targetFields = fieldMap.get(srcField);
            for (FieldDescriptor targetField : targetFields) {
                Optional<EnrichmentFunction<?, ?, ?>> function =
                        firstThat(functions,
                                  SupportsFieldConversion.of(sourceFieldClass,
                                                             Field.getFieldClass(targetField)));
                EnrichmentFunction fieldEnrichment = function.get();

                @SuppressWarnings("unchecked") /* The model is checked during the initialization
                                                  and activation. */
                        Object targetValue = fieldEnrichment.apply(srcFieldValue, context);
                if (targetValue != null) {
                    builder.setField(targetField, targetValue);
                }
            }
        }
    }

    private Object getSrcFieldValue(FieldDescriptor srcField, S eventMsg, C context) {
        boolean isContextField = srcField.getContainingType()
                                         .equals(EventContext.getDescriptor());
        Object result = isContextField
                        ? context.getField(srcField)
                        : eventMsg.getField(srcField);
        return result;
    }
}
