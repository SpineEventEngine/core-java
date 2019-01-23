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
import io.spine.core.MessageContext;
import io.spine.server.reflect.Field;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.Descriptors.FieldDescriptor;
import static io.spine.protobuf.Messages.defaultInstance;
import static io.spine.server.event.enrich.SupportsFieldConversion.supportsConversion;

/**
 * The default mechanism for enriching messages based on {@code FieldOptions}
 * of Protobuf message definitions.
 *
 * @param <S> a type of the source message to enrich
 * @param <T> a type of the target enrichment message
 * @param <C> the type of the source message context
 */
class MessageEnrichment<S extends Message, C extends MessageContext, T extends Message>
        extends EnrichmentFunction<S, C, T> {

    /** A parent instance holding this instance and its siblings. */
    private final Enricher enricher;

    /** Tells, whether this instance is active or not. */
    private boolean active = false;

    /** A map from source message field class to enrichment functions. */
    private ImmutableMultimap<Class<?>, EnrichmentFunction<?, ?, ?>> fieldFunctions;

    /** A map from source message/context field to target enrichment field descriptors. */
    private ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap;

    MessageEnrichment(Enricher enricher, Class<S> sourceClass, Class<T> enrichmentClass) {
        super(sourceClass, enrichmentClass);
        this.enricher = enricher;
    }

    @Override
    void activate() {
        Linker linker = new Linker(enricher, sourceClass(), targetClass());
        FieldTransitions fieldTransitions = linker.link();
        this.fieldFunctions = fieldTransitions.functionMap();
        this.fieldMap = fieldTransitions.fieldMap();
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
     * Method object for {@link MessageEnrichment#apply(Message, MessageContext)}.
     *
     * @param <S> the type of the source message
     * @param <T> the type of the target message
     * @param <C> the type of the source message context
     */
    private static final
    class Helper<S extends Message, C extends MessageContext, T extends Message> {

        /** The parent enrichment instance. */
        private final MessageEnrichment<S, C, T> enrichment;

        /** The source message that we enrich. */
        private final S message;

        /** The context of the source message. */
        private final C context;

        /** The builder of the target message type. */
        private final Message.Builder builder;

        private Helper(MessageEnrichment<S, C, T> enrichment, S message, C context) {
            this.enrichment = enrichment;
            this.message = message;
            this.context = context;
            this.builder = defaultInstance(enrichment.targetClass()).toBuilder();
        }

        private T createEnrichment() {
            setFields();
            @SuppressWarnings("unchecked")
            /* Types are checked during the initialization and validation. */
            T result = (T) builder.build();
            return result;
        }

        private void setFields() {
            for (FieldDescriptor srcField : enrichment.fieldMap.keySet()) {
                enrichField(srcField);
            }
        }

        /**
         * Creates one or more enrichments for the source field.
         */
        private void enrichField(FieldDescriptor srcField) {
            Object sourceValue = getValue(srcField, message, context);
            Class<?> sourceFieldClass = sourceValue.getClass();
            Collection<FieldDescriptor> targetFields = enrichment.fieldMap.get(srcField);
            for (FieldDescriptor targetField : targetFields) {
                FieldEnrichment fieldEnrichment = find(sourceFieldClass, targetField);
                @SuppressWarnings("unchecked")
                /* The model is checked during the initialization and activation. */
                Object targetValue = fieldEnrichment.apply(sourceValue, context);
                if (targetValue != null) {
                    builder.setField(targetField, targetValue);
                }
            }
        }

        /**
         * Obtains the value of the source field either from the message, or from the context,
         * if the field descriptor represents a context field.
         */
        private Object getValue(FieldDescriptor srcField, S msg, C context) {
            boolean isContextField = srcField.getContainingType()
                                             .equals(context.getDescriptorForType());
            Object result = isContextField
                            ? context.getField(srcField)
                            : msg.getField(srcField);
            return result;
        }

        private
        FieldEnrichment<?, ?, ?> find(Class<?> sourceField, FieldDescriptor targetField) {
            SupportsFieldConversion conversion =
                    supportsConversion(sourceField, Field.getFieldClass(targetField));
            Iterable<EnrichmentFunction<?, ?, ?>> functions =
                    enrichment.fieldFunctions.get(sourceField);
            EnrichmentFunction<?, ?, ?> result = firstThat(functions, conversion)
                    .orElseThrow(conversion::unsupported);
            return (FieldEnrichment<?, ?, ?>) result;
        }
    }
}
