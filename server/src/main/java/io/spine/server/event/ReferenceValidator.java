/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.logging.Logging;
import io.spine.option.OptionsProto;
import io.spine.server.reflect.Field;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.protobuf.Descriptors.Descriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * Performs validation analyzing which of fields annotated in the enrichment message
 * can be initialized with the translation functions supplied in the parent enricher.
 *
 * <p>As long as the new enrichment functions may be appended to the parent enricher at runtime,
 * the validation result will vary for the same enricher depending on its actual state.
 *
 * @author Alexander Yevsyukov
 */
final class ReferenceValidator implements Logging {

    /** The separator used in Protobuf fully-qualified names. */
    private static final String PROTO_FQN_SEPARATOR = ".";

    private static final String PIPE_SEPARATOR = "|";
    private static final Pattern PATTERN_PIPE_SEPARATOR = Pattern.compile("\\|");

    private static final String SPACE = " ";
    private static final String EMPTY_STRING = "";
    private static final Pattern SPACE_PATTERN = Pattern.compile(SPACE, Pattern.LITERAL);


    /** The reference to the event context used in the `by` field option. */
    @SuppressWarnings("DuplicateStringLiteralInspection") // refers to the proto field name,
        // not a variable name as other strings that use the same value.
    private static final String CONTEXT_REFERENCE = "context";

    private final Enricher enricher;
    private final Descriptor eventDescriptor;
    private final Descriptor enrichmentDescriptor;

    ReferenceValidator(Enricher enricher,
                       Class<? extends Message> eventClass,
                       Class<? extends Message> enrichmentClass) {
        this.enricher = enricher;
        this.eventDescriptor = Internal.getDefaultInstance(eventClass)
                                       .getDescriptorForType();
        this.enrichmentDescriptor = Internal.getDefaultInstance(enrichmentClass)
                                            .getDescriptorForType();
    }

    /**
     * Returns those fields and functions, that may be used for the enrichment at the moment.
     *
     * @return a {@code ValidationResult} data transfer object, containing the valid fields and
     * functions.
     */
    ValidationResult validate() {
        List<EnrichmentFunction<?, ?, ?>> functions = newLinkedList();
        Multimap<FieldDescriptor, FieldDescriptor> fields = LinkedListMultimap.create();
        for (FieldDescriptor enrichmentField : enrichmentDescriptor.getFields()) {
            Collection<FieldDescriptor> sourceFields = findSourceFields(enrichmentField);
            putEnrichmentsByField(functions, fields, enrichmentField, sourceFields);
        }
        ImmutableMultimap<FieldDescriptor, FieldDescriptor> sourceToTargetMap =
                ImmutableMultimap.copyOf(fields);
        ImmutableList<EnrichmentFunction<?, ?, ?>> enrichmentFunctions =
                ImmutableList.copyOf(functions);
        ValidationResult result = new ValidationResult(enrichmentFunctions,
                                                       sourceToTargetMap);
        return result;
    }

    private void putEnrichmentsByField(List<EnrichmentFunction<?, ?, ?>> functions,
                                       Multimap<FieldDescriptor, FieldDescriptor> fields,
                                       FieldDescriptor enrichmentField,
                                       Iterable<FieldDescriptor> sourceFields) {
        for (FieldDescriptor sourceField : sourceFields) {
            Optional<EnrichmentFunction<?, ?, ?>> function =
                    getEnrichmentFunction(sourceField, enrichmentField);
            if (function.isPresent()) {
                functions.add(function.get());
                fields.put(sourceField, enrichmentField);
            }
        }
    }

    /**
     * Searches for the event/context field with the name parsed from the enrichment
     * field {@code by} option.
     */
    private Collection<FieldDescriptor> findSourceFields(FieldDescriptor enrichmentField) {
        String byOptionArgument = enrichmentField.getOptions()
                                                 .getExtension(OptionsProto.by);
        checkNotNull(byOptionArgument);
        String targetFields = removeSpaces(byOptionArgument);
        int pipeSeparatorIndex = targetFields.indexOf(PIPE_SEPARATOR);
        if (pipeSeparatorIndex < 0) {
            FieldDescriptor fieldDescriptor = findSourceFieldByName(targetFields,
                                                                    enrichmentField,
                                                                    true);
            return Collections.singleton(fieldDescriptor);
        } else {
            String[] targetFieldNames = PATTERN_PIPE_SEPARATOR.split(targetFields);
            return findSourceFieldsByNames(targetFieldNames, enrichmentField);
        }
    }

    /**
     * Searches for the event/context field with the name retrieved from the
     * enrichment field {@code by} option.
     *
     * @param name            the name of the searched field
     * @param enrichmentField the field of the enrichment targeted onto the searched field
     * @param strict          if {@code true} the field must be found, an exception is thrown
     *                        otherwise.
     *                        <p>If {@code false} {@code null} will be returned upon an
     *                        unsuccessful search
     * @return {@link FieldDescriptor} for the field with the given name or {@code null} if the
     * field is absent and if not in the strict mode
     */
    private FieldDescriptor findSourceFieldByName(String name,
                                                  FieldDescriptor enrichmentField,
                                                  boolean strict) {
        checkSourceFieldName(name, enrichmentField);
        Descriptor srcMessage = getSrcMessage(name);
        FieldDescriptor field = findField(name, srcMessage);
        if (field == null && strict) {
            throw noFieldException(name, srcMessage, enrichmentField);
        }
        return field;
    }

    private static String removeSpaces(String source) {
        checkNotNull(source);
        String result = SPACE_PATTERN.matcher(source)
                                     .replaceAll(EMPTY_STRING);
        return result;
    }

    private Collection<FieldDescriptor> findSourceFieldsByNames(String[] names,
                                                                FieldDescriptor enrichmentField) {
        checkArgument(names.length > 0, "Names may not be empty");
        checkArgument(names.length > 1,
                      "Enrichment target field names may not be a singleton array. " +
                      "Use findSourceFieldByName().");
        Collection<FieldDescriptor> result = new HashSet<>(names.length);

        FieldDescriptor.Type basicType = null;
        Descriptor messageType = null;
        for (String name : names) {
            FieldDescriptor field = findSourceFieldByName(name, enrichmentField, false);
            if (field == null) {
                /* We don't know at this stage the type of the event.
                   The enrichment is to be included anyway,
                   but by other ReferenceValidator instance */
                continue;
            }

            if (basicType == null) { // Get type of the first field
                basicType = field.getType();
                if (basicType == MESSAGE) {
                    messageType = field.getMessageType();
                }
            } else { // Compare the type with each of the next
                checkState(basicType == field.getType(),
                           differentTypesErrorMessage(enrichmentField));
                if (basicType == MESSAGE) {
                    checkState(messageType.equals(field.getMessageType()),
                               differentTypesErrorMessage(enrichmentField));
                }
            }

            boolean noDuplicateFiled = result.add(field);
            checkState(
                    noDuplicateFiled,
                    "Enrichment target field names may contain no duplicates. " +
                    "Found duplicate field: %s",
                    name
            );
        }
        return result;
    }

    private static String differentTypesErrorMessage(FieldDescriptor enrichmentField) {
        return format("Enrichment field %s targets fields of different types.", enrichmentField);
    }

    private static FieldDescriptor findField(String fieldNameFull, Descriptor srcMessage) {
        if (fieldNameFull.contains(PROTO_FQN_SEPARATOR)) { // is event field FQN or context field
            int firstCharIndex = fieldNameFull.lastIndexOf(PROTO_FQN_SEPARATOR) + 1;
            String fieldName = fieldNameFull.substring(firstCharIndex);
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
        Descriptor msg = fieldName.contains(CONTEXT_REFERENCE)
                         ? EventContext.getDescriptor()
                         : eventDescriptor;
        return msg;
    }

    private Optional<EnrichmentFunction<?, ?, ?>> getEnrichmentFunction(FieldDescriptor srcField,
                                                                        FieldDescriptor targetField) {
        Class<?> sourceFieldClass = Field.getFieldClass(srcField);
        Class<?> targetFieldClass = Field.getFieldClass(targetField);
        Optional<EnrichmentFunction<?, ?, ?>> func =
                enricher.functionFor(sourceFieldClass,targetFieldClass);
        if (!func.isPresent()) {
            logNoFunction(sourceFieldClass, targetFieldClass);
        }
        return func;
    }

    /**
     * Checks if the source field name (from event or context) is not empty.
     */
    private static void checkSourceFieldName(String srcFieldName, FieldDescriptor enrichmentField) {
        if (srcFieldName.isEmpty()) {
            throw newIllegalStateException("There is no `by` option for the enrichment field `%s`",
                                            enrichmentField.getFullName());
        }
    }

    private static IllegalStateException noFieldException(
            String eventFieldName,
            Descriptor srcMessage,
            FieldDescriptor enrichmentField) {
        throw newIllegalStateException(
                "No field `%s` in the message `%s` found. " +
                "The field is referenced in the option of the enrichment field `%s`.",
                eventFieldName,
                srcMessage.getFullName(),
                enrichmentField.getFullName());
    }

    private static void logNoFunction(Class<?> sourceFieldClass, Class<?> targetFieldClass) {
        Logger log = Logging.get(ReferenceValidator.class);
        if (log.isDebugEnabled()) {
            log.debug("There is no enrichment function for translating {} into {}",
                      sourceFieldClass, targetFieldClass);
        }
    }

    /**
     * A wrapper DTO for the validation result.
     */
    static class ValidationResult {
        private final ImmutableList<EnrichmentFunction<?, ?, ?>> functions;
        private final ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap;

        private ValidationResult(ImmutableList<EnrichmentFunction<?, ?, ?>> functions,
                                 ImmutableMultimap<FieldDescriptor, FieldDescriptor> fieldMap) {
            this.functions = functions;
            this.fieldMap = fieldMap;
        }

        /**
         * Returns the validated list of {@code EnrichmentFunction}s that may be used for
         * the conversion in scope of the validated {@code Enricher}.
         */
        @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK, since an `ImmutableList`
                                                            // is returned.
        List<EnrichmentFunction<?, ?, ?>> getFunctions() {
            return functions;
        }

        /**
         * Returns a map from source event/context field to target enrichment field descriptors,
         * which is valid in scope of the target {@code Enricher}.
         */
        ImmutableMultimap<FieldDescriptor, FieldDescriptor> getFieldMap() {
            return fieldMap;
        }
    }
}
