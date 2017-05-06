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

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;

/**
 * Utility class for working with {@linkplain ValidatingBuilder validating builders}.
 *
 * @author Alex Tymchenko
 */
public class ValidatingBuilders {

    private ValidatingBuilders() {
        // Prevent instantiation of this utility class.
    }

    public static final class StringValueValidatingBuilder
            extends AbstractValidatingBuilder<StringValue, StringValue.Builder> {

        private StringValueValidatingBuilder() {
            // Prevent instantiation from the outside.
        }

        public static StringValueValidatingBuilder newBuilder() {
            return new StringValueValidatingBuilder();
        }

        public StringValueValidatingBuilder setValue(String value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    public static final class TimestampValidatingBuilder
            extends AbstractValidatingBuilder<Timestamp, Timestamp.Builder> {

        private TimestampValidatingBuilder() {
            // Prevent instantiation from the outside.
        }

        public static TimestampValidatingBuilder newBuilder() {
            return new TimestampValidatingBuilder();
        }
    }

    public static final class UInt32ValueValidatingBuilder
            extends AbstractValidatingBuilder<UInt32Value, UInt32Value.Builder> {

        private UInt32ValueValidatingBuilder() {
            // Prevent instantiation from the outside.
        }

        public static UInt32ValueValidatingBuilder newBuilder() {
            return new UInt32ValueValidatingBuilder();
        }

        public UInt32ValueValidatingBuilder setValue(int value) {
            getMessageBuilder().setValue(value);
            return this;
        }
    }

    public static final class AnyValidatingBuilder
            extends AbstractValidatingBuilder<Any, Any.Builder> {

        private AnyValidatingBuilder() {
            // Prevent instantiation from the outside.
        }

        public static AnyValidatingBuilder newBuilder() {
            return new AnyValidatingBuilder();
        }
    }

    public static final class EmptyValidatingBuilder
            extends AbstractValidatingBuilder<Empty, Empty.Builder> {

        private EmptyValidatingBuilder() {
            // Prevent instantiation from the outside.
        }

        public static EmptyValidatingBuilder newBuilder() {
            return new EmptyValidatingBuilder();
        }
    }
}
