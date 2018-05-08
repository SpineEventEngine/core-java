/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Any;
import io.spine.protobuf.TypeConverter;
import org.junit.Test;

import static io.spine.client.FilterValueConverter.toAny;
import static io.spine.client.FilterValueConverter.toValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmytro Kuzmin
 */
public class FilterValueConverterShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(FilterValueConverter.class);
    }

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(Any.class, Any.getDefaultInstance())
                .testAllPublicStaticMethods(FilterValueConverter.class);
    }

    @Test
    public void convert_non_enum_value_to_any_without_changes() {
        final int value = 42;
        final Any expected = TypeConverter.toAny(value);
        final Any actual = toAny(value);
        assertEquals(expected, actual);
    }

    @Test
    public void convert_enum_value_to_any_through_its_name() {
        final TestEnum value = TestEnum.ONE;
        final String valueName = value.name();
        final Any expected = TypeConverter.toAny(valueName);
        final Any actual = toAny(value);
        assertEquals(expected, actual);
    }

    @Test
    public void retrieve_non_enum_value_from_any() {
        final int value = 42;
        final Any convertedValue = TypeConverter.toAny(value);
        final Object unpackedValue = toValue(convertedValue, Integer.class);
        assertEquals(value, unpackedValue);
    }

    @Test
    public void retrieve_enum_value_from_any() {
        final TestEnum value = TestEnum.ONE;
        final Any convertedValue = TypeConverter.toAny(value.name());
        final Object unpackedValue = toValue(convertedValue, TestEnum.class);
        assertEquals(value, unpackedValue);
    }

    private enum TestEnum {
        ZERO,
        ONE
    }
}
