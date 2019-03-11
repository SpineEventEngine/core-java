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

package io.spine.testing.client.blackbox.given;

import com.google.protobuf.Value;
import io.spine.base.Error;

public class ErrorQualifierTestEnv {

    /** Prevents instantiation of this utility class. */
    private ErrorQualifierTestEnv() {
        // Does nothing.
    }

    public static Error newError(Type type, Code code, Pangram message) {
        return Error.newBuilder()
                    .setType(type.value())
                    .setCode(code.value())
                    .setMessage(message.text())
                    .build();
    }

    public static Error newError(Attribute attribute, AttributeValue value) {
        return Error.newBuilder()
                    .setCode(9876) // a non-zero value
                    .putAttributes(attribute.title(), value.value())
                    .build();
    }

    public enum Pangram {
        FIRST("How vexingly quick daft zebras jump!"),
        SECOND("Pack my box with five dozen liquor jugs."),
        THIRD("Jackdaws love my big sphinx of quartz.");

        private final String sentence;

        Pangram(String sentence) {
            this.sentence = sentence;
        }

        public String text() {
            return sentence;
        }
    }

    public enum Type {
        FIRST("type-1"),
        SECOND("type-2"),
        THIRD("type-3"),
        FOURTH("type-4"),
        ELEVEN("type-11");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Code {
        ZERO(0),
        ONE(1),
        TWO(2),
        SEVENTEEN(17);

        private final int value;

        Code(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum Attribute {
        HEIGHT("height"),
        WEIGHT("weight");

        private final String name;

        Attribute(String name) {
            this.name = name;
        }

        public String title() {
            return name;
        }
    }

    public enum Height implements AttributeValue {
        BRYANT("193 cm"),
        LEBRON("203 cm");

        private final Value value;

        Height(String value) {
            this.value = parse(value);
        }

        private static Value parse(String stringValue) {
            return Value.newBuilder()
                        .setStringValue(stringValue)
                        .build();
        }

        @Override
        public Value value() {
            return value;
        }
    }

    interface AttributeValue {

        Value value();
    }
}
