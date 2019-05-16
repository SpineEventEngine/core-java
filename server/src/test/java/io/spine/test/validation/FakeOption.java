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
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import io.spine.option.OptionsProto;
import io.spine.validate.Constraint;
import io.spine.validate.FieldValidatingOption;
import io.spine.validate.FieldValue;

public final class FakeOption extends FieldValidatingOption<Void, Object> {

    FakeOption() {
        super(createExtension());
    }

    @SuppressWarnings("unchecked") // OK for tests.
    private static GeneratedExtension<DescriptorProtos.FieldOptions, Void> createExtension() {
        GeneratedExtension<DescriptorProtos.FieldOptions, ?> beta = OptionsProto.beta;
        return (GeneratedExtension<DescriptorProtos.FieldOptions, Void>) beta;
    }

    @Override
    public Constraint<FieldValue<Object>> constraintFor(FieldValue<Object> value) {
        RuntimeException exception = FakeOptionFactory.plannedException();
        if (exception != null) {
            return v -> { throw exception; };
        } else {
            return v -> ImmutableList.of();
        }
    }

    @Override
    protected boolean shouldValidate(FieldDescriptor field) {
        return true;
    }
}
