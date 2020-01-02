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
import io.spine.code.proto.FieldContext;
import io.spine.code.proto.FieldDeclaration;
import io.spine.option.OptionsProto;
import io.spine.validate.Constraint;
import io.spine.validate.ConstraintTranslator;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.CustomConstraint;
import io.spine.validate.MessageValue;
import io.spine.validate.option.FieldConstraint;
import io.spine.validate.option.FieldValidatingOption;

import static com.google.common.base.Preconditions.checkNotNull;

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
@SuppressWarnings("Immutable") // effectively the 2nd type argument is an immutable Object.
public final class FakeOption extends FieldValidatingOption<Boolean> {

    FakeOption() {
        super(OptionsProto.beta);
    }

    @Override
    public Constraint constraintFor(FieldContext field) {
        RuntimeException exception = FakeOptionFactory.plannedException();
        return exception != null
               ? new FaultyConstraint(field.targetDeclaration())
               : new NoOpConstraint(field.targetDeclaration());
    }

    @Override
    public boolean shouldValidate(FieldContext value) {
        return true;
    }

    private static final class FaultyConstraint extends FieldConstraint<Boolean>
            implements CustomConstraint  {

        private FaultyConstraint(FieldDeclaration field) {
            super(true, field);
        }

        @Override
        public ImmutableList<ConstraintViolation> validate(MessageValue value) {
            RuntimeException exception = FakeOptionFactory.plannedException();
            checkNotNull(exception);
            throw exception;
        }

        @Override
        public String errorMessage(FieldContext context) {
            return "";
        }
    }

    private static final class NoOpConstraint extends FieldConstraint<Boolean>
            implements CustomConstraint {

        private NoOpConstraint(FieldDeclaration field) {
            super(true, field);
        }

        @Override
        public ImmutableList<ConstraintViolation> validate(MessageValue value) {
            return ImmutableList.of();
        }

        @Override
        public String errorMessage(FieldContext context) {
            return "";
        }
    }
}
