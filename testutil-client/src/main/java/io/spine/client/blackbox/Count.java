/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.client.blackbox;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An integer Tiny Type representing a numeric value that can not be negative.
 *
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public final class Count {

    private static final Count NONE = new Count(0);
    private static final Count ONCE = new Count(1);
    private static final Count TWICE = new Count(2);
    private static final Count THRICE = new Count(3);

    private final int value;

    private Count(int value) {
        this.value = value;
    }

    /** @return an integer value of the current count. */
    public int value() {
        return value;
    }

    /**
     * {@link Count Count} static factory creating an instance with the provided value.
     *
     * <p>Additionally checks the value to be equal or more than 0, because count
     * cannot be negative.
     *
     * @param value a value for a new count instance
     * @return an instance of count with a provided value
     */
    public static Count count(int value) {
        checkArgument(value >= 0, "Count can not be negative");
        return new Count(value);
    }

    /**
     * A literate shortcut for {@code count(0)}.
     *
     * @return an instance of {@link Count Count} with 0 as a value
     */
    public static Count none() {
        return NONE;
    }

    /**
     * A literate shortcut for {@code count(1)}.
     *
     * @return an instance of {@link Count Count} with 1 as a value
     */
    public static Count once() {
        return ONCE;
    }

    /**
     * A literate shortcut for {@code count(2)}.
     *
     * @return an instance of {@link Count Count} with 2 as a value
     */
    public static Count twice() {
        return TWICE;
    }

    /**
     * A literate shortcut for {@code count(3)}.
     *
     * @return an instance of {@link Count Count} with 3 as a value
     */
    public static Count thrice() {
        return THRICE;
    }
}
