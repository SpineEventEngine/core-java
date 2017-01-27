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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AccessOfSystemProperties")
public class EnvironmentShould {

    private Environment environment;

    /*
     * Environment protection START
     *
     * We remember the state and restore it after this test suite is complete because other tests
     * may initialize the environment.
     */
    @SuppressWarnings("StaticVariableMayNotBeInitialized")
    private static Environment storedEnvironment;

    @BeforeClass
    public static void storeEnvironment() {
        storedEnvironment = Environment.getInstance().createCopy();
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    @AfterClass
    public static void restoreEnvironment() {
        Environment.getInstance().restoreFrom(storedEnvironment);
    }

    /* Environment protection END */

    @Before
    public void setUp() {
        environment = Environment.getInstance();
    }

    @After
    public void cleanUp() throws NoSuchFieldException, IllegalAccessException {
        Environment.getInstance().reset();
    }

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(Environment.class));
    }

    @Test
    public void tell_when_not_running_under_AppEngine() {
        // Tests are not run by AppEngine by default.
        assertFalse(environment.isAppEngine());
    }

    @Test
    public void obtain_AppEngine_version_as_optional_string() {
        // By default we're not running under AppEngine.
        assertFalse(environment.appEngineVersion()
                               .isPresent());
    }

    @Test
    public void tell_that_we_are_under_tests_if_env_var_set_to_true() throws Exception {
        Environment.getInstance().setToTests();

        assertTrue(environment.isTests());
    }

    @Test
    public void tell_that_we_are_under_tests_if_env_var_set_to_1() throws Exception {
        System.setProperty(Environment.ENV_KEY_TESTS, "1");

        assertTrue(environment.isTests());
    }

    @Test
    public void tell_that_we_are_under_tests_if_run_under_known_framework() {
        // As we run this from under JUnit...
        assertTrue(environment.isTests());
    }

    @Test
    public void tell_that_we_are_not_under_tests_if_env_set_to_something_else() {
        System.setProperty(Environment.ENV_KEY_TESTS, "neitherTrueNor1");

        assertFalse(environment.isTests());
    }

    @Test
    public void turn_into_tests_mode() {
        environment.setToTests();

        assertTrue(environment.isTests());
    }

    @Test
    public void turn_into_production_mode() {
        environment.setToProduction();

        assertFalse(environment.isTests());
    }

    @Test
    public void clear_environment_var_on_reset() {
        environment.reset();

        assertNull(System.getProperty(Environment.ENV_KEY_TESTS));
    }
}
