/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.util;

/**
 * Math utilities.
 *
 * @author Alexander Yevsyukov
 */
public class Math {

    private Math() {
    }

    /**
     * Safely multiply a long by an int.
     *
     * @param a  the first value
     * @param b  the second value
     * @return the new total
     * @throws ArithmeticException if the result overflows a long
     */
    public static long safeMultiply(long a, int b) {
        switch (b) {
            case -1:
                if (a == Long.MIN_VALUE) {
                    return throwOverflow(a, b);
                }
                return -a;
            case 0:
                return 0L;
            case 1:
                return a;
            default:
                // fall through.
        }
        final long total = a * b;
        if (total / b != a) {
            throwOverflow(a, b);
        }
        return total;
    }

    private static long throwOverflow(long a, int b) {
        throw new ArithmeticException(String.format("Multiplication overflows a long: %d * %d", a, b));
    }

    /**
     * Returns the floor division.
     *
     * @param a  the dividend
     * @param b  the divisor
     * @return the floor division. Examples:
     * <ul>
     *   <li>returns {@code 0} for {@code floorDiv(0, 4)}
     *   <li>returns {@code -1} for {@code floorDiv(-1, 4)}
     *   <li>returns {@code -1} for {@code floorDiv(-2, 4)}
     *   <li>returns {@code -1} for {@code floorDiv(-3, 4)}
     *   <li>returns {@code -1} for {@code floorDiv(-4, 4)}
     *   <li>returns {@code -2} for {@code floorDiv(-5, 4)}
     * </ul>
     * <li>
     */
    public static long floorDiv(long a, long b) {
        return (a >= 0 ? a / b : ((a + 1) / b) - 1);
    }
}
