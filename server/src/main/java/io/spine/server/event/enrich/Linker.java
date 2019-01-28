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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.Message;
import io.spine.code.proto.FieldReference;
import io.spine.core.EventContext;
import io.spine.logging.Logging;
import io.spine.server.reflect.Field;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.protobuf.Descriptors.Descriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE;
import static io.spine.protobuf.Messages.defaultInstance;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * Analyzes which of fields annotated in the enrichment message can be initialized with the
 * translation functions supplied in the parent Enricher.
 */
final class Linker implements Logging {

    /** The separator used in Protobuf fully-qualified names. */
    private static final String PROTO_FQN_SEPARATOR = ".";

    private static final String SPACE = " ";
    private static final String EMPTY_STRING = "";
    private static final Pattern SPACE_PATTERN = Pattern.compile(SPACE, Pattern.LITERAL);

    private final Enricher enricher;
    private final Descriptor eventDescriptor;
    private final Descriptor enrichmentDescriptor;

    private final ImmutableMultimap.Builder<FieldDescriptor, FieldDescriptor> fields =
            ImmutableMultimap.builder();
    private final ImmutableList.Builder<EnrichmentFunction<?, ?, ?>> functions =
            ImmutableList.builder();

    Linker(Enricher enricher,
           Class<? extends Message> sourceClass,
           Class<? extends Message> enrichmentClass) {
        this.enricher = enricher;
        this.eventDescriptor = descriptorOf(sourceClass);
        this.enrichmentDescriptor = descriptorOf(enrichmentClass);
    }

    private static Descriptor descriptorOf(Class<? extends Message> cls) {
        return defaultInstance(cls).getDescriptorForType();
    }

    /**
     * Provides information on how to create an instance of the target enrichment
     * from the source message.
     */
    FieldTransitions createTransitions() {
        for (FieldDescriptor enrichmentField : enrichmentDescriptor.getFields()) {
            Collection<FieldDescriptor> sourceFields = toSourceFields(enrichmentField);
            putEnrichmentsByField(enrichmentField, sourceFields);
        }
        FieldTransitions result = new FieldTransitions(functions.build(), fields.build());
        return result;
    }

    private void putEnrichmentsByField(FieldDescriptor enrichmentField,
                                       Iterable<FieldDescriptor> sourceFields) {
        for (FieldDescriptor sourceField : sourceFields) {
            Optional<FieldEnrichment<?, ?, ?>> found = transition(sourceField, enrichmentField);
            found.ifPresent(fn -> {
                functions.add(fn);
                fields.put(sourceField, enrichmentField);
            });
        }
    }

    private Collection<FieldDescriptor> toSourceFields(FieldDescriptor enrichmentField) {
        ImmutableList<FieldReference> fieldReferences =
                FieldReference.allFrom(enrichmentField.toProto());
        checkState(!fieldReferences.isEmpty(),
                   "Unable to get source field information from the enrichment field `%s`",
                   enrichmentField.getFullName());

        ImmutableList<String> fieldNames =
                fieldReferences.stream()
                               .map(FieldReference::fieldName)
                               .collect(toImmutableList());
        return findSourceFieldsByNames(fieldNames, enrichmentField);
    }

    /**
     * Searches for the event/context field with the name retrieved from the
     * enrichment field {@code by} option.
     *
     * @param fieldReference
     *         the reference to a field as discovered in the {@code (by)} option
     * @param enrichmentField the field of the enrichment targeted onto the searched field
     * @param strict          if {@code true} the field must be found, an exception is thrown
     *                        otherwise.
     *                        <p>If {@code false} {@code null} will be returned upon an
     *                        unsuccessful search
     * @return {@link FieldDescriptor} for the field with the given name or {@code null} if the
     * field is absent and if not in the strict mode
     */
    private @Nullable FieldDescriptor findSourceFieldByName(String fieldReference,
                                                            FieldDescriptor enrichmentField,
                                                            boolean strict) {
        checkSourceFieldName(fieldReference, enrichmentField);
        Descriptor srcMessage = sourceDescriptor(fieldReference);
        FieldDescriptor field = findField(fieldReference, srcMessage);
        if (field == null && strict) {
            throw noFieldException(fieldReference, srcMessage, enrichmentField);
        }
        return field;
    }

    private Collection<FieldDescriptor> findSourceFieldsByNames(ImmutableList<String> names,
                                                                FieldDescriptor enrichmentField) {
        int nameCount = names.size();
        checkArgument(nameCount > 0, "Names may not be empty");
        Collection<FieldDescriptor> result = new HashSet<>(nameCount);

        FieldDescriptor.Type basicType = null;
        Descriptor messageType = null;
        for (String name : names) {
            FieldDescriptor field = findSourceFieldByName(name, enrichmentField, false);
            if (field == null) {
                /* We don't know at this stage the type of the event.
                   The enrichment is to be included anyway,
                   but by another Linker instance */
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

    private static
    @Nullable FieldDescriptor findField(String fieldReference, Descriptor srcMessage) {
        if (fieldReference.contains(PROTO_FQN_SEPARATOR)) { // is event field FQN or context field
            int firstCharIndex = fieldReference.lastIndexOf(PROTO_FQN_SEPARATOR) + 1;
            String fieldName = fieldReference.substring(firstCharIndex);
            return srcMessage.findFieldByName(fieldName);
        } else {
            return srcMessage.findFieldByName(fieldReference);
        }
    }

    /**
     * Returns an event descriptor or context descriptor
     * if the field name contains {@code "context"} in the name.
     */
    private Descriptor sourceDescriptor(String fieldName) {
        Descriptor msg = FieldReference.Via.context.matches(fieldName)
                         ? EventContext.getDescriptor()
                         : eventDescriptor;
        return msg;
    }

    private Optional<FieldEnrichment<?, ?, ?>>
    transition(FieldDescriptor source, FieldDescriptor target) {
        Class<?> sourceField = Field.getFieldClass(source);
        Class<?> targetField = Field.getFieldClass(target);
        Optional<FieldEnrichment<?, ?, ?>> func =
                enricher.schema()
                        .transition(sourceField, targetField);
        if (!func.isPresent()) {
            logNoFunction(sourceField, targetField);
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

    private static IllegalStateException noFieldException(String eventFieldName,
                                                          Descriptor srcMessage,
                                                          FieldDescriptor enrichmentField) {
        throw newIllegalStateException(
                "No field `%s` in the message `%s` found. " +
                "The field is referenced in the option of the enrichment field `%s`.",
                eventFieldName,
                srcMessage.getFullName(),
                enrichmentField.getFullName());
    }

    private void logNoFunction(Class<?> sourceFieldClass, Class<?> targetFieldClass) {
        _debug("There is no enrichment function for translating {} into {}",
               sourceFieldClass, targetFieldClass);
    }
}
