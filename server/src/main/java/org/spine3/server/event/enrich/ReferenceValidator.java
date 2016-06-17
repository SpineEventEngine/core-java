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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import org.spine3.annotations.EventAnnotationsProto;
import org.spine3.protobuf.Messages;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Performs validation checking that all fields annotated in the enrichment message
 * can be created with the translation functions supplied in the parent enricher.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class ReferenceValidator {

    private final EventEnricher enricher;
    private final Descriptors.Descriptor sourceDescriptor;
    private final Descriptors.Descriptor targetDescriptor;
    private final Descriptors.FieldDescriptor byOptionDescriptor;

    @Nullable
    private ImmutableBiMap<Descriptors.FieldDescriptor, Descriptors.FieldDescriptor> sourceToTargetMap;

    ReferenceValidator(EventEnricher enricher,
            Class<? extends Message> sourceClass,
            Class<? extends Message> targetClass) {
        this.enricher = enricher;
        final GeneratedMessage.GeneratedExtension<DescriptorProtos.FieldOptions, String> byOption =
                EventAnnotationsProto.by;
        this.byOptionDescriptor = byOption.getDescriptor();

        this.sourceDescriptor = Internal.getDefaultInstance(sourceClass)
                                        .getDescriptorForType();
        this.targetDescriptor = Internal.getDefaultInstance(targetClass)
                                        .getDescriptorForType();
    }

    @Nullable
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK since the implementation is immutable.
    /* package */ ImmutableBiMap<Descriptors.FieldDescriptor, Descriptors.FieldDescriptor> fieldMap() {
        return sourceToTargetMap;
    }

    /**
     * @throws IllegalStateException if the parent {@code Enricher} does not have a function for a field enrichment
     */
    @SuppressWarnings("MethodWithMultipleLoops") // OK because we iterate through fields and options.
    /* package */ List<EnrichmentFunction<?, ?>> validate() {
        final ImmutableList.Builder<EnrichmentFunction<?, ?>> functions = ImmutableList.builder();

        final ImmutableBiMap.Builder<Descriptors.FieldDescriptor, Descriptors.FieldDescriptor> fields =
                ImmutableBiMap.builder();

        for (Descriptors.FieldDescriptor targetField : targetDescriptor.getFields()) {
            final Map<Descriptors.FieldDescriptor, Object> allOptions = targetField.getOptions()
                                                                                   .getAllFields();
            for (Descriptors.FieldDescriptor option : allOptions.keySet()) {
                if (option.equals(byOptionDescriptor)) {
                    final String srcFieldRef = (String) allOptions.get(option);
                    final Descriptors.FieldDescriptor srcField = resolveFieldRef(srcFieldRef, targetField);

                    final EnrichmentFunction<?, ?> function = getEnrichmentFunction(srcField, targetField);
                    functions.add(function);
                    fields.put(srcField, targetField);
                }
            }
        }
        this.sourceToTargetMap = fields.build();
        return functions.build();
    }

    private Descriptors.FieldDescriptor resolveFieldRef(String sourceFieldReference,
                                                        Descriptors.FieldDescriptor targetField) {

        // In the following code we assume that the reference is not qualified.
        //TODO:2016-06-17:alexander.yevsyukov: Handle the sibling type and another package reference too.

        // Now try to find a field with such a name in the outer (source) message.
        final Descriptors.FieldDescriptor srcField = sourceDescriptor.findFieldByName(sourceFieldReference);
        if (srcField == null) {
            final String msg = String.format(
                    "Unable to find the field `%s` in the message `%s`. " +
                            "The field is referenced in the option of the field `%s`",
                    sourceFieldReference,
                    sourceDescriptor.getFullName(),
                    targetField.getFullName());
            throw new IllegalStateException(msg);
        }
        return srcField;
    }

    private EnrichmentFunction<?, ?> getEnrichmentFunction(Descriptors.FieldDescriptor srcField,
                                                           Descriptors.FieldDescriptor targetField) {
        final Class<?> sourceFieldClass = Messages.getFieldClass(srcField);
        final Class<?> targetFieldClass = Messages.getFieldClass(targetField);

        final Optional<EnrichmentFunction<?, ?>> func = enricher.functionFor(sourceFieldClass, targetFieldClass);
        if (!func.isPresent()) {
            final String msg = String.format(
                    "There is no enrichment function for translating %s to %s",
                    sourceFieldClass,
                    targetFieldClass);
            throw new IllegalStateException(msg);
        }
        return func.get();
    }
}
