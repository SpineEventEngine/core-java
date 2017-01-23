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

import org.spine3.util.Exceptions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * @author Illia Shepilov
 */
public class NullToleranceTest {

    private static final String CHECK_NOT_NULL_METHOD = "checkNotNull";
    private final Class utilClass;
    private final Set<String> excludedMethods;
    private final Map defaultValuesMap;
    private final Map<Class, Object> primitiveValues;

    private NullToleranceTest(Builder builder) {
        this.utilClass = builder.utilClass;
        this.excludedMethods = builder.excludedMethods;
        this.defaultValuesMap = builder.defaultValues;
        primitiveValues = newHashMap();
        initializePrimitiveValuesMap();
    }

    public boolean check() {
        final Method[] methods = utilClass.getDeclaredMethods();
        for (Method method : methods) {
            final String methodName = method.getName();
            final boolean isExcluded = excludedMethods.contains(methodName);
            final Class[] parameterTypes = method.getParameterTypes();
            final Object[] parameterValues = new Object[parameterTypes.length];
            if (isExcluded || parameterTypes.length == 0) {
                continue;
            }

            wrapPrimitives(parameterTypes, parameterValues);
            setDefaultValues(parameterTypes, parameterValues);

            for (int i = 0; i < parameterValues.length; i++) {
                Object[] copiedArray = Arrays.copyOf(parameterValues, parameterValues.length);
                final boolean isPrimitive = primitiveValues.keySet()
                                                           .contains(parameterTypes[i]);
                if (!isPrimitive) {
                    copiedArray[i] = null;
                }

                final boolean isPrimitives = primitiveValues.keySet()
                                                            .containsAll(Arrays.asList(parameterTypes));
                final boolean isCorrect = invokeAndCheck(method, copiedArray, isPrimitives);

                if (!isCorrect) {
                    return false;
                }
            }

        }
        return true;
    }

    private void setDefaultValues(Class[] parameterTypes, Object[] parameterValues) {
        for (int i = 0; i < parameterTypes.length; i++) {
            final Object defaultValue = defaultValuesMap.get(parameterTypes[i]);
            if (defaultValue != null) {
                parameterValues[i] = defaultValue;
                return;
            }
        }
    }

    private void wrapPrimitives(Class[] parameterTypes, Object[] parameterValues) {
        for (int i = 0; i < parameterTypes.length; i++) {
            final Object wrappedPrimitive = primitiveValues.get(parameterTypes[i]);
            if (wrappedPrimitive != null) {
                parameterValues[i] = wrappedPrimitive;
            }
        }
    }

    private boolean invokeAndCheck(Method method, Object[] params, boolean isPrimitives) {
        try {
            method.invoke(null, params);
            if (isPrimitives) {
                return true;
            }
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
        final boolean isCorrectEx = isCorrectException(cause);
        if (!isCorrectEx) {
            return true;
        }

        final boolean isCorrect = isCorrectStackTraceElements(methodName, cause);
        return isCorrect;
    }

    private boolean isCorrectException(Throwable cause) {
        final boolean isCorrectException = cause instanceof NullPointerException;
        return isCorrectException;
    }

    private boolean isCorrectStackTraceElements(String methodName, Throwable cause) {
        final StackTraceElement[] stackTraceElements = cause.getStackTrace();
        final StackTraceElement preconditionsElement = stackTraceElements[0];
        final boolean isPreconditionsMethod = CHECK_NOT_NULL_METHOD.equals(preconditionsElement.getMethodName());
        if (!isPreconditionsMethod) {
            return false;
        }

        final StackTraceElement expectedUtilClassElement = stackTraceElements[1];
        final boolean isCorrectMethodName = methodName.equals(expectedUtilClassElement.getMethodName());
        if (!isCorrectMethodName) {
            return false;
        }

        final boolean isCorrectClassName = utilClass.getName()
                                                    .equals(expectedUtilClassElement.getClassName());
        return isCorrectClassName;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private void initializePrimitiveValuesMap() {
        primitiveValues.put(boolean.class, false);
        primitiveValues.put(byte.class, (byte) 0);
        primitiveValues.put(short.class, (short) 0);
        primitiveValues.put(int.class, 0);
        primitiveValues.put(long.class, 0L);
        primitiveValues.put(char.class, '\u0000');
        primitiveValues.put(float.class, 0.0f);
        primitiveValues.put(double.class, 0.0d);
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
