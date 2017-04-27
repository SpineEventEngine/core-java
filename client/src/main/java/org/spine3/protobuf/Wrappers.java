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
package org.spine3.protobuf;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for packing wrapped values.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 * @see Wrapper
 */
public final class Wrappers {

    private Wrappers() {
        // Prevent instantiation of this utility class.
    }

    /** Packs the passed value into {@link StringValue} and then into {@link Any}. */
    public static Any pack(String value) {
        checkNotNull(value);
        final Any result = Wrapper.forString()
                                  .pack(value);
        return result;
    }

    /** Packs the passed value into {@link Any}. */
    public static Any pack(double value) {
        return Wrapper.forDouble()
                      .pack(value);
    }

    /** Packs the passed value into {@link Any}. */
    public static Any pack(float value) {
        return Wrapper.forFloat()
                      .pack(value);
    }

    /** Packs the passed value into {@link Int32Value} and then into {@link Any}. */
    public static Any pack(int value) {
        return Wrapper.forInteger()
                      .pack(value);
    }

    /** Packs the passed value into {@link Int64Value} and then into {@link Any}. */
    public static Any pack(long value) {
        return Wrapper.forLong()
                      .pack(value);
    }

    /** Creates a new {@code BoolValue} wrapping the passed value. */
    public static BoolValue newBoolValue(boolean value) {
        return Wrapper.forBoolean(value);
    }

    /** Packs the passed value into {@link BoolValue} and then into {@link Any}. */
    public static Any pack(boolean value) {
        return Wrapper.forBoolean()
                      .pack(value);
    }
}
