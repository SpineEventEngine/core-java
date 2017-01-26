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

package org.spine3.util;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AccessOfSystemProperties")
public class EnvironmentShould {

    @After
    public void cleanUp() throws NoSuchFieldException, IllegalAccessException {
        // Clean the value of `Environment.tests` field so that the next test would run on a clean Environment.
        Field tests = Environment.class.getDeclaredField("tests");
        tests.setAccessible(true);
        tests.set(Environment.getInstance(), null);
    }

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Environment.class));
    }

    @Test
    public void tell_when_not_running_under_AppEngine() {
        // Tests are not run by AppEngine by default.
        assertFalse(Environment.getInstance().isAppEngine());
    }

    @Test
    public void obtain_AppEngine_version_as_optional_string() {
        // By default we're not running under AppEngine.
        assertFalse(Environment.getInstance().appEngineVersion().isPresent());
    }

    @Test
    public void tell_that_we_are_under_tests_if_env_var_set_to_true() throws Exception {
        try {
            System.setProperty(Environment.ENV_KEY_TESTS, Environment.VAL_TRUE);

            assertTrue(Environment.getInstance().isTests());

        } finally {
            System.clearProperty(Environment.ENV_KEY_TESTS);
        }
    }

    @Test
    public void tell_that_we_are_under_tests_if_env_var_set_to_1() throws Exception {
        try {
            System.setProperty(Environment.ENV_KEY_TESTS, "1");

            assertTrue(Environment.getInstance().isTests());

        } finally {
            System.clearProperty(Environment.ENV_KEY_TESTS);
        }
    }

    @Test
    public void tell_that_we_are_under_tests_if_run_under_known_framework() {
        // As we run this from under JUnit...
        assertTrue(Environment.getInstance().isTests());
    }
}
