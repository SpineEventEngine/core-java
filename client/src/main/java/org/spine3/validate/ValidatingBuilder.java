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

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

/**
 * An interface for all validating builders.
 *
 * <p>Validating builder is used to validate messages according
 * to the business rules during the message building.
 *
 * @author Illia Shepilov
 */
public interface ValidatingBuilder<T extends Message> {

    /**
     * Validates the field according to the protocol buffer message declaration.
     *
     * @param descriptor the {@code FieldDescriptor} of the field
     * @param fieldValue the value of the field
     * @param fieldName  the name of the field
     * @param <V>        the type of the field value
     * @throws ConstraintViolationThrowable if there are any constraint violations
     */
    <V> void validate(FieldDescriptor descriptor,
                      V fieldValue,
                      String fieldName) throws ConstraintViolationThrowable;

    /**
     * Validates and builds {@code Message}.
     *
     * @return the {@code Message} instance
     * @throws ConstraintViolationThrowable if there are any constraint violations
     */
    T build() throws ConstraintViolationThrowable;
}
