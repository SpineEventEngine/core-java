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

package io.spine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import io.spine.annotation.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.SubstituteLogger;

import javax.annotation.Nullable;

/**
 * Provides information about the environment (current platform used, etc).
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SPI
@SuppressWarnings("AccessOfSystemProperties") // OK as we need system properties for this class.
public final class Environment {

    /**
     * The key name of the system property which tells if a code runs under a testing framework.
     *
     * <p>If your testing framework is not among supported by {@link #isTests()},
     * set this property to {@code true} before running tests.
     */
    public static final String ENV_KEY_TESTS = "io.spine.tests";

    private static final String VAL_TRUE = "true";
    private static final String VAL_FALSE = "false";

    /** If set, tells if the code runs from a testing framework. */
    @Nullable
    private Boolean tests;

    private Environment() {
        // Prevent instantiation of this singleton class from outside.
    }

    private Environment(Environment copy) {
        this.tests = copy.tests;
    }

    /** Returns the singleton instance. */
    public static Environment getInstance() {
        return Singleton.INSTANCE.value;
    }

    /**
     * Creates a copy of the instance so that it can be later
     * restored via {@link #restoreFrom(Environment)} by cleanup in tests.
     */
    @VisibleForTesting
    public Environment createCopy() {
        return new Environment(this);
    }

    /**
     * Restores the state from the instance created by {@link #createCopy()}.
     *
     * <p>Call this method when cleaning up tests that modify {@code Environment}.
     */
    @VisibleForTesting
    public void restoreFrom(Environment copy) {
        // Make sure this matches the set of fields copied in the copy constructor.
        this.tests = copy.tests;
    }

    /**
     * Verifies if the code currently runs under a unit testing framework.
     *
     * <p>The method returns {@code true} if the following packages are discovered
     * in the stacktrace:
     * <ul>
     *     <li>{@code org.junit}
     *     <li>{@code org.testng}
     * </ul>
     *
     * @return {@code true} if the code runs under a testing framework, {@code false} otherwise
     */
    @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern") // OK as we cache the result
    public boolean isTests() {
        // If we cached the value before, return it.
        if (tests != null) {
            return tests;
        }

        // Check the environment variable. We may run under unknown testing framework or
        // tests may require production-like mode, which they simulate by setting
        // the property to `false`.
        String testProp = System.getProperty(ENV_KEY_TESTS);
        if (testProp != null) {
            testProp = testProp.replaceAll("\"' ", "");
            this.tests = (VAL_TRUE.equalsIgnoreCase(testProp)
                                                || "1".equals(testProp));
            return this.tests;
        }

        // Check stacktrace for known frameworks.
        final String stacktrace = Throwables.getStackTraceAsString(new RuntimeException(""));
        if (stacktrace.contains("org.junit")
                || stacktrace.contains("org.testng")) {
            this.tests = true;
            return true;
        }

        this.tests = false;
        return false;
    }

    /**
     * Verifies if the code runs in the production mode.
     *
     * <p>This method is opposite to {@link #isTests()}
     *
     * @return {@code true} if the code runs in the production mode, {@code false} otherwise
     */
    public boolean isProduction() {
        return !isTests();
    }

    /**
     * Turns the test mode on.
     *
     * <p>This method is opposite to {@link #setToProduction()}.
     */
    @VisibleForTesting
    public void setToTests() {
        this.tests = true;
        System.setProperty(ENV_KEY_TESTS, VAL_TRUE);
    }

    /**
     * Turns the production mode on.
     *
     * <p>This method is opposite to {@link #setToTests()}.
     */
    @VisibleForTesting
    public void setToProduction() {
        this.tests = false;
        System.setProperty(ENV_KEY_TESTS, VAL_FALSE);
    }

    /**
     * Resets the instance and clears the {@link #ENV_KEY_TESTS} variable.
     */
    @VisibleForTesting
    public void reset() {
        this.tests = null;
        System.clearProperty(ENV_KEY_TESTS);
    }

    /**
     * Obtains a logger for the passed class depending on the state of the environment.
     *
     * <p>In {@linkplain #isTests() tests mode}, the returned logger is an instance of
     * {@link org.slf4j.helpers.SubstituteLogger SubstituteLogger} delegating to a logger obtained
     * from the {@link LoggerFactory#getLogger(Class) LoggerFactory}.
     *
     * <p>In {@linkplain #isProduction() production mode}, returns the instance obtained from
     * the {@link LoggerFactory#getLogger(Class) LoggerFactory}.
     *
     * @param cls the class for which to create the logger
     * @return the logger instance
     */
    public Logger getLogger(Class<?> cls) {
        final Logger logger = LoggerFactory.getLogger(cls);
        if (isTests()) {
            final SubstituteLogger substLogger = new SubstituteLogger(cls.getName(), null, true);
            substLogger.setDelegate(logger);
            return substLogger;
        } else {
            return logger;
        }
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Environment value = new Environment();
    }
}
