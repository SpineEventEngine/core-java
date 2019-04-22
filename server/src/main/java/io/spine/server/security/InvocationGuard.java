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

package io.spine.server.security;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Controls which class can call a method.
 */
public final class InvocationGuard {

    /** Prevents instantiation of this utility class. */
    private InvocationGuard() {
    }

    /**
     * Throws {@link SecurityException} of the calling class is not that passed.
     */
    public static void allowOnly(String allowedCallerClass) {
        checkNotNull(allowedCallerClass);
        Class callingClass = CallerProvider.instance()
                                           .getPreviousCallerClass();
        if (!allowedCallerClass.equals(callingClass.getName())) {
            throw nonAllowedCaller(callingClass);
        }
    }

    /**
     * Throws {@link SecurityException} of the calling class is not among the named.
     */
    public static void allowOnly(String firstClass, String... otherClasses) {
        checkNotNull(firstClass);
        checkNotNull(otherClasses);
        Class callingClass = CallerProvider.instance()
                                           .getPreviousCallerClass();
        ImmutableSet<String> allowedCallers = ImmutableSet
                .<String>builder()
                .add(firstClass)
                .add(otherClasses)
                .build();
        if (!allowedCallers.contains(callingClass.getName())) {
            throw nonAllowedCaller(callingClass);
        }
    }

    private static SecurityException nonAllowedCaller(Class callingClass) {
        String msg = format(
                "The class %s is not allowed to perform this operation.", callingClass
        );
        throw new SecurityException(msg);
    }
}
