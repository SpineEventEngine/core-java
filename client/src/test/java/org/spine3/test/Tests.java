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

package org.spine3.test;

import org.spine3.Internal;

import java.lang.reflect.Constructor;
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
     * Verifies if the passed class has private parameterless constructor and invokes it
     * using Reflection.
     *
     * <p>Use this method to add utility constructor into covered code:
     * <pre>
     * public class MyUtilityShould
     *     ...
     *     {@literal @}Test
     *     public void have_private_utility_ctor() {
     *         assertTrue(hasPrivateUtilityConstructor(MyUtility.class));
     *     }
     * </pre>
     * @return true if the class has private parameterless constructor
     */
    public static boolean hasPrivateUtilityConstructor(Class<?> utilityClass) {
        final Constructor constructor;
        try {
            constructor = utilityClass.getDeclaredConstructor();
        } catch (NoSuchMethodException ignored) {
            return false;
        }

        if (!Modifier.isPrivate(constructor.getModifiers())) {
            return false;
        }

        constructor.setAccessible(true);

        //noinspection OverlyBroadCatchBlock
        try {
            // Call the constructor to include it into the coverage.

            // Some of the coding conventions may encourage throwing AssertionError to prevent the instantiation
            // of the utility class from within the class.
            constructor.newInstance();
        } catch (Exception ignored) {
            return true;
        }
        return true;
    }

    public static long currentTimeSeconds() {
        return currentTimeMillis() / MSEC_IN_SECOND;
    }
}
