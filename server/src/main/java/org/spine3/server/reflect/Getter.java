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

package org.spine3.server.reflect;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A representation of a Java-style getter method.
 *
 * <p>Aggregates a {@link Method} and some meta information on it.
 *
 * @author Dmytro Dashenkov.
 */
public class Getter {

    private static final String GETTER_PREFIX_REGEX = "(get)|(is)";
    private static final Pattern GETTER_PREFIX_PATTERN = Pattern.compile(GETTER_PREFIX_REGEX);

    private final String name;
    private final Method method;
    private final String propertyName;

    private boolean initialized;
    private boolean nullable;

    public Getter(Method method) {
        this.method = checkNotNull(method);
        this.name = checkNotNull(method.getName());
        this.propertyName = GETTER_PREFIX_PATTERN.matcher(name)
                                                 .replaceFirst("")
                                                 .toLowerCase();
    }

    /**
     * @return the {@linkplain Method#getName() method name}
     */
    public String getName() {
        return name;
    }

    /**
     * @return the name of the property which is represented by this getter method
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return the {@linkplain Method#getReturnType() return type} of the getter
     * (a.k.a. the type of the property)
     */
    public Class<?> getType() {
        return method.getReturnType();
    }

    /**
     * Invokes the {@link Method} on the given object and returns the result of the invocation.
     *
     * @param object the object to invoke method on
     * @return the result of the invocation
     */
    public Object get(Object object) {
        checkNotNull(object);
        initialize();
        final Object property;
        try {
            property = method.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        if (!nullable) {
            checkNotNull(property,
                         "null value of a non-null property %s.",
                         propertyName);
        }
        return property;
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        nullable = method.isAnnotationPresent(Nullable.class);
        initialized = true;
    }
}
