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

package org.spine3.server.validate;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.FieldPath;
import org.spine3.validate.ConstraintViolation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates that one of the fields defined by the {@code required_field} option is present.
 *
 * See definition of {@code MessageOptions.required_field} in {@code validate.proto}.
 *
 * @author Alexander Yevsyukov
 */
class AlternativeFieldValidator {

    /**
     * The name of the message option field.
     */
    private static final String OPTION_REQUIRED_FIELD = "required_field";

    /**
     * The separator of field name (or field combination) options
     */
    private static final char OPTION_SEPARATOR = '|';

    /**
     * Combination of fields are made with ampersand.
     */
    private static final char AMPERSAND = '&';

    /**
     * The pattern to remove whitespace from the option field value.
     */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * The descriptor of the message we validate.
     */
    private final Descriptor messageDescriptor;

    /**
     * The field path of the message we validate.
     */
    private final FieldPath rootFieldPath;

    /**
     * The list builder to accumulate violations.
     */
    private final ImmutableList.Builder<ConstraintViolation> violations = ImmutableList.builder();

    AlternativeFieldValidator(Descriptor messageDescriptor, FieldPath rootFieldPath) {
        this.messageDescriptor = messageDescriptor;
        this.rootFieldPath = rootFieldPath;
    }

    List<? extends ConstraintViolation> validate(Message message) {
        final Map<FieldDescriptor, Object> options = messageDescriptor.getOptions()
                                                                      .getAllFields();
        for (FieldDescriptor optionDescriptor : options.keySet()) {
            if (OPTION_REQUIRED_FIELD.equals(optionDescriptor.getName())) {
                final JavaType optionType = optionDescriptor.getJavaType();
                if (optionType == JavaType.STRING) {
                    final String requiredFieldExpression = (String) options.get(optionDescriptor);
                    final ImmutableList<RequiredFieldOption> fieldOptions = parse(requiredFieldExpression);
                    if (!alternativeFound(message, fieldOptions)) {
                        ConstraintViolation requiredFieldNotFound = ConstraintViolation.newBuilder()
                                .setMsgFormat("None of the fields match the `required_field` definition: %s")
                                .addParam(requiredFieldExpression)
                                .build();
                        violations.add(requiredFieldNotFound);
                    }
                } else {
                    log().warn("`{}` is not of string type. Found: {}", OPTION_REQUIRED_FIELD, optionType);
                }
            }
        }
        return violations.build();
    }

    private static ImmutableList<RequiredFieldOption> parse(String optionsDefinition) {
        final ImmutableList.Builder<RequiredFieldOption> alternatives = ImmutableList.builder();
        final String whiteSpaceRemoved = WHITESPACE.matcher(optionsDefinition)
                                                   .replaceAll("");
        final Iterable<String> parts = Splitter.on(OPTION_SEPARATOR)
                                               .split(whiteSpaceRemoved);
        for (String part : parts) {
            if (part.indexOf(AMPERSAND) > 0) {
                alternatives.add(RequiredFieldOption.ofCombination(part));
            } else {
                alternatives.add(RequiredFieldOption.ofField(part));
            }
        }
        return alternatives.build();
    }

    private boolean alternativeFound(Message message, Iterable<RequiredFieldOption> fieldOptions) {
        for (RequiredFieldOption option : fieldOptions) {
            boolean found = option.isCombination()
                            ? checkCombination(message, option.getFieldNames())
                            : checkField(message, option.getFieldName());
            if (found) {
                return true;
            }
        }
        return false;
    }

    private boolean checkField(Message message, String fieldName) {
        final FieldDescriptor field = messageDescriptor.findFieldByName(fieldName);
        if (field == null) {
            ConstraintViolation notFound = ConstraintViolation.newBuilder()
                                                              .setMsgFormat("Field %s not found")
                                                              .addParam(fieldName)
                                                              .build();
            violations.add(notFound);
            return false;
        }

        Object fieldValue = message.getField(field);
        final FieldValidator<?> fieldValidator = FieldValidatorFactory.createStrict(field, fieldValue, rootFieldPath);
        final List<ConstraintViolation> violations = fieldValidator.validate();

        // Do not add violations to the results because we have options.
        // The violation would be that none of the field or combinations is defined.

        return violations.isEmpty();
    }

    private boolean checkCombination(Message message, ImmutableList<String> fieldNames) {
        for (String fieldName : fieldNames) {
            if (!checkField(message, fieldName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Represents an alternative in the definition of {@code required_field}.
     *
     * <p>It can be either a field or combination of fields.
     */
    private static class RequiredFieldOption {

        @Nullable
        private final String fieldName;
        @Nullable
        private final ImmutableList<String> fieldNames;

        private RequiredFieldOption(String fieldName) {
            this.fieldName = fieldName;
            this.fieldNames = null;
        }

        private RequiredFieldOption(Iterable<String> fieldNames) {
            this.fieldName = null;
            this.fieldNames = ImmutableList.copyOf(fieldNames);
        }

        static RequiredFieldOption ofField(String fieldName) {
            return new RequiredFieldOption(fieldName);
        }

        static RequiredFieldOption ofCombination(Iterable<String> fieldNames) {
            return new RequiredFieldOption(fieldNames);
        }

        static RequiredFieldOption ofCombination(CharSequence expression) {
            final Iterable<String> parts = Splitter.on(AMPERSAND)
                                                   .split(expression);
            return ofCombination(parts);
        }

        boolean isField() {
            return fieldName != null;
        }

        boolean isCombination() {
            return fieldNames != null;
        }

        String getFieldName() {
            if (fieldName == null) {
                throw new IllegalStateException("The option is not a field but a combination of fields.");
            }
            return fieldName;
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField")     // It is OK to suppress as we're using ImmutableList.
        ImmutableList<String> getFieldNames() {
            if (fieldNames == null) {
                throw new IllegalStateException("The option is not a combination, but a single field.");
            }

            return fieldNames;
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(AlternativeFieldValidator.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
