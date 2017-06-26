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

package io.spine.test;

import com.google.protobuf.StringValue;
import io.spine.protobuf.Wrapper;

import java.util.concurrent.ThreadLocalRandom;

import static io.spine.Identifier.newUuid;

/**
 * Utility factories for test values.
 *
 * @author Alexander Yevsyukov
 */
public class TestValues {

    private TestValues() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Generates a {@code StringValue} with generated UUID.
     *
     * <p>Use this method when you need to generate a test {@code Message} value
     * but do not want to resort to {@code Timestamp} via {@code Timestamps#getCurrentTime()}.
     */
    public static StringValue newUuidValue() {
        return Wrapper.forString(newUuid());
    }

    /**
     * Generates a random integer in the range [0, max).
     */
    public static int random(int max) {
        return random(0, max);
    }

    /**
     * Generates a random integer in the range [min, max).
     */
    public static int random(int min, int max) {
        int randomNum = ThreadLocalRandom.current().nextInt(min, max);
        return randomNum;
    }
}
