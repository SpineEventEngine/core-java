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
package org.spine3.validate;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.time.Time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.validate.Validate.isDefault;
import static org.spine3.validate.ValidatingBuilders.newInstance;

/**
 * @author Alex Tymchenko
 */
public class ValidatingBuildersShould {

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(ValidatingBuilders.class);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullPointerTester tester = new NullPointerTester();
        tester.testStaticMethods(ValidatingBuilders.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void propagate_exceptions_occurred_in_newBuilder_as_ISE() {
        try {
            newInstance(CannotBuildValidatingBuilder.class);
            fail("Exception is expected, but not thrown");
        } catch (Exception e) {
            assertEquals(CannotBuildValidatingBuilder.EXCEPTION, e.getCause());
        }
    }

    @Test
    public void provide_Empty_validating_builder() {
        assertNotNull(newInstance(EmptyValidatingBuilder.class));
    }

    @Test
    public void provide_StringValue_validating_builder() {
        final StringValue sampleState = StringValue.newBuilder()
                                                   .setValue("Sample value")
                                                   .build();
        checkBuilder(StringValueValidatingBuilder.class, sampleState);

        final StringValueValidatingBuilder builder =
                newInstance(StringValueValidatingBuilder.class);
        final StringValue actual = builder.setValue(sampleState.getValue())
                                          .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_Timestamp_validating_builder() {

        final Timestamp sampleState = Time.getCurrentTime();
        checkBuilder(TimestampValidatingBuilder.class, sampleState);

        final TimestampValidatingBuilder builder = newInstance(TimestampValidatingBuilder.class);
        final Timestamp actual = builder.setSeconds(sampleState.getSeconds())
                                        .setNanos(sampleState.getNanos())
                                        .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_Any_validating_builder() {
        final Any sampleState = AnyPacker.pack(Time.getCurrentTime());
        checkBuilder(AnyValidatingBuilder.class, sampleState);

        final AnyValidatingBuilder builder = newInstance(AnyValidatingBuilder.class);
        final Any actual = builder.mergeFrom(sampleState)
                                  .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_Int32Value_validating_builder() {
        final Int32Value sampleState = Int32Value.newBuilder()
                                                 .setValue(32)
                                                 .build();
        checkBuilder(Int32ValueValidatingBuilder.class, sampleState);

        final Int32ValueValidatingBuilder builder = newInstance(Int32ValueValidatingBuilder.class);
        final Int32Value actual = builder.setValue(sampleState.getValue())
                                         .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_Int64Value_validating_builder() {
        final Int64Value sampleState = Int64Value.newBuilder()
                                                 .setValue(64)
                                                 .build();
        checkBuilder(Int64ValueValidatingBuilder.class, sampleState);

        final Int64ValueValidatingBuilder builder = newInstance(Int64ValueValidatingBuilder.class);
        final Int64Value actual = builder.setValue(sampleState.getValue())
                                         .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_UInt32Value_validating_builder() {
        final UInt32Value sampleState = UInt32Value.newBuilder()
                                                   .setValue(32)
                                                   .build();
        checkBuilder(UInt32ValueValidatingBuilder.class, sampleState);

        final UInt32ValueValidatingBuilder builder =
                newInstance(UInt32ValueValidatingBuilder.class);
        final UInt32Value actual = builder.setValue(sampleState.getValue())
                                          .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_UInt64Value_validating_builder() {
        final UInt64Value sampleState = UInt64Value.newBuilder()
                                                   .setValue(64)
                                                   .build();
        checkBuilder(UInt64ValueValidatingBuilder.class, sampleState);

        final UInt64ValueValidatingBuilder builder =
                newInstance(UInt64ValueValidatingBuilder.class);
        final UInt64Value actual = builder.setValue(sampleState.getValue())
                                          .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_FloatValue_validating_builder() {
        final FloatValue sampleState = FloatValue.newBuilder()
                                                 .setValue(0.1f)
                                                 .build();
        checkBuilder(FloatValueValidatingBuilder.class, sampleState);

        final FloatValueValidatingBuilder builder = newInstance(FloatValueValidatingBuilder.class);
        final FloatValue actual = builder.setValue(sampleState.getValue())
                                         .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_DoubleValue_validating_builder() {
        final DoubleValue sampleState = DoubleValue.newBuilder()
                                                   .setValue(0.02d)
                                                   .build();
        checkBuilder(DoubleValueValidatingBuilder.class, sampleState);

        final DoubleValueValidatingBuilder builder =
                newInstance(DoubleValueValidatingBuilder.class);
        final DoubleValue actual = builder.setValue(sampleState.getValue())
                                          .build();
        assertEquals(sampleState, actual);
    }

    @Test
    public void provide_BoolValue_validating_builder() {
        final BoolValue sampleState = BoolValue.newBuilder()
                                               .setValue(true)
                                               .build();
        checkBuilder(BoolValueValidatingBuilder.class, sampleState);

        final BoolValueValidatingBuilder builder =
                newInstance(BoolValueValidatingBuilder.class);
        final BoolValue actual = builder.setValue(sampleState.getValue())
                                        .build();
        assertEquals(sampleState, actual);
    }

    private static <T extends Message, B extends Message.Builder> void
    checkBuilder(Class<? extends ValidatingBuilder<T, B>> builderClass, T sampleValue) {
        final ValidatingBuilder<T, B> builder = newInstance(builderClass);
        assertNotNull(builder);

        assertTrue(isDefault(builder.build()));

        assertTrue(!isDefault(sampleValue));
        builder.setOriginalState(sampleValue);
        assertEquals(sampleValue, builder.build());
    }

    /**
     * Sample {@code ValidatingBuilder}, that throws an exception in its {@code newBuilder()}
     * method.
     */
    public static class CannotBuildValidatingBuilder
            extends AbstractValidatingBuilder<Empty, Empty.Builder> {

        private static final RuntimeException EXCEPTION =
                new RuntimeException("that should be propagated");

        private CannotBuildValidatingBuilder() {
            super();
        }

        public static CannotBuildValidatingBuilder newBuilder() {
            throw EXCEPTION;
        }
    }
}
