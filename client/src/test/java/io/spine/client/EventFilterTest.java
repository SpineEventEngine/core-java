/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.StringValue;
import io.spine.base.EventMessageField;
import io.spine.base.Field;
import io.spine.core.EventContext;
import io.spine.core.EventContextField;
import io.spine.protobuf.AnyPacker;
import io.spine.test.client.ClProjectCreated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_THAN;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.lang.String.format;

@DisplayName("`EventFilter` should")
class EventFilterTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(EventMessageField.class, ClProjectCreated.Field.name())
                .setDefault(EventContextField.class, EventContext.Field.external())
                .testAllPublicStaticMethods(EventFilter.class);
    }

    @Nested
    @DisplayName("create a filter targeting event message and having operator")
    class CreateForEventMessage {

        @Test
        @DisplayName("`equals`")
        void eqFilter() {
            checkCreates(EventFilter::eq, EQUAL);
        }

        @Test
        @DisplayName("`greater than`")
        void gtFilter() {
            checkCreates(EventFilter::gt, GREATER_THAN);
        }

        @Test
        @DisplayName("`less than`")
        void ltFilter() {
            checkCreates(EventFilter::lt, LESS_THAN);
        }

        @Test
        @DisplayName("`greater than or equals`")
        void geFilter() {
            checkCreates(EventFilter::ge, GREATER_OR_EQUAL);
        }

        @Test
        @DisplayName("`less than or equals`")
        void leFilter() {
            checkCreates(EventFilter::le, LESS_OR_EQUAL);
        }

        private void
        checkCreates(BiFunction<EventMessageField, Object, EventFilter> factoryMethod,
                     Filter.Operator expectedOperator) {
            var field = ClProjectCreated.Field.id();
            var value = "some-ID";
            var eventFilter = factoryMethod.apply(field, value);
            var filter = eventFilter.filter();

            var fieldPath = filter.getFieldPath();
            var nameCount = fieldPath.getFieldNameCount();
            assertThat(nameCount).isEqualTo(1);

            var fieldName = fieldPath.getFieldName(0);
            var expectedFieldName = field.getField().toString();
            assertThat(fieldName).isEqualTo(expectedFieldName);

            assertThat(filter.getOperator()).isEqualTo(expectedOperator);

            var unpacked = AnyPacker.unpack(filter.getValue(), StringValue.class);
            assertThat(unpacked.getValue()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("create a filter targeting event context and having operator")
    class CreateForEventContext {

        @Test
        @DisplayName("`equals`")
        void eqFilter() {
            checkCreates(EventFilter::eq, EQUAL);
        }

        @Test
        @DisplayName("`greater than`")
        void gtFilter() {
            checkCreates(EventFilter::gt, GREATER_THAN);
        }

        @Test
        @DisplayName("`less than`")
        void ltFilter() {
            checkCreates(EventFilter::lt, LESS_THAN);
        }

        @Test
        @DisplayName("`greater than or equals`")
        void geFilter() {
            checkCreates(EventFilter::ge, GREATER_OR_EQUAL);
        }

        @Test
        @DisplayName("`less than or equals`")
        void leFilter() {
            checkCreates(EventFilter::le, LESS_OR_EQUAL);
        }

        private void
        checkCreates(BiFunction<EventContextField, Object, EventFilter> factoryMethod,
                     Filter.Operator expectedOperator) {
            var field = EventContext.Field.commandId().uuid();
            var value = "some-UUID";
            var eventFilter = factoryMethod.apply(field, value);
            var filter = eventFilter.filter();

            var fieldPath = filter.getFieldPath();
            var nameCount = fieldPath.getFieldNameCount();
            assertThat(nameCount).isEqualTo(3);

            var actualFieldPath = Field.withPath(fieldPath).toString();
            var expectedFieldPath = format("context.%s", field.getField());
            assertThat(actualFieldPath).isEqualTo(expectedFieldPath);

            assertThat(filter.getOperator()).isEqualTo(expectedOperator);

            var unpacked = AnyPacker.unpack(filter.getValue(), StringValue.class);
            assertThat(unpacked.getValue()).isEqualTo(value);
        }
    }
}
