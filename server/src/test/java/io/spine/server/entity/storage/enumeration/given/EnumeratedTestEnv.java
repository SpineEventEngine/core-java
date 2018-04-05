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

package io.spine.server.entity.storage.enumeration.given;

import io.spine.server.entity.storage.enumeration.Enumerated;

import static io.spine.server.entity.storage.enumeration.given.EnumeratedTestEnv.TestEnum.ONE;
import static io.spine.server.entity.storage.enumeration.EnumType.STRING;

/**
 * @author Dmytro Kuzmin
 */
public class EnumeratedTestEnv {

    private EnumeratedTestEnv() {
        // Prevent instantiation of this utility class.
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestClass {

        public TestClass() {
        }

        public int getInt() {
            return 42;
        }

        public TestEnum getEnumNotAnnotated() {
            return ONE;
        }

        @Enumerated
        public TestEnum getEnumOrdinal() {
            return ONE;
        }

        @Enumerated(STRING)
        public TestEnum getEnumString() {
            return ONE;
        }
    }

    public enum TestEnum {
        ZERO,
        ONE
    }
}
