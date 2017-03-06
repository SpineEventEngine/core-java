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
import org.spine3.base.Stringifier;
import org.spine3.base.StringifierRegistry;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.ConstraintViolationThrowable;

import java.util.List;

import static java.lang.String.format;

/**
 * @author Illia Shepilov
 */
public abstract class CommonValidatingBuilder<T extends Message> implements ValidatingBuilder<T> {

    private final StringifierRegistry registry = StringifierRegistry.getInstance();

    public <V> V getConvertedValue(Class<V> valueClass, String value) {
        final Stringifier<V> stringifier = getStringifier(valueClass);
        final V convertedValue = stringifier.reverse()
                                            .convert(value);
        return convertedValue;
    }

    private <V> Stringifier<V> getStringifier(Class<V> valueClass) {
        final Optional<Stringifier<V>> stringifierOptional = registry.get(valueClass);
        if (stringifierOptional.isPresent()) {
            return stringifierOptional.get();
        }

        final String exMessage =
                format("StringifierRegistry does not contain converter by specified key: %s",
                       valueClass);
        throw new StringifierNotFoundException(exMessage);
    }

    public <V> void validate(FieldDescriptor descriptor, V fieldValue, String valueName)
            throws ConstraintViolationThrowable {
        final FieldPath fieldPath = FieldPath.newBuilder()
                                             .addFieldName(valueName)
                                             .build();
        final FieldValidator<?> validator =
                FieldValidatorFactory.create(descriptor, fieldValue, fieldPath);
        final List<ConstraintViolation> constraints = validator.validate();
        if (!constraints.isEmpty()) {
            throw new ConstraintViolationThrowable(constraints);
        }
    }
}
