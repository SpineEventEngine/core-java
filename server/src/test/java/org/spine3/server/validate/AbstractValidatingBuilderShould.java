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
import org.junit.Before;
import org.junit.Test;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.Task;
import org.spine3.test.aggregate.TaskId;
import org.spine3.test.validate.msg.PatternStringFieldValue;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.ConversionError;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.spine3.base.Identifiers.newUuid;

/**
 * @author Illia Shepilov
 */
public class AbstractValidatingBuilderShould {

    private AbstractValidatingBuilder<Task> validatingBuilder;

    @Before
    public void setUp() {
        validatingBuilder = new ConcreteValidatingBuilder();
    }

    @Test
    public void return_converted_value() throws ConversionError {
        final Integer convertedValue = validatingBuilder.getConvertedValue(
                new SingularKey<>(Integer.class), "1");
        assertEquals(1, convertedValue.intValue());
    }

    @Test(expected = StringifierNotFoundException.class)
    public void throw_exception_when_appropriate_stringifier_is_not_found() throws ConversionError {
        final String stringToConvert = "{value:1}";
        validatingBuilder.getConvertedValue(new SingularKey<>(TaskId.class), stringToConvert);
    }

    @Test(expected = ConversionError.class)
    public void throw_exception_when_string_cannot_be_converted() throws ConversionError {
        final String stringToConvert = "";
        validatingBuilder.getConvertedValue(new PluralKey<>(List.class, Integer.class),
                                            stringToConvert);
    }

    @Test
    public void validate_value() throws ConstraintViolationThrowable {
        final FieldDescriptor descriptor = ProjectId.getDescriptor()
                                                    .getFields()
                                                    .get(0);
        validatingBuilder.validate(descriptor, newUuid(), "id");
    }

    @Test(expected = ConstraintViolationThrowable.class)
    public void throw_exception_when_field_contains_constraint_violations()
            throws ConstraintViolationThrowable {
        final FieldDescriptor descriptor = PatternStringFieldValue.getDescriptor()
                                                                  .getFields()
                                                                  .get(0);
        validatingBuilder.validate(descriptor,
                                   "incorrectEmail",
                                   "email");
    }

    private static class ConcreteValidatingBuilder extends AbstractValidatingBuilder<Task> {
        @Override
        public Task build() {
            return Task.getDefaultInstance();
        }
    }
}
