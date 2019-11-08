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

package io.spine.client;

import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Augments {@link CompositeFilter} with useful methods.
 */
@SuppressWarnings("override") // to handle the absence of `@Override` in the generated code.
@GeneratedMixin
public interface CompositeFilterMixin extends Message, Predicate<Message> {

    List<Filter> getFilterList();
    CompositeFilter.CompositeOperator getOperator();

    /**
     * Verifies if this filter passed the message.
     */
    @Override
    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // OK for Proto enum.
    default boolean test(Message message) {
        Stream<Filter> filters = getFilterList().stream();
        CompositeFilter.CompositeOperator operator = getOperator();
        Predicate<Filter> passesFilter = f -> f.test(message);
        switch (operator) {
            case ALL:
                return filters.allMatch(passesFilter);
            case EITHER:
                return filters.anyMatch(passesFilter);
            default:
                throw newIllegalArgumentException(
                        "Unknown composite filter operator `%s`.", operator);
        }
    }
}
