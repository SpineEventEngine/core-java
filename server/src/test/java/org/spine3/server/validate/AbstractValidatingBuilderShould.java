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
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.ConversionException;
import org.spine3.base.Stringifier;
import org.spine3.base.StringifierRegistry;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.TaskId;
import org.spine3.test.types.Task;
import org.spine3.test.validate.msg.PatternStringFieldValue;
import org.spine3.validate.ConstraintViolationThrowable;

import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.base.Stringifiers.listStringifier;
import static org.spine3.base.Types.listTypeOf;

/**
 * @author Illia Shepilov
 */
public class AbstractValidatingBuilderShould {

    private AbstractValidatingBuilder<Task> validatingBuilder;

    @Before
    public void setUp() {
        validatingBuilder = new TestValidatingBuilder();
    }

    @Test
    public void return_converted_value() throws ConversionException {
        final Type type = listTypeOf(Integer.class);
        final Stringifier<List<Integer>> stringifier = listStringifier(Integer.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        final List<Integer> convertedValue =
                validatingBuilder.getConvertedValue(type, "\"1\"");
        final List<Integer> expectedList = newArrayList(1);
        assertThat(convertedValue, is(expectedList));
    }

    @Test(expected = ConversionException.class)
    public void throw_exception_when_appropriate_stringifier_is_not_found() throws
                                                                            ConversionException {
        final String stringToConvert = "{value:1}";
        validatingBuilder.getConvertedValue(TaskId.class, stringToConvert);
    }

    @Test(expected = ConversionException.class)
    public void throw_exception_when_string_cannot_be_converted() throws ConversionException {
        final String stringToConvert = "";
        final Type type = listTypeOf(Timestamp.class);
        final Stringifier<List<Timestamp>> stringifier = listStringifier(Timestamp.class);
        StringifierRegistry.getInstance()
                           .register(stringifier, type);
        validatingBuilder.getConvertedValue(type, stringToConvert);
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
        validatingBuilder.validate(descriptor, "incorrectEmail", "email");
    }

    private static class TestValidatingBuilder extends AbstractValidatingBuilder<Task> {
        @Override
        public Task build() {
            return Task.getDefaultInstance();
        }
    }
}
