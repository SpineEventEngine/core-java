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

package org.spine3.server.validate;

import com.google.common.base.Optional;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.spine3.base.FieldPath;
import org.spine3.base.RegistryKey;
import org.spine3.base.Stringifier;
import org.spine3.base.StringifierRegistry;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.ConversionError;
import org.spine3.validate.IllegalConversionArgumentException;

import java.util.List;

import static java.lang.String.format;

/**
 * Serves as an abstract base for all validating builders.
 *
 * @author Illia Shepilov
 * @see ValidatingBuilder
 */
@SuppressWarnings({
        "AbstractClassNeverImplemented",
        /* It will be implemented during the build of project
         after the validating builders generation.*/
        "unused" // It will be used by auto-generated code.
})
public abstract class AbstractValidatingBuilder<T extends Message> implements ValidatingBuilder<T> {

    private final StringifierRegistry registry = StringifierRegistry.getInstance();

    /**
     * Converts the passed `raw` value and returns it.
     *
     * @param registryKey the key of the {@code StringifierRegistry} warehouse
     *                    to obtain {@code Stringifier}
     * @param value       the value to convert
     * @param <V>         the type of the converted value
     * @return the converted value
     * @throws ConversionError if passed value cannot be converted
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    // It is OK because caught exception is not ignored, it delivers exception to throw.
    public <V> V getConvertedValue(RegistryKey registryKey, String value) throws ConversionError {
        try {
            final Stringifier<V> stringifier = getStringifier(registryKey);
            final V convertedValue = stringifier.reverse()
                                                .convert(value);
            return convertedValue;
        } catch (IllegalConversionArgumentException ex) {
            throw ex.getConversionError();
        }
    }

    private <V> Stringifier<V> getStringifier(RegistryKey registryKey) {
        final Optional<Stringifier<V>> stringifierOptional = registry.get(registryKey);
        if (stringifierOptional.isPresent()) {
            return stringifierOptional.get();
        }

        final String exMessage =
                format("StringifierRegistry does not contain converter by specified key: %s",
                       registryKey);
        throw new StringifierNotFoundException(exMessage);
    }

    /**
     * Validates the field according to the protoBuf declaration.
     *
     * @param descriptor the {@code FieldDescriptor} of the field
     * @param fieldValue the value of the field
     * @param fieldName  the name of the field
     * @param <V>        the type of the field value
     * @throws ConstraintViolationThrowable if the validating value contains constraint violations
     */
    public <V> void validate(FieldDescriptor descriptor, V fieldValue, String fieldName)
            throws ConstraintViolationThrowable {
        final FieldPath fieldPath = FieldPath.newBuilder()
                                             .addFieldName(fieldName)
                                             .build();
        final FieldValidator<?> validator =
                FieldValidatorFactory.create(descriptor, fieldValue, fieldPath);
        final List<ConstraintViolation> constraints = validator.validate();
        if (!constraints.isEmpty()) {
            throw new ConstraintViolationThrowable(constraints);
        }
    }
}
