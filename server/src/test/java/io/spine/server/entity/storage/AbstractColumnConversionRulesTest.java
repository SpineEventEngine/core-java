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

package io.spine.server.entity.storage;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.server.entity.storage.given.TestConversionRules;
import io.spine.test.entity.TaskView;
import io.spine.test.entity.TaskViewId;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.entity.storage.given.TestConversionRules.CONVERTED_MESSAGE;
import static io.spine.server.entity.storage.given.TestConversionRules.CONVERTED_STRING;
import static io.spine.server.entity.storage.given.TestConversionRules.NULL_VALUE;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`AbstractColumnConversionRules` should")
class AbstractColumnConversionRulesTest {

    private final ColumnConversionRules<String> conversionRules = new TestConversionRules();

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicInstanceMethods(conversionRules);
    }

    @Test
    @DisplayName("obtain a conversion rule for the given proto type")
    void obtainForType() {
        ConversionRule<?, ? extends String> rule = conversionRules.of(String.class);
        String result = rule.applyTo("some-string");
        assertThat(result).isEqualTo(CONVERTED_STRING);
    }

    @Test
    @DisplayName("obtain a conversion rule of a supertype if the given proto type is not present")
    void obtainForSupertype() {
        Timestamp timestamp = Time.currentTime();
        ConversionRule<?, ? extends String> rule = conversionRules.of(Timestamp.class);
        String result = rule.applyTo(timestamp);
        assertThat(result).isEqualTo(CONVERTED_MESSAGE);
    }

    @Test
    @DisplayName("obtain a conversion rule for `null`")
    void obtainForNull() {
        ConversionRule<@Nullable ?, ? extends String> rule = conversionRules.ofNull();
        String result = rule.apply(null);
        assertThat(result).isEqualTo(NULL_VALUE);
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Called to throw exception.
    @Test
    @DisplayName("throw an `IAE` when the rule is not found for the specified type")
    void throwOnUnknownType() {
        assertThrows(IllegalArgumentException.class,
                     () -> conversionRules.of(AbstractColumnConversionRulesTest.class));
    }

    @Test
    @DisplayName("allow to setup custom rules in the derived classes")
    void allowToSetupCustomRules() {
        TaskView taskView = taskView();
        ConversionRule<?, ? extends String> rule = conversionRules.of(TaskView.class);
        String result = rule.applyTo(taskView);
        assertThat(result).isEqualTo(taskView.getName());
    }

    @Test
    @DisplayName("consider supertypes when obtaining custom rules")
    void obtainCustomForSupertype() {
        TaskViewId id = taskViewId();
        ConversionRule<?, ? extends String> rule = conversionRules.of(TaskViewId.class);
        String result = rule.applyTo(id);
        assertThat(result).isEqualTo(String.valueOf(id.getId()));
    }

    private static TaskView taskView() {
        return TaskView
                .newBuilder()
                .setId(taskViewId())
                .build();
    }

    private static TaskViewId taskViewId() {
        return TaskViewId
                .newBuilder()
                .setId(42)
                .build();
    }
}
