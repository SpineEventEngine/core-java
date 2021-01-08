/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.client;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.Field;
import io.spine.protobuf.TypeConverter;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.OperatorEvaluator.eval;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Augments {@link Filter} with useful methods.
 */
@GeneratedMixin
interface FilterMixin extends FilterOrBuilder, MessageFilter<Message> {

    /**
     * Verifies that the filter can be applied to the given {@code target}.
     *
     * <p>Makes sure the field specified in the filter is a valid entity column or a message field
     * in the type enclosed by the {@code target}.
     *
     * @throws IllegalStateException
     *         if the field is not present in the target type or doesn't satisfy the constraints
     */
    default void checkCanApplyTo(Target target) {
        checkNotNull(target);
        FilteringField field = new FilteringField(this);
        field.checkAppliesTo(target);
    }

    /**
     * Verifies if this filter passed the message.
     */
    @Override
    default boolean test(Message message) {
        Field field = Field.withPath(getFieldPath());
        Object actual = field.valueIn(message);
        Any requiredAsAny = getValue();
        Object required = TypeConverter.toObject(requiredAsAny, actual.getClass());
        try {
            return eval(actual, getOperator(), required);
        } catch (IllegalArgumentException e) {
            throw newIllegalArgumentException(
                    e,
                    "Filter value `%s` cannot be properly compared to" +
                            " the message field `%s` of the class `%s`.",
                    required, field, actual.getClass().getName()
            );
        }
    }
}
