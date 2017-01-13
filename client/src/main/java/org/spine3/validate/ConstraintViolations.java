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

import java.util.List;

/**
 * Utility class for working with {@link ConstraintViolation}s.
 *
 * @author Alexander Yevsyukov
 */
public class ConstraintViolations {

    private ConstraintViolations() {
    }

    /**
     * Returns a formatted string using the format string and parameters from the violation.
     *
     * @param violation violation which contains the format string and
     *                  arguments referenced by the format specifiers in it
     * @return a formatted string
     * @see String#format(String, Object...)
     */
    public static String toText(ConstraintViolation violation) {
        final String format = violation.getMsgFormat();
        final List<String> params = violation.getParamList();
        final String result = String.format(format, params.toArray());
        return result;
    }

    /**
     * Returns a formatted string using the specified format string and parameters from the violation.
     *
     * @param format    a format string
     * @param violation violation which contains arguments referenced by the format specifiers in the format string
     * @return a formatted string
     * @see String#format(String, Object...)
     */
    public static String toText(String format, ConstraintViolation violation) {
        final List<String> params = violation.getParamList();
        final String result = String.format(format, params.toArray());
        return result;
    }
}
