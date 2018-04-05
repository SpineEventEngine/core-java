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

package io.spine.server.entity.storage.enumeration;

import com.google.common.testing.NullPointerTester;
import org.junit.Test;

import java.io.Serializable;

import static io.spine.server.entity.storage.enumeration.EnumConverters.forType;
import static io.spine.server.entity.storage.enumeration.EnumType.ORDINAL;
import static io.spine.server.entity.storage.enumeration.EnumType.STRING;
import static io.spine.server.entity.storage.enumeration.given.EnumeratedTestEnv.TestEnum.ONE;
import static org.junit.Assert.assertEquals;

public class EnumConverterShould {

    @Test
    public void not_accept_nulls() {
        final EnumConverter<? extends Serializable> converter = forType(ORDINAL);
        new NullPointerTester().testAllPublicInstanceMethods(converter);
    }

    @Test
    public void convert_ordinal_value() {
        final EnumConverter<? extends Serializable> converter = forType(ORDINAL);
        final Serializable value = converter.convert(ONE);
        assertEquals(ONE.ordinal(), value);
    }

    @Test
    public void convert_string_value() {
        final EnumConverter<? extends Serializable> converter = forType(STRING);
        final Serializable value = converter.convert(ONE);
        assertEquals(ONE.name(), value);
    }
}
