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

package org.spine3.server.event;

import com.google.common.base.Function;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Internal;
import com.google.protobuf.Message;
import org.spine3.annotations.EventAnnotationsProto;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * The default mechanism for enriching messages based on {@code FieldOptions} of
 * Protobuf message definitions.
 *
 * @param <M> a type of the message of the event to enrich
 * @param <E> a type of the enrichment message
 *
 * @author Alexander Yevsyukov
 */
/* package */ class EventMessageEnricher<M extends Message, E extends Message> extends EnrichmentFunction<M, E> {

    private final EventEnricher enricher;

    private EventMessageEnricher(EventEnricher enricher, Class<M> sourceClass, Class<E> targetClass) {
        super(sourceClass, targetClass, new Func<>(enricher, sourceClass, targetClass));
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

    /**
     * Performs validation checking that all fields annotated in the enrichment message
     * can be created with the translation functions supplied in the parent enricher.
     *
     * @throws IllegalStateException if the parent {@code Enricher} does not have a function for a field enrichment
     */
    @SuppressWarnings("MethodWithMultipleLoops") // is OK because of nested nature of Protobuf descriptors.
    @Override
    /* package */void validate() {
        final Descriptors.Descriptor sourceDescriptor = Internal.getDefaultInstance(getSourceClass())
                                                                .getDescriptorForType();
        final Descriptors.Descriptor targetDescriptor = Internal.getDefaultInstance(getTargetClass())
                                                                .getDescriptorForType();

        final GeneratedMessage.GeneratedExtension<DescriptorProtos.FieldOptions, String> byOption =
                EventAnnotationsProto.by;
        final Descriptors.FieldDescriptor byOptionDescriptor = byOption.getDescriptor();

        for (Descriptors.FieldDescriptor targetField : targetDescriptor.getFields()) {
            final Map<Descriptors.FieldDescriptor, Object> allOptions = targetField.getOptions()
                                                                                   .getAllFields();
            for (Descriptors.FieldDescriptor option : allOptions.keySet()) {
                if (option.equals(byOptionDescriptor)) {
                    final String fieldReference = (String) allOptions.get(option);

                    // In the following code we assume that the reference is not qualified.
                    //TODO:2016-06-17:alexander.yevsyukov: Handle the sibling type and another package reference too.

                    // Now try to find a field with such a name in the outer (source) message.
                    final Descriptors.FieldDescriptor srcField = sourceDescriptor.findFieldByName(fieldReference);
                    if (srcField == null) {
                        final String msg = String.format(
                                "Unable to find the field `%s` in the message `%s`. " +
                                        "The field is referenced in the option of the field `%s`",
                                    fieldReference,
                                    sourceDescriptor.getFullName(),
                                    targetField.getFullName());
                        throw new IllegalStateException(msg);
                    }

                    final Class<?> sourceFieldClass = srcField.getDefaultValue()
                                                              .getClass();
                    final Class<?> targetFieldClass = targetField.getDefaultValue()
                                                                 .getClass();

                    if (enricher.hasFunctionFor(sourceFieldClass, targetFieldClass)) {
                        final String msg = String.format(
                                "There is no enrichment function for translating %s to %s",
                                sourceFieldClass,
                                targetFieldClass);
                        throw new IllegalStateException(msg);
                    }
                }
            }
        }
    }

    /**
     * An interim entry in the {@link EventEnricher.Builder} to hold information about the types.
     *
     * <p>Instances of this class are converted to real version via {@link #toBound(EventEnricher)} method.
     */
    /* package */ static class Unbound<M extends Message, E extends Message> extends EnrichmentFunction<M, E> {

        /* package */Unbound(Class<M> sourceClass, Class<E> targetClass) {
            super(sourceClass, targetClass, Unbound.<M, E>empty());
        }

        private static <M extends Message, E extends Message> Function<M, E> empty() {
            return new Function<M, E>() {
                @Nullable
                @Override
                public E apply(@Nullable M input) {
                    return null;
                }
            };
        }

        /**
         * Converts the instance into the {@link EventMessageEnricher}.
         */
        /* package */ EventMessageEnricher<M, E> toBound(EventEnricher enricher) {
            final EventMessageEnricher<M, E> result = new EventMessageEnricher<>(enricher, getSourceClass(), getTargetClass());
            return result;
        }

        /**
         * @throws IllegalStateException always to prevent the usage of instances in configured {@link EventEnricher}
         */
        @Override
        /* package */void validate() {
            throw new IllegalStateException("Unbound instance cannot be used for enrichments");
        }
    }

    /**
     * This is a helper class that performs the conversion.
     */
    private static class Func<M extends Message, E extends Message> implements Function<M, E> {

        private final EventEnricher enricher;
        private final Class<M> sourceClass;
        private final Class<E> targetClass;

        private Func(EventEnricher enricher, Class<M> sourceClass, Class<E> targetClass) {
            this.enricher = enricher;
            this.sourceClass = sourceClass;
            this.targetClass = targetClass;
        }

        @Nullable
        @Override
        public E apply(@Nullable M input) {
            if (input == null) {
                return null;
            }

            //TODO:2016-06-17:alexander.yevsyukov: Implement
            return null;
        }

        /* package */ EventEnricher getEnricher() {
            return enricher;
        }
    }
}
