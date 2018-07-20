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

package io.spine.server.model;

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The checker of a {@link Method} access level.
 *
 * <p>If the access level check fails, the {@linkplain Logger#warn(String) warning} will be output
 * to the log. If the check passes, no action is performed.
 *
 * <p>This class is effectively {@code final} since it has a single {@code private} constructor.
 * Though the modifier "{@code final}" is absent to make it possible to create mocks for testing.
 *
 * @author Dmytro Kuzmin
 */
@Internal
public class MethodAccessChecker {

    private final Method method;

    private MethodAccessChecker(Method method) {
        this.method = method;
    }

    /**
     * Creates a new instance of the {@link MethodAccessChecker} for the specified {@link Method}.
     *
     * @param method the method to create new instance for
     * @return a new instance of {@code MethodAccessChecker}
     */
    public static MethodAccessChecker forMethod(Method method) {
        checkNotNull(method);
        return new MethodAccessChecker(method);
    }

    /**
     * Checks that method access is {@code public}.
     *
     * <p>If the access is not {@code public}, {@linkplain Logger#warn(String) prints} the warning
     * to the log with the specified {@code warningMessageFormat}.
     *
     * <p>{@code warningMessageFormat} should contain a placeholder for the method name.
     *
     * @param warningMessageFormat a formatted {@code String} representing the warning message
     * @see String#format(String, Object...)
     */
    public void checkPublic(String warningMessageFormat) {
        checkNotNull(warningMessageFormat);
        if (!Modifier.isPublic(method.getModifiers())) {
            warnOnWrongModifier(warningMessageFormat);
        }
    }

    /**
     * Checks that method access is {@code package-private}.
     *
     * <p>If the access is not {@code package-private}, {@linkplain Logger#warn(String) prints}
     * the warning to the log with the specified {@code warningMessageFormat}.
     *
     * <p>{@code warningMessageFormat} should contain a placeholder for the method name.
     *
     * @param warningMessageFormat a formatted {@code String} representing the warning message
     * @see String#format(String, Object...)
     */
    public void checkPackagePrivate(String warningMessageFormat) {
        checkNotNull(warningMessageFormat);
        if (!isPackagePrivate(method)) {
            warnOnWrongModifier(warningMessageFormat);
        }
    }

    /**
     * Checks that method access is {@code private}.
     *
     * <p>If the access is not {@code private}, {@linkplain Logger#warn(String) prints} the warning
     * to the log with the specified {@code warningMessageFormat}.
     *
     * <p>{@code warningMessageFormat} should contain a placeholder for the method name.
     *
     * @param warningMessageFormat a formatted {@code String} representing the warning message
     * @see String#format(String, Object...)
     */
    public void checkPrivate(String warningMessageFormat) {
        checkNotNull(warningMessageFormat);
        if (!Modifier.isPrivate(method.getModifiers())) {
            warnOnWrongModifier(warningMessageFormat);
        }
    }

    /**
     * Logs a message at the WARN level according to the specified format.
     *
     * <p>{@code messageFormat} should contain a placeholder for the method name.
     *
     * @param messageFormat formatted {@code String} representing the warning message
     * @see String#format(String, Object...)
     */
    @VisibleForTesting
    void warnOnWrongModifier(String messageFormat) {
        String methodFullName = method.getDeclaringClass()
                                      .getName() + '.' + method.getName() + "()";
        log().warn(messageFormat, methodFullName);
    }

    /**
     * Checks that the specified {@link Method} has the {@code package-private} access.
     *
     * @param method the method to check
     * @return {@code true} if the method has the {@code package-private} access, {@code false}
     *         otherwise
     */
    private static boolean isPackagePrivate(Method method) {
        int modifiers = method.getModifiers();
        boolean result = !(Modifier.isPublic(modifiers)
                         || Modifier.isProtected(modifiers)
                         || Modifier.isPrivate(modifiers));
        return result;
    }

    /** The logger used by the MethodAccessChecker class. */
    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(HandlerMethod.class);
    }
}
