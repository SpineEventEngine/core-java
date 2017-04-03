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

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.spine3.base.ConversionException;
import org.spine3.base.FieldPath;
import org.spine3.base.StringifierRegistry;
import org.spine3.base.Stringifiers;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.ConstraintViolationThrowable;

import java.lang.reflect.Type;
import java.util.List;

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
     * @param type  the key of the {@code StringifierRegistry} storage
     *              to obtain the {@code Stringifier}
     * @param value the value to convert
     * @param <V>   the type of the converted value
     * @return the converted value
     * @throws ConversionException if passed value cannot be converted
     */
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    // It is OK because caught exception is not ignored,
    // it delivers information for the exception to throw.
    public <V> V getConvertedValue(Type type, String value) throws ConversionException {
        try {
            final V convertedValue = Stringifiers.fromString(value, type);
            return convertedValue;
        } catch (RuntimeException ex) {
            throw new ConversionException(ex.getMessage(), ex);
        }
    }

    /**
     * Validates the field according to the protocol buffer message declaration.
     *
     * @param descriptor the {@code FieldDescriptor} of the field
     * @param fieldValue the value of the field
     * @param fieldName  the name of the field
     * @param <V>        the type of the field value
     * @throws ConstraintViolationThrowable if there are some constraint violations
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
