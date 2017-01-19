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
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import org.spine3.annotations.EventAnnotationsProto;
import org.spine3.base.EventContext;
import org.spine3.protobuf.Messages;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.Descriptors.Descriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE;
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

    private static final String PIPE_SEPARATOR = "|";
    private static final Pattern PATTERN_PIPE_SEPARATOR = Pattern.compile("\\|");

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

    /** Throws IllegalStateException if the parent {@code Enricher} does not have a function for a field enrichment */
    List<EnrichmentFunction<?, ?>> validate() {
        final List<EnrichmentFunction<?, ?>> functions = new LinkedList<>();
        final Multimap<FieldDescriptor, FieldDescriptor> fields = LinkedListMultimap.create();
        for (FieldDescriptor enrichmentField : enrichmentDescriptor.getFields()) {
            final Collection<FieldDescriptor> sourceFields = findSourceFields(enrichmentField);
            putEnrichmentsByField(functions, fields, enrichmentField, sourceFields);
        }
        this.sourceToTargetMap = ImmutableMultimap.copyOf(fields);
        final ImmutableList<EnrichmentFunction<?, ?>> result = ImmutableList.copyOf(functions);
        return result;
    }

    private void putEnrichmentsByField(List<EnrichmentFunction<?, ?>> functions,
                                       Multimap<FieldDescriptor, FieldDescriptor> fields,
                                       FieldDescriptor enrichmentField,
                                       Iterable<FieldDescriptor> sourceFields) {
        for (FieldDescriptor sourceField : sourceFields) {
            final EnrichmentFunction<?, ?> function = getEnrichmentFunction(sourceField, enrichmentField);
            functions.add(function);
            fields.put(sourceField, enrichmentField);
        }
    }

    /** Searches for the event/context field with the name parsed from the enrichment field `by` option. */
    private Collection<FieldDescriptor> findSourceFields(FieldDescriptor enrichmentField) {
        final String byOptionArgument = enrichmentField.getOptions()
                                                       .getExtension(EventAnnotationsProto.by);
        checkNotNull(byOptionArgument);
        final int pipeSeparatorIndex = byOptionArgument.indexOf(PIPE_SEPARATOR);
        if (pipeSeparatorIndex < 0) {
            return Collections.singleton(findSourceFieldByName(byOptionArgument, enrichmentField, true));
        } else {
            final String[] targetFieldNames = PATTERN_PIPE_SEPARATOR.split(byOptionArgument);
            return findSourceFieldsByNames(targetFieldNames, enrichmentField);
        }
    }

    // TODO:19-01-17:dmytro.dashenkov: Javadoc.
    private FieldDescriptor findSourceFieldByName(String name, FieldDescriptor enrichmentField, boolean strict) {
        checkSourceFieldName(name, enrichmentField);
        final Descriptor srcMessage = getSrcMessage(name);
        final FieldDescriptor field = findField(name, srcMessage);
        if (field == null && strict) {
            throw noFieldException(name, srcMessage, enrichmentField);
        }
        return field;
    }

    private Collection<FieldDescriptor> findSourceFieldsByNames(String[] names, FieldDescriptor enrichmentField) {
        checkArgument(names.length > 0, "Names may not be empty");
        checkArgument(names.length > 1,
                      "Enrichment target field names may not be a singleton array. Use findSourceFieldByName.");
        final Collection<FieldDescriptor> result = new HashSet<>(names.length);

        FieldDescriptor.Type basicType = null;
        Descriptor messageType = null;
        for (String name : names) {
            final FieldDescriptor field = findSourceFieldByName(name, enrichmentField, false);
            if (field == null) {
                // We don't know at this stage the type of the event
                // The enrichment is to be included anyway, but by other {@code ReferenceValidator} instance
                continue;
            }

            if (basicType == null) { // Get type of the first field
                basicType = field.getType();
                if (basicType == MESSAGE) {
                    messageType = field.getMessageType();
                }
            } else { // Compare the type with each of the next
                checkState(basicType == field.getType(), differentTypesErrorMessage(enrichmentField));
                if (basicType == MESSAGE) {
                    checkState(messageType.equals(field.getMessageType()), differentTypesErrorMessage(enrichmentField));
                }
            }

            final boolean noDuplicateFiled = result.add(field);
            checkState(
                    noDuplicateFiled,
                    "Enrichment target field names may contain no duplicates. Found duplicate field " + name
            );
        }
        return result;
    }

    private static String differentTypesErrorMessage(FieldDescriptor enrichmentField) {
        return String.format("Enrichment field %s targets fields of different types", enrichmentField);
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

    private EnrichmentFunction<?, ?> getEnrichmentFunction(FieldDescriptor srcField, FieldDescriptor targetField) {
        final Class<?> sourceFieldClass = Messages.getFieldClass(srcField);
        final Class<?> targetFieldClass = Messages.getFieldClass(targetField);
        final Optional<EnrichmentFunction<?, ?>> func = enricher.functionFor(sourceFieldClass, targetFieldClass);
        if (!func.isPresent()) {
            throw noFunction(sourceFieldClass, targetFieldClass);
        }
        return func.get();
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

    private static IllegalStateException noFunction(Class<?> sourceFieldClass, Class<?> targetFieldClass) {
        final String msg = format(
                "There is no enrichment function for translating %s to %s",
                sourceFieldClass,
                targetFieldClass);
        throw new IllegalStateException(msg);
    }
}
