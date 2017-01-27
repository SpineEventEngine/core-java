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
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.util.Exceptions;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Defaults.defaultValue;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.primitives.Primitives.allPrimitiveTypes;
import static com.google.common.primitives.Primitives.isWrapperType;
import static com.google.common.primitives.Primitives.unwrap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static javax.lang.model.SourceVersion.isName;

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
 * <p> The helper does not check the methods:
 * <ul>
 *     <li> with the {@code private} modifier;
 *     <li> without the {@code static} modifier;
 *     <li> with only the primitive parameters;
 *     <li> if all the parameters are marked as {@code Nullable}.
 * </ul>
 *
 * <p> The examples of the methods which will be checked:
 * <ul>
 *     <li> public static void method(Object obj);
 *     <li> protected static void method(Object first, long second);
 *     <li> public static void method(@Nullable Object first, Object second);
 *     <li> static void method(Object first, Object second).
 * </ul>
 *
 * <p> The examples of the methods which will be ignored:
 * <ul>
 *     <li> public void method(Object obj);
 *     <li> private static void method(Object obj);
 *     <li> public static void method(@Nullable Object obj);
 *     <li> protected static void method(int first, float second).
 * </ul>
 *
 * @author Illia Shepilov
 */
public class NullToleranceTest {

    private final Class targetClass;
    private final Set<String> excludedMethods;
    private final Map<?, ?> defaultValues;

    private NullToleranceTest(Builder builder) {
        this.targetClass = builder.targetClass;
        this.excludedMethods = builder.excludedMethods;
        this.defaultValues = builder.defaultValues;
    }

    /**
     * Checks the all non-private methods in the {@code targetClass}.
     *
     * <p> Check is successful if each of the non-primitive method parameters is ensured to be non-null.
     *
     * @return {@code true} if all methods have non-null check
     * for the input reference type parameters, {@code false} otherwise
     */
    public boolean check() {
        final DefaultValuesProvider valuesProvider = new DefaultValuesProvider(defaultValues);
        final Method[] accessibleMethods = getAccessibleMethods(targetClass);
        final String targetClassName = targetClass.getName();
        for (Method method : accessibleMethods) {
            final Class[] parameterTypes = method.getParameterTypes();
            final String methodName = method.getName();
            final boolean excluded = excludedMethods.contains(methodName);
            final boolean primitivesOnly = allPrimitiveTypes().containsAll(Arrays.asList(parameterTypes));
            final boolean skipMethod = excluded || parameterTypes.length == 0 || primitivesOnly;
            if (skipMethod) {
                continue;
            }

            final MethodChecker methodChecker = new MethodChecker(method, targetClassName, valuesProvider);
            final boolean passed = methodChecker.check();
            if (!passed) {
                return false;
            }

        }
        return true;
    }

    /**
     * Returns the array of the declared {@code Method}s
     * in the {@code Class} which are static and non-private.
     *
     * @param targetClass the target class
     * @return the array of the {@code Method}
     */
    private static Method[] getAccessibleMethods(Class targetClass) {
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

    /**
     * Creates a new builder for the {@code NullToleranceTest}.
     *
     * @return the {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @VisibleForTesting
    Class getTargetClass() {
        return targetClass;
    }

    @VisibleForTesting
    Set<String> getExcludedMethods() {
        return unmodifiableSet(excludedMethods);
    }

    @VisibleForTesting
    Map<?, ?> getDefaultValues() {
        return unmodifiableMap(defaultValues);
    }

    /**
     * Serves as a helper to ensure that method does not accept {@code null}s as argument values.
     */
    private static class MethodChecker {

        private final Method method;
        private final String targetClassName;
        private final DefaultValuesProvider valuesProvider;

        private MethodChecker(Method method, String targetClassName, DefaultValuesProvider valuesProvider) {
            this.method = method;
            this.targetClassName = targetClassName;
            this.valuesProvider = valuesProvider;
        }

        private boolean check() {
            final boolean nullableOnly = hasOnlyNullableArgs();
            if (nullableOnly) {
                return true;
            }
            final Class[] parameterTypes = method.getParameterTypes();
            final Object[] parameterValues = getParameterValues(parameterTypes);
            final ImmutableList<Parameter> parameters = Invokable.from(method)
                                                                 .getParameters();
            for (int i = 0; i < parameterValues.length; i++) {
                Object[] copiedParametersArray = Arrays.copyOf(parameterValues, parameterValues.length);
                final boolean primitive = TypeToken.of(parameterTypes[i])
                                                   .isPrimitive();

                final boolean nullableParameter = parameters.get(i)
                                                            .isAnnotationPresent(Nullable.class);
                if (primitive || nullableParameter) {
                    continue;
                }

                copiedParametersArray[i] = null;
                final boolean correct = invokeAndCheck(copiedParametersArray);

                if (!correct) {
                    return false;
                }
            }
            return true;
        }

        private boolean hasOnlyNullableArgs() {
            final ImmutableList<Parameter> parameters = Invokable.from(method)
                                                                 .getParameters();
            for (Parameter parameter : parameters) {
                final boolean present = parameter.isAnnotationPresent(Nullable.class);
                if (!present) {
                    return false;
                }
            }
            return true;
        }

        private Object[] getParameterValues(Class[] parameterTypes) {
            final Object[] parameterValues = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                final Class type = parameterTypes[i];
                final Object defaultValue = valuesProvider.getDefaultValue(type);
                parameterValues[i] = defaultValue;
            }
            return parameterValues;
        }

        private boolean invokeAndCheck(Object[] params) {
            try {
                method.setAccessible(true);
                method.invoke(null, params);
            } catch (InvocationTargetException ex) {
                boolean valid = validateException(ex);
                return valid;
            } catch (IllegalAccessException e) {
                throw Exceptions.wrappedCause(e);
            }
            return false;
        }

        private boolean validateException(InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            checkException(cause);
            final boolean result = hasExpectedStackTrace(cause);
            return result;
        }

        private static void checkException(Throwable cause) {
            final boolean correctException = cause instanceof NullPointerException;
            if (!correctException) {
                throw Exceptions.wrappedCause(cause);
            }
        }

        /**
         * Checks the stack trace elements.
         *
         * <p> It is expected that each of the tested utility methods invokes
         * {@link Preconditions#checkNotNull(Object)} as a first step of the execution.
         *
         * <p> Therefore the stack trace is analysed to ensure its first element references
         * the {@code Preconditions#checkNotNull(Object)} and the second one references the tested method.
         *
         * @param cause the {@code Throwable}
         * @return {@code true} if the {@code StackTraceElement}s matches the expected, {@code false} otherwise
         */
        private boolean hasExpectedStackTrace(Throwable cause) {
            final StackTraceElement[] stackTraceElements = cause.getStackTrace();
            final StackTraceElement preconditionsElement = stackTraceElements[0];
            final boolean preconditionClass = Preconditions.class.getName()
                                                                 .equals(preconditionsElement.getClassName());
            if (!preconditionClass) {
                return false;
            }

            final StackTraceElement targetClassElement = stackTraceElements[1];
            final String targetMethodName = targetClassElement.getMethodName();
            final boolean correct = method.getName()
                                          .equals(targetMethodName);
            if (!correct) {
                return false;
            }

            final boolean correctClass = targetClassName.equals(targetClassElement.getClassName());
            return correctClass;
        }
    }

    /**
     * Provides the default values for the method arguments.
     */
    private static class DefaultValuesProvider {

        private static final Class[] EMPTY_PARAMETER_TYPES = {};
        private static final Object[] EMPTY_ARGUMENTS = {};
        private static final String METHOD_NAME = "getDefaultInstance";
        private final Map<?, ?> defaultValues;

        private DefaultValuesProvider(Map<?, ?> defaultValues) {
            this.defaultValues = defaultValues;
        }

        /**
         * Returns the default value for the method argument by the method argument type.
         * If default value does not provide, throws {@link IllegalStateException} otherwise.
         *
         * @param type the {@code Class} of the method argument
         * @return the default value
         */
        private Object getDefaultValue(Class<?> type) {
            Object result = defaultValues.get(type);
            if (result != null) {
                return result;
            }

            result = getChildDefaultValue(type);

            final boolean primitive = allPrimitiveTypes().contains(type);
            if (result == null && primitive) {
                result = defaultValue(type);
            }

            final boolean wrapper = isWrapperType(type);
            if (result == null && wrapper) {
                final Class<?> unwrappedPrimitive = unwrap(type);
                result = defaultValue(unwrappedPrimitive);
            }

            final Class<Message> messageClass = Message.class;
            final boolean message = messageClass.isAssignableFrom(type);
            if (result == null && message) {
                result = getDefaultMessageInstance(type);
            }

            checkState(result != null);
            return result;
        }

        @Nullable
        private Object getChildDefaultValue(Class<?> type) {
            for (Object clazz : defaultValues.keySet()) {
                final boolean passedToMap = type.isAssignableFrom(((Class<?>) clazz));
                if (passedToMap) {
                    final Object result = defaultValues.get(clazz);
                    return result;
                }
            }
            return null;
        }

        private static Object getDefaultMessageInstance(Class<?> type) {
            try {
                if (type.equals(Message.class)) {
                    return Any.getDefaultInstance();
                }
                final Method method = type.getMethod(METHOD_NAME, EMPTY_PARAMETER_TYPES);
                final Object defaultInstance = method.invoke(null, EMPTY_ARGUMENTS);
                return defaultInstance;
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw Exceptions.wrappedCause(e);
            }
        }
    }

    /**
     * A builder for producing the {@link NullToleranceTest} instance.
     */
    public static class Builder {

        private static final String STRING_DEFAULT_VALUE = "";
        private final Set<String> excludedMethods;
        private final Map<? super Class<?>, ? super Object> defaultValues;
        private Class<?> targetClass;

        private Builder() {
            defaultValues = newHashMap();
            excludedMethods = newHashSet();
        }

        /**
         * Sets the target class.
         *
         * @param utilClass the utility {@link Class}
         * @return the {@code Builder}
         */
        public Builder setClass(Class utilClass) {
            this.targetClass = checkNotNull(utilClass);
            return this;
        }

        /**
         * Adds the method name which will be excluded from the check.
         *
         * @param methodName the name of the method to exclude
         * @return the {@code Builder}
         */
        @SuppressWarnings("WeakerAccess") // it is a public API.
        public Builder excludeMethod(String methodName) {
            checkNotNull(methodName);
            final boolean validName = isName(methodName);
            checkArgument(validName);

            excludedMethods.add(methodName);
            return this;
        }

        /**
         * Adds the default value for the method argument.
         *
         * <p> During the checks, each applicable method of the target class is executed
         * with the {@code null} values passed as arguments.This is done in an iterative fashion,
         * so that each invocation checks the {@code null} tolerance for the only one of the parameters.
         * The rest of the method arguments are set with the default values.
         *
         * <p> If the default value for the type is not customized,
         * the predefined list of the default values per type will be used:
         *
         * <p> The list of the classes and their default values:
         * <ul>
         *     <li> for the {@code String} is used empty string, as the default value;
         *     <li> for the primitive and wrapper types is used the default value
         *     returned by the {@link com.google.common.base.Defaults#defaultValue(Class)}
         *     </li>
         *     <li> for the {@code Class<? extends Message>} instances the default value
         *     will be provided by the {@code #getDefaultInstance} method.
         *     </li>
         * </ul>
         *
         * <p><b>Example.</b>
         *
         * <p>The method declared as
         *
         * <pre> {@code public static void doSomething(PersonName messageValue,
         *                                             Integer wrapperValue,
         *                                             CustomType objectValue)} </pre>
         *
         * <p> will be invoked three times:
         *
         * <ol>
         *     <li> {@code doSomething(null, 0, <default value for CustomType>)},
         *     <li> {@code doSomething(PersonName.getDefaultInstance(), null , <default value for CustomType>)},
         *     <li> {@code doSomething(PersonName.getDefaultInstance(), 0 , null)}.
         * </ol>
         *
         * <p> If the default value for the {@code CustomType} is not provided,
         * an {@code IllegalStateException} is thrown.
         *
         * @param value the default value for the class
         * @return the {@code Builder}
         */
        @SuppressWarnings("WeakerAccess") // it is a public API.
        public <I> Builder addDefaultValue(I value) {
            checkNotNull(value);
            defaultValues.put(value.getClass(), value);
            return this;
        }

        @VisibleForTesting
        Class<?> getTargetClass() {
            return targetClass;
        }

        @VisibleForTesting
        Set<String> getExcludedMethods() {
            return unmodifiableSet(excludedMethods);
        }

        @VisibleForTesting
        Map<?, ?> getDefaultValues() {
            return unmodifiableMap(defaultValues);
        }

        /**
         * Returns the constructed {@link NullToleranceTest}.
         *
         * @return the {@code nullToleranceTest} instance.
         */
        public NullToleranceTest build() {
            checkNotNull(targetClass);
            addDefaultStringValueIfNeeded();
            final NullToleranceTest result = new NullToleranceTest(this);
            return result;
        }

        private void addDefaultStringValueIfNeeded() {
            for (Object clazz : defaultValues.keySet()) {
                final boolean stringClass = String.class.isAssignableFrom((Class) clazz);
                if (stringClass) {
                    return;
                }
            }
            defaultValues.put(String.class, STRING_DEFAULT_VALUE);
        }
    }
}
