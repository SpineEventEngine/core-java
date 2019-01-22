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

import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.protobuf.Messages;
import io.spine.server.reflect.Field;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.Descriptors.FieldDescriptor;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The default mechanism for enriching messages based on {@code FieldOptions}
 * of Protobuf message definitions.
 *
 * @param <S> a type of the source message to enrich
 * @param <T> a type of the target enrichment message
 * @param <C> the type of the source message context
 */
final class MessageEnrichment<S extends Message, C extends Message, T extends Message>
        extends EnrichmentFunction<S, C, T> {

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
    MessageEnrichment<S, C, T> create(Enricher enricher,
                                      Class<S> messageClass,
                                      Class<T> enrichmentClass) {
        return new MessageEnrichment<>(enricher, messageClass, enrichmentClass);
    }

    private MessageEnrichment(Enricher enricher, Class<S> sourceClass, Class<T> enrichmentClass) {
        super(sourceClass, enrichmentClass);
        this.enricher = enricher;
    }

    static <T extends Message> T defaultInstance(Class<? extends T> cls) {
        return Messages.defaultInstance(cls);
    }

    @Override
    void activate() {
        Class<? extends Message> sourceClass = sourceClass();
        ReferenceValidator referenceValidator =
                new ReferenceValidator(enricher, sourceClass, targetClass());
        ValidationResult validationResult = referenceValidator.validate();
        this.fieldFunctions = validationResult.functionMap();
        this.fieldMap = validationResult.fieldMap();

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
        verifyState();

        Helper<S, C, T> helper = new Helper<>(this, eventMsg, context);
        T result = helper.createEnrichment();
        return result;
    }

    private void markActive() {
        try {
            verifyState();
            active = true;
        } catch (RuntimeException ignored) {
            active = false;
        }
    }

    private void verifyState() {
        checkNotNull(fieldMap, "fieldMap");
        checkNotNull(fieldFunctions, "fieldFunctions");
        checkState(!fieldMap.isEmpty(), "fieldMap is empty");
        checkState(!fieldFunctions.isEmpty(), "fieldFunctions is empty");
    }

    /**
     * Method object for {@link MessageEnrichment#apply(Message, Message)}.
     *
     * @param <S> the type of the source message
     * @param <T> the type of the target message
     * @param <C> the type of the source message context
     */
    private static final class Helper<S extends Message, C extends Message, T extends Message> {

        private final MessageEnrichment<S, C, T> enrichment;
        private final S message;
        private final C context;

        private final Message.Builder builder;

        private Helper(MessageEnrichment<S, C, T> enrichment, S message, C context) {
            this.enrichment = enrichment;
            this.message = message;
            this.context = context;
            this.builder = defaultInstance(enrichment.targetClass()).toBuilder();
        }

        private T createEnrichment() {
            setFields();
            @SuppressWarnings("unchecked") /* Types are checked during the initialization and
            validation. */
            T result = (T) builder.build();
            return result;
        }

        @SuppressWarnings({
                "ConstantConditions" /* it is assured that collections are not null, and
                                    after validation maps have required entries. */,
                "MethodWithMultipleLoops"}
        )
        private void setFields() {
            for (FieldDescriptor srcField : enrichment.fieldMap.keySet()) {
                Object srcFieldValue = getSrcFieldValue(srcField, message, context);
                Class<?> sourceFieldClass = srcFieldValue.getClass();
                Collection<EnrichmentFunction<?, ?, ?>> functions =
                        enrichment.fieldFunctions.get(sourceFieldClass);
                Collection<FieldDescriptor> targetFields = enrichment.fieldMap.get(srcField);
                for (FieldDescriptor targetField : targetFields) {
                    SupportsFieldConversion conversion =
                            SupportsFieldConversion.of(sourceFieldClass,
                                                       Field.getFieldClass(targetField)
                            );
                    Optional<EnrichmentFunction<?, ?, ?>> function = firstThat(functions, conversion);
                    EnrichmentFunction fieldEnrichment = function
                            .orElseThrow(() -> newIllegalStateException(
                                    "Unable to get enrichment for the conversion from message field " +
                                            "of type `%s` to enrichment field of type `%s`.",
                                    conversion.messageFieldClass(),
                                    conversion.enrichmentFieldClass()
                            ));

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
}
