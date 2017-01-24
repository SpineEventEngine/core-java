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

package org.spine3.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;
import org.spine3.util.Exceptions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.primitives.Primitives.allPrimitiveTypes;

/**
 * Serves as a helper to ensure that none of the methods of the target utility
 * class accept {@code null}s as argument values.
 *
 * <p> The helper checks the methods with access modifiers:
 * <ul>
 *     <li> the {@code public};
 *     <li> the {@code protected};
 *     <li> the {@code default}.
 * </ul>
 *
 * <p> The helper does not checks the methods:
 * <ul>
 *     <li> with the {@code private} modifier;
 *     <li> without the {@code static} modifier;
 *     <li> with only the primitive parameters.
 * </ul>
 *
 * <p> The examples of the methods which will be checked:
 * <ul>
 *     <li> public static void method(Object obj);
 *     <li> protected static void method(Object first, long second);
 *     <li> static void method(Object first, Object second).
 * </ul>
 *
 * <p> The examples of the methods which will be ignored:
 * <ul>
 *     <li> public void method(Object obj);
 *     <li> private static void method(Object obj);
 *     <li> protected static void method(int first, float second).
 * </ul>
 *
 * @author Illia Shepilov
 */
public class NullToleranceTest {

    private final Class targetClass;
    private final Set<String> excludedMethods;
    private final Map<?, ?> defaultValuesMap;

    private NullToleranceTest(Builder builder) {
        this.targetClass = builder.targetClass;
        this.excludedMethods = builder.excludedMethods;
        this.defaultValuesMap = builder.defaultValues;
    }

    /**
     * Checks the all non-private methods in the {@code targetClass}.
     *
     * <p> Check is successful if each of the non-primitive method parameters is ensured to be non-null.
     *
     * @return {@code true} if all methods have not-null check
     * for the input reference type parameters, {@code false} otherwise
     */
    public boolean check() {
        final Method[] accessibleMethods = getAccessibleMethods();
        for (Method method : accessibleMethods) {
            final Class[] parameterTypes = method.getParameterTypes();
            final String methodName = method.getName();
            final boolean excluded = excludedMethods.contains(methodName);
            final boolean arePrimitives = allPrimitiveTypes().containsAll(Arrays.asList(parameterTypes));
            final boolean skipIteration = excluded || parameterTypes.length == 0 || arePrimitives;
            if (skipIteration) {
                continue;
            }

            final Object[] parameterValues = getParameterValues(parameterTypes);

            final boolean correct = invokeAndCheck(method, parameterValues, parameterTypes);
            if (!correct) {
                return false;
            }

        }
        return true;
    }

    private Method[] getAccessibleMethods() {
        final Method[] declaredMethods = targetClass.getDeclaredMethods();
        final List<Method> methodList = newLinkedList();

        for (Method method : declaredMethods) {
            final Invokable<?, Object> invokable = Invokable.from(method);
            final boolean privateMethod = invokable.isPrivate();
            final boolean staticMethod = invokable.isStatic();
            if (!privateMethod && staticMethod) {
                methodList.add(method);
            }
        }

        final Method[] result = methodList.toArray(new Method[methodList.size()]);
        return result;
    }

    private Object[] getParameterValues(Class[] parameterTypes) {
        final Object[] parameterValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            final Class type = parameterTypes[i];
            parameterValues[i] = defaultValuesMap.get(type);
        }
        return parameterValues;
    }

    private boolean invokeAndCheck(Method method, Object[] parameterValues, Class[] parameterTypes) {
        for (int i = 0; i < parameterValues.length; i++) {
            Object[] copiedParametersArray = Arrays.copyOf(parameterValues, parameterValues.length);
            final boolean primitive = TypeToken.of(parameterTypes[i])
                                               .isPrimitive();
            if (!primitive) {
                copiedParametersArray[i] = null;
            }

            final boolean correct = invokeAndCheck(method, copiedParametersArray);

            if (!correct) {
                return false;
            }
        }
        return true;
    }

    private boolean invokeAndCheck(Method method, Object[] params) {
        try {
            method.invoke(null, params);
        } catch (InvocationTargetException ex) {
            boolean valid = validateException(method.getName(), ex);
            return valid;
        } catch (IllegalAccessException e) {
            throw Exceptions.wrappedCause(e);
        }
        return false;
    }

    private boolean validateException(String methodName, InvocationTargetException ex) {
        final Throwable cause = ex.getCause();
        checkException(cause);
        final boolean result = isExpectedStackTraceElements(methodName, cause);
        return result;
    }

    private void checkException(Throwable cause) {
        final boolean correctException = cause instanceof NullPointerException;
        if (!correctException) {
            throw Exceptions.wrappedCause(cause);
        }
    }

    @VisibleForTesting
    Class getTargetClass() {
        return targetClass;
    }

    @VisibleForTesting
    public Set<String> getExcludedMethods() {
        return excludedMethods;
    }

    @VisibleForTesting
    public Map<?, ?> getDefaultValuesMap() {
        return defaultValuesMap;
    }

    /**
     * Checks the stack trace elements.
     *
     * <p> According to the business rules the not-null check should to be in the each utility method.
     * So the usage of the {@link Preconditions} should allocated there.
     *
     * <p> The first {@code StackTraceElement} have to contains
     * the information about the {@code Preconditions} method.
     *
     * <p> The second {@code StackTraceElement} should contains information
     * about the method from the {@code targetClass}.
     *
     * @param methodName the name of the invokable method
     * @param cause      the {@code Throwable}
     * @return {@code true} if the {@code StackTraceElement}s matches the expected, {@code false} otherwise
     */
    private boolean isExpectedStackTraceElements(String methodName, Throwable cause) {
        final StackTraceElement[] stackTraceElements = cause.getStackTrace();
        final StackTraceElement preconditionsElement = stackTraceElements[0];
        final boolean preconditionClass = Preconditions.class.getName()
                                                             .equals(preconditionsElement.getClassName());
        if (!preconditionClass) {
            return false;
        }

        final StackTraceElement expectedUtilClassElement = stackTraceElements[1];
        final boolean correct = isCorrectMethodName(methodName, expectedUtilClassElement);
        if (!correct) {
            return false;
        }

        final boolean correctClass = targetClass.getName()
                                                .equals(expectedUtilClassElement.getClassName());
        return correctClass;
    }

    private boolean isCorrectMethodName(String methodName, StackTraceElement expectedUtilClassElement) {
        final boolean correct = methodName.equals(expectedUtilClassElement.getMethodName());
        return correct;
    }

    /**
     * Creates a new builder for the {@code NullToleranceTest}.
     *
     * @return the {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for producing the {@link NullToleranceTest} instance.
     */
    public static class Builder {

        private Class targetClass;
        private Set<String> excludedMethods;
        private Map<? super Class, ? super Object> defaultValues;

        private Builder() {
            defaultValues = newHashMap();
            excludedMethods = newHashSet();
        }

        /**
         * Sets the utility class.
         *
         * @param utilClass the utility {@link Class}
         * @return the {@code Builder}
         */
        public Builder setClass(Class utilClass) {
            this.targetClass = checkNotNull(utilClass);
            return this;
        }

        /**
         * Adds the method name which will be excluded during the check.
         *
         * @param methodName the name of the excluded method
         * @return the {@code Builder}
         */
        public Builder excludeMethod(String methodName) {
            excludedMethods.add(methodName);
            return this;
        }

        /**
         * Adds the default value for the class which will be used during the the check.
         *
         * @param clazz the class for which will be added default value
         * @param value the default value for the class
         * @return the {@code Builder}
         */
        public <I> Builder addDefaultValue(Class<I> clazz, I value) {
            defaultValues.put(clazz, value);
            return this;
        }

        /**
         * Returns the target class.
         *
         * @return the {@link Class}
         */
        @VisibleForTesting
        Class getTargetClass() {
            return targetClass;
        }

        /**
         * Return the {@code Set} of the excluded method name.
         *
         * @return the {@code Set}
         */
        @VisibleForTesting
        Set<String> getExcludedMethods() {
            return excludedMethods;
        }

        /**
         * Return the {@code Map} of the default values for the classes.
         *
         * @return the {@code Map}
         */
        @VisibleForTesting
        Map<? super Class, ? super Object> getDefaultValues() {
            return defaultValues;
        }

        /**
         * Returns the constructed the {@link NullToleranceTest}.
         *
         * @return the {@code nullToleranceTest} instance.
         */
        public NullToleranceTest build() {
            checkNotNull(targetClass);
            final NullToleranceTest result = new NullToleranceTest(this);
            return result;
        }
    }
}
