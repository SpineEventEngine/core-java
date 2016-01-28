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

import com.google.common.annotations.VisibleForTesting;
import org.spine3.Internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static java.lang.System.currentTimeMillis;

/**
 * Utilities for testing.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class Tests {

    private static final long MSEC_IN_SECOND = 1000L;

    private Tests() {}

    /**
     * Utility method for verifying existence of the private constructor in the passed utility class
     * and inclusion of the lines of code of this constructor into coverage report.
     *
     * @param utilityClass a utility class to verify
     * @throws java.lang.IllegalStateException if the constructor of the passed class isn't private
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     * @throws InstantiationException
     */
    @VisibleForTesting
    @SuppressWarnings("MethodWithTooExceptionsDeclared")
    public static void callPrivateUtilityConstructor(Class<?> utilityClass)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final Constructor constructor = utilityClass.getDeclaredConstructor();

        // We throw exception if the modified isn't private instead of asserting via JUnit to avoid dependency
        // on JUnit in the main part of the code.
        if (!Modifier.isPrivate(constructor.getModifiers())) {
            throw new IllegalStateException("Constructor must be private.");
        }

        constructor.setAccessible(true);
        constructor.newInstance();
    }

    public static long currentTimeSeconds() {
        return currentTimeMillis() / MSEC_IN_SECOND;
    }
}
