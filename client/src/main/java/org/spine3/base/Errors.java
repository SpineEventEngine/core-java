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

package org.spine3.base;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

/**
 * Utility class for working with {@link Error}s.
 *
 * @author Alexander Yevsyukov
 */
public class Errors {

    private Errors() {}

    /** Creates new instance of {@link Error} by the passed exception. */
    public static Error fromException(Exception exception) {
        final String message = exception.getMessage();
        final Error result = Error.newBuilder()
                                  .setType(exception.getClass()
                                                    .getName())
                                  .setMessage(message)
                                  .setStacktrace(Throwables.getStackTraceAsString(exception))
                                  .build();
        return result;
    }

    /** Creates new instance of {@link Error} by the passed {@code Throwable}. */
    public static Error fromThrowable(Throwable throwable) {
        final String message = Strings.nullToEmpty(throwable.getMessage());
        final Error result = Error.newBuilder()
                                  .setType(throwable.getClass()
                                                    .getName())
                                  .setMessage(message)
                                  .setStacktrace(Throwables.getStackTraceAsString(throwable))
                                  .build();
        return result;
    }
}
