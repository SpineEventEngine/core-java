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
package io.spine.server.entity;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.validate.ValidationException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Custom implementation of {@code ValidatingBuilder}, which allows to simulate an error
 * during the state building.
 */
public abstract class ThrowingValidatingBuilder<M extends Message,
                                                B extends GeneratedMessageV3.Builder<B>>
        extends GeneratedMessageV3.Builder<B> implements ValidatingBuilder<M> {

    private final ValidatingBuilder<M> delegateBuilder;
    private @MonotonicNonNull RuntimeException shouldThrow;

    protected ThrowingValidatingBuilder(ValidatingBuilder<M> delegateBuilder) {
        this.delegateBuilder = delegateBuilder;
    }

    @Override
    public M build() throws ValidationException {
        if (shouldThrow != null) {
            throw shouldThrow;
        } else {
            return delegateBuilder.build();
        }
    }

    @Override
    public M buildPartial() {
        return delegateBuilder.buildPartial();
    }

    /**
     * Sets an exception to throw upon {@code build()}.
     */
    public void shouldThrow(RuntimeException shouldThrow) {
        checkNotNull(shouldThrow);
        this.shouldThrow = shouldThrow;
    }

    @Override
    protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        Descriptor type = delegateBuilder.getDescriptorForType();
        String[] fieldNames = type.getFields()
                                  .stream()
                                  .map(FieldDescriptor::getName)
                                  .map(String::toUpperCase)
                                  .toArray(String[]::new);
        return new GeneratedMessageV3.FieldAccessorTable(type, fieldNames);
    }

    @Override
    public Message getDefaultInstanceForType() {
        return delegateBuilder.getDefaultInstanceForType();
    }
}
