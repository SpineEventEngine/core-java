/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.annotations.EventAnnotationsProto;
import org.spine3.base.EventContext;
import org.spine3.protobuf.Messages;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.protobuf.Descriptors.Descriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor;
import static java.lang.String.format;

/**
 * Performs validation checking that all fields annotated in the enrichment message
 * can be initialized with the translation functions supplied in the parent enricher.
 *
 * @author Alexander Yevsyukov
 */
class ReferenceValidator {

    /** The separator used in Protobuf fully-qualified names. */
    private static final String PROTO_FQN_SEPARATOR = ".";

    /** The reference to the event context used in the `by` field option. */
    private static final String CONTEXT_REFERENCE = "context";

    private final EventEnricher enricher;
    private final Descriptor eventDescriptor;
    private final Descriptor enrichmentDescriptor;

    @Nullable
    private ImmutableMultimap<FieldDescriptor, FieldDescriptor> sourceToTargetMap;

    ReferenceValidator(EventEnricher enricher,
            Class<? extends Message> eventClass,
            Class<? extends Message> enrichmentClass) {
        this.enricher = enricher;
        this.eventDescriptor = Internal.getDefaultInstance(eventClass)
                                       .getDescriptorForType();
        this.enrichmentDescriptor = Internal.getDefaultInstance(enrichmentClass)
                                            .getDescriptorForType();
    }

    @Nullable
    ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap() {
        return sourceToTargetMap;
    }

    //TODO:24-Jan-2017:alex.tymchenko: fix the description.
    /** Throws IllegalStateException if the parent {@code Enricher} does not have a function for a field enrichment */
    List<EnrichmentFunction<?, ?>> validate() {
        final ImmutableList.Builder<EnrichmentFunction<?, ?>> functions = ImmutableList.builder();
        final ImmutableMultimap.Builder<FieldDescriptor, FieldDescriptor> fields = ImmutableMultimap.builder();
        for (FieldDescriptor enrichmentField : enrichmentDescriptor.getFields()) {
            final FieldDescriptor sourceField = findSourceField(enrichmentField);
            final Optional<EnrichmentFunction<?, ?>> function = getEnrichmentFunction(sourceField, enrichmentField);
            if(function.isPresent()) {
                functions.add(function.get());
                fields.put(sourceField, enrichmentField);
            }
        }
        this.sourceToTargetMap = fields.build();
        return functions.build();
    }

    /** Searches for the event/context field with the name parsed from the enrichment field `by` option. */
    private FieldDescriptor findSourceField(FieldDescriptor enrichmentField) {
        final String fieldName = enrichmentField.getOptions().getExtension(EventAnnotationsProto.by);
        checkSourceFieldName(fieldName, enrichmentField);
        final Descriptor srcMessage = getSrcMessage(fieldName);
        final FieldDescriptor field = findField(fieldName, srcMessage);
        if (field == null) {
            throw noFieldException(fieldName, srcMessage, enrichmentField);
        }
        return field;
    }

    private static FieldDescriptor findField(String fieldNameFull, Descriptor srcMessage) {
        if (fieldNameFull.contains(PROTO_FQN_SEPARATOR)) { // is event field FQN or context field
            final int firstCharIndex = fieldNameFull.lastIndexOf(PROTO_FQN_SEPARATOR) + 1;
            final String fieldName = fieldNameFull.substring(firstCharIndex);
            return srcMessage.findFieldByName(fieldName);
        } else {
            return srcMessage.findFieldByName(fieldNameFull);
        }
    }

    /**
     * Returns an event descriptor or context descriptor
     * if the field name contains {@link ReferenceValidator#CONTEXT_REFERENCE}.
     */
    private Descriptor getSrcMessage(String fieldName) {
        final Descriptor msg = fieldName.contains(CONTEXT_REFERENCE)
                               ? EventContext.getDescriptor()
                               : eventDescriptor;
        return msg;
    }

    private Optional<EnrichmentFunction<?, ?>> getEnrichmentFunction(FieldDescriptor srcField, FieldDescriptor targetField) {
        final Class<?> sourceFieldClass = Messages.getFieldClass(srcField);
        final Class<?> targetFieldClass = Messages.getFieldClass(targetField);
        final Optional<EnrichmentFunction<?, ?>> func = enricher.functionFor(sourceFieldClass, targetFieldClass);
        if (!func.isPresent()) {
            warnNoFunction(sourceFieldClass, targetFieldClass);
        }
        return func;
    }

    /** Checks if the source field name (from event or context) is not empty. */
    private static void checkSourceFieldName(String srcFieldName, FieldDescriptor enrichmentField) {
        if (srcFieldName.isEmpty()) {
            final String msg = format("There is no `by` option for the enrichment field `%s`",
                                      enrichmentField.getFullName());
            throw new IllegalStateException(msg);
        }
    }

    private static IllegalStateException noFieldException(
            String eventFieldName,
            Descriptor srcMessage,
            FieldDescriptor enrichmentField) {
        final String msg = format(
                "No field `%s` in the message `%s` found. " +
                "The field is referenced in the option of the enrichment field `%s`.",
                eventFieldName,
                srcMessage.getFullName(),
                enrichmentField.getFullName());
        throw new IllegalStateException(msg);
    }

    private static void warnNoFunction(Class<?> sourceFieldClass, Class<?> targetFieldClass) {
        // Using `DEBUG` level to avoid polluting the `stderr`.
        if (log().isDebugEnabled()) {
            log().debug("There is no enrichment function for translating {} to {}", sourceFieldClass, targetFieldClass);
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ReferenceValidator.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
