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

package io.spine.test.validation;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import io.spine.code.proto.FieldContext;
import io.spine.option.OptionsProto;
import io.spine.type.MessageType;
import io.spine.validate.Constraint;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.CustomConstraint;
import io.spine.validate.MessageValue;
import io.spine.validate.option.FieldValidatingOption;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A test-only implementation of a validating option.
 *
 * <p>It is sometimes required for tests that a call to validation fails with a predictable
 * exception. In such cases, the test authors may set up the fake option to fail validation.
 *
 * <p>This option is applied to any given field. Although the {@link #extension()} method returns
 * the {@code beta} extension, the option is not bound to any real Protobuf option.
 *
 * <p>If the {@link FakeOptionFactory#plannedException()} is non-null, the planned exception is
 * thrown by the constraint produced by this option. Otherwise, the constraint never discovers any
 * violations.
 */
@Immutable
public final class FakeOption extends FieldValidatingOption<Void> {

    FakeOption() {
        super(createExtension());
    }

    @SuppressWarnings("unchecked") // OK for tests.
    private static GeneratedExtension<FieldOptions, Void> createExtension() {
        GeneratedExtension<FieldOptions, ?> beta = OptionsProto.beta;
        return (GeneratedExtension<FieldOptions, Void>) beta;
    }

    @Override
    public Constraint constraintFor(FieldContext value) {
        RuntimeException exception = FakeOptionFactory.plannedException();
        return new FakeConstraint(value, exception);
    }

    @SuppressWarnings("Immutable")  // effectively immutable.
    private static final class FakeConstraint implements CustomConstraint {

        private final FieldContext value;
        private final @Nullable RuntimeException exception;

        private FakeConstraint(FieldContext value, @Nullable RuntimeException exception) {
            this.value = value;
            this.exception = exception;
        }

        @Override
        public ImmutableList<ConstraintViolation> validate(MessageValue value) {
            if (exception != null) {
                throw exception;
            }
            return ImmutableList.of();
        }

        @Override
        public MessageType targetType() {
            return new MessageType(value.target()
                                        .getMessageType());
        }

        @Override
        public String errorMessage(FieldContext context) {
            return "";
        }
    }
}
