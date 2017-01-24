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

import com.google.common.base.Preconditions;
import org.spine3.util.Exceptions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Provides the method to check the utility classes for the not-null check
 * the non-primitive method parameters.
 *
 * @author Illia Shepilov
 */
public class NullToleranceTest {

    private final Class utilClass;
    private final Set<String> excludedMethods;
    private final Map defaultValuesMap;
    private final Set<Class> primitiveValues;

    private NullToleranceTest(Builder builder) {
        this.utilClass = builder.utilClass;
        this.excludedMethods = builder.excludedMethods;
        this.defaultValuesMap = builder.defaultValues;
        primitiveValues = newHashSet();
        initPrimitiveValuesSet();
    }

    /**
     * Checks the all non-private methods in the {@code utilClass}.
     *
     * <p> Check is successful if each the non-primitive method parameter has the not-null check.
     *
     * @return {@code true} if all methods have not-null check
     * for input reference type parameters, {@code false} otherwise
     */
    public boolean check() {
        final Method[] accessibleMethods = getAccessibleMethods();
        for (Method method : accessibleMethods) {
            final Class[] parameterTypes = method.getParameterTypes();
            final String methodName = method.getName();
            final boolean isExcluded = excludedMethods.contains(methodName);
            final boolean arePrimitives = primitiveValues.containsAll(Arrays.asList(parameterTypes));
            final boolean skipIteration = isExcluded || parameterTypes.length == 0 || arePrimitives;
            if (skipIteration) {
                continue;
            }

            final Object[] parameterValues = getParameterValues(parameterTypes);

            final boolean isCorrect = invokeAndCheck(method, parameterValues, parameterTypes);
            if (!isCorrect) {
                return false;
            }

        }
        return true;
    }

    private Method[] getAccessibleMethods() {
        final Method[] declaredMethods = utilClass.getDeclaredMethods();
        final List<Method> methodList = newLinkedList();

        for (Method method : declaredMethods) {
            final boolean isPrivate = Modifier.isPrivate(method.getModifiers());
            if (!isPrivate) {
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
            final boolean isPrimitive = primitiveValues.contains(parameterTypes[i]);
            if (!isPrimitive) {
                copiedParametersArray[i] = null;
            }

            final boolean isCorrect = invokeAndCheck(method, copiedParametersArray);

            if (!isCorrect) {
                return false;
            }
        }
        return true;
    }

    private boolean invokeAndCheck(Method method, Object[] params) {
        try {
            method.invoke(null, params);
        } catch (InvocationTargetException ex) {
            boolean isValid = validateException(method.getName(), ex);
            return isValid;
        } catch (IllegalAccessException e) {
            throw Exceptions.wrappedCause(e);
        }
        return false;
    }

    private boolean validateException(String methodName, InvocationTargetException ex) {
        final Throwable cause = ex.getCause();
        checkException(cause);
        final boolean result = isCorrectStackTraceElements(methodName, cause);
        return result;
    }

    private void checkException(Throwable cause) {
        final boolean isCorrectException = cause instanceof NullPointerException;
        if (!isCorrectException) {
            throw Exceptions.wrappedCause(cause);
        }
    }

    private boolean isCorrectStackTraceElements(String methodName, Throwable cause) {
        final StackTraceElement[] stackTraceElements = cause.getStackTrace();
        final StackTraceElement preconditionsElement = stackTraceElements[0];
        final boolean isPreconditionsClass = Preconditions.class.getName()
                                                                .equals(preconditionsElement.getClassName());
        if (!isPreconditionsClass) {
            return false;
        }

        final StackTraceElement expectedUtilClassElement = stackTraceElements[1];
        final boolean isCorrectMethod = isCorrectMethodName(methodName, expectedUtilClassElement);
        if (!isCorrectMethod) {
            return false;
        }

        final boolean isUtilClass = utilClass.getName()
                                             .equals(expectedUtilClassElement.getClassName());
        return isUtilClass;
    }

    private boolean isCorrectMethodName(String methodName, StackTraceElement expectedUtilClassElement) {
        final boolean isCorrectMethodName = methodName.equals(expectedUtilClassElement.getMethodName());
        return isCorrectMethodName;
    }

    /**
     * Creates a new builder for the {@code NullToleranceTest}.
     *
     * @return the {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    private void initPrimitiveValuesSet() {
        primitiveValues.add(boolean.class);
        primitiveValues.add(byte.class);
        primitiveValues.add(short.class);
        primitiveValues.add(int.class);
        primitiveValues.add(long.class);
        primitiveValues.add(char.class);
        primitiveValues.add(float.class);
        primitiveValues.add(double.class);
    }

    /**
     * A builder for producing the {@link NullToleranceTest} instance.
     */
    public static class Builder {

        private Class utilClass;
        private Set<String> excludedMethods;
        private Map defaultValues;

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
            this.utilClass = utilClass;
            return this;
        }

        /**
         * Adds the method name in the {@code excludedMethods} set.
         *
         * @param methodName the name of the excluded method
         * @return the {@code Builder}
         */
        public Builder excludeMethod(String methodName) {
            excludedMethods.add(methodName);
            return this;
        }

        /**
         * Adds the default value for the class in the {@code defaultValues} map.
         *
         * @param clazz the class for which will be added default value
         * @param value the default value for the class
         * @return the {@code Builder}
         */
        @SuppressWarnings("unchecked") // check on the method level.
        public <I> Builder addDefaultValue(Class<I> clazz, I value) {
            defaultValues.put(clazz, value);
            return this;
        }

        /**
         * Returns the constructed the {@link NullToleranceTest}.
         *
         * @return the {@code nullToleranceTest} instance.
         */
        public NullToleranceTest build() {
            checkNotNull(utilClass);
            final NullToleranceTest result = new NullToleranceTest(this);
            return result;
        }
    }
}
