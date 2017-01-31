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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static com.google.common.base.Defaults.defaultValue;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Queues.newPriorityQueue;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.primitives.Primitives.allPrimitiveTypes;
import static com.google.common.primitives.Primitives.isWrapperType;
import static com.google.common.primitives.Primitives.unwrap;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static javax.lang.model.SourceVersion.isName;

/**
 * Serves as a helper to ensure that none of the methods of the target utility
 * class accept {@code null}s as argument values.
 *
 * <p>The helper checks the methods with access modifiers:
 * <ul>
 *     <li>the {@code public};
 *     <li>the {@code protected};
 *     <li>the {@code default}.
 * </ul>
 *
 * <p>The helper does not check the methods:
 * <ul>
 *     <li>with the {@code private} modifier;
 *     <li>without the {@code static} modifier;
 *     <li>with only the primitive parameters;
 *     <li>if all the parameters are marked as {@code Nullable}.
 * </ul>
 *
 * <p>The examples of the methods which will be checked:
 * <ul>
 *     <li>public static void method(Object obj);
 *     <li>protected static void method(Object first, long second);
 *     <li>public static void method(@Nullable Object first, Object second);
 *     <li>static void method(Object first, Object second).
 * </ul>
 *
 * <p>The examples of the methods which will be ignored:
 * <ul>
 *     <li>public void method(Object obj);
 *     <li>private static void method(Object obj);
 *     <li>public static void method(@Nullable Object obj);
 *     <li>protected static void method(int first, float second).
 * </ul>
 *
 * @author Illia Shepilov
 */
public class NullToleranceTest {

    private final Class targetClass;
    private final Set<String> excludedMethods;
    private final Map<Class<?>, ?> defaultValues;

    private NullToleranceTest(Builder builder) {
        this.targetClass = builder.targetClass;
        this.excludedMethods = builder.excludedMethods;
        this.defaultValues = builder.defaultValues;
    }

    /**
     * Checks the all non-private {@code static} methods in the {@code targetClass}
     * to ensure each of them is not tolerant for {@code null} values passed as arguments.
     *
     * <p>The check is successful if all of the non-primitive method parameters
     * do not accept {@code null}s.
     *
     * @return {@code true} if the check is successful, {@code false} otherwise
     */
    public boolean check() {
        final DefaultValueProvider valuesProvider = new DefaultValueProvider(defaultValues);
        final Iterable<Method> accessibleMethods = getAccessibleMethods(targetClass);
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
     * Returns the {@code Iterable} over all the declared {@code Method}s
     * in the {@code targetClass} which are eligible for testing.
     *
     * @param targetClass the target class
     * @return an iterable over the {@code Method}s
     */
    private static Iterable<Method> getAccessibleMethods(Class targetClass) {
        final Method[] declaredMethods = targetClass.getDeclaredMethods();
        final ImmutableList.Builder<Method> listBuilder = ImmutableList.builder();

        for (Method method : declaredMethods) {
            final Invokable<?, Object> invokable = Invokable.from(method);
            final boolean privateMethod = invokable.isPrivate();
            final boolean staticMethod = invokable.isStatic();
            if (!privateMethod && staticMethod) {
                listBuilder.add(method);
            }
        }

        final ImmutableList<Method> result = listBuilder.build();
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
     * Serves as a helper to ensure that the method does not accept {@code null}s as argument values.
     */
    private static class MethodChecker {

        /**
         * The method to test.
         */
        private final Method method;

        /**
         * The name of the {@code Class} instance, which the tested method belongs to.
         */
        private final String targetClassName;

        /**
         * The pre-configured provider of the default values per type.
         *
         * @see Builder#addDefaultValue(Object)
         */
        private final DefaultValueProvider valuesProvider;

        private MethodChecker(Method method, String targetClassName, DefaultValueProvider valuesProvider) {
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
         * <p>It is expected that each of the tested utility methods invokes
         * {@link Preconditions#checkNotNull(Object)} as a first step of the execution.
         *
         * <p>Therefore the stack trace is analysed to ensure its first element references
         * the {@code Preconditions#checkNotNull(Object)} and the second one references the tested method.
         *
         * @param cause the {@code Throwable} to inspect
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
     * Provides the default values for the method arguments based on the argument type.
     */
    private static class DefaultValueProvider {

        private static final Class[] EMPTY_PARAMETER_TYPES = {};
        private static final Object[] EMPTY_ARGUMENTS = {};
        private static final String METHOD_NAME = "getDefaultInstance";

        private final Map<Class<?>, ?> defaultValues;

        private DefaultValueProvider(Map<Class<?>, ?> defaultValues) {
            this.defaultValues = defaultValues;
        }

        /**
         * Returns the default value for the method argument by the method argument {@code type}.
         *
         * <p>If a default value cannot be provided for the type of interest,
         * an {@link IllegalStateException} is thrown.
         *
         * @param type the {@code Class} of the method argument
         * @return the default value
         */
        private Object getDefaultValue(Class<?> type) {
            Object result = defaultValues.get(type);
            if (result != null) {
                return result;
            }

            result = findDerivedTypeValue(type);

            final boolean primitive = allPrimitiveTypes().contains(type);
            if (result == null && primitive) {
                result = defaultValue(type);
            }

            final boolean wrapper = isWrapperType(type);
            if (result == null && wrapper) {
                final Class<?> unwrappedPrimitive = unwrap(type);
                result = defaultValue(unwrappedPrimitive);
            }

            if (result == null) {
                final Class<Message> messageClass = Message.class;
                final boolean assignableFromMessage = messageClass.isAssignableFrom(type);
                if (assignableFromMessage) {
                    @SuppressWarnings("unchecked")      //It's fine since we checked for the type.
                    final Class<? extends Message> messageType = (Class<? extends Message>) type;
                    result = getDefaultMessageInstance(messageType);
                }
            }

            checkState(result != null);
            return result;
        }

        /**
         * Returns the default value, if its type is derived from the given {@code type}.
         *
         * <p><b>Example:</b>
         *
         * <pre>
         *     {@code
         *     // Given the types
         *     public class Person {...}
         *     public class User extends Person {...}
         *
         *     // and customizing the default value for `User` type,
         *     final User defaultUser = new User();
         *     builder.addDefaultValue(defaultUser);
         *
         *     // an invocation of the method with `User` parent class
         *     findDerivedTypeValue(Person.class);    // will return the `defaultUser` instance.
         *     }
         * </pre>
         *
         * <p>In case there are several suitable value, the first one is used.
         *
         * @param type the parent type to look the default value for
         * @return the default value, or {@code null} if no such value can be found
         */
        @Nullable
        private <T> T findDerivedTypeValue(Class<T> type) {
            for (Class clazz : defaultValues.keySet()) {
                final boolean defaultValuePresent = type.isAssignableFrom(clazz);
                if (defaultValuePresent) {
                    @SuppressWarnings("unchecked")      // It's OK, since we check for the type compliance above.
                    final T result = (T) defaultValues.get(clazz);
                    return result;
                }
            }
            return null;
        }

        /**
         * Obtains the default value for the given {@code Message} {@code type}.
         *
         * <p>The default instance of {@code Any} is used {@code type == Message.class}.
         *
         * @param type the type of {@code Message} to obtain a default value for
         * @return the default instance of the given {@code Message}
         */
        private static Message getDefaultMessageInstance(Class<? extends Message> type) {
            if (type.equals(Message.class)) {
                return Any.getDefaultInstance();
            }

            try {
                final Method method = type.getMethod(METHOD_NAME, EMPTY_PARAMETER_TYPES);
                final Message defaultInstance = (Message) method.invoke(null, EMPTY_ARGUMENTS);
                return defaultInstance;
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw Exceptions.wrappedCause(e);
            }
        }
    }

    /**
     * A builder for {@link NullToleranceTest}.
     */
    public static class Builder {

        private static final String STRING_DEFAULT_VALUE = "";

        private final Set<String> excludedMethods;
        private final Map<Class<?>, ? super Object> defaultValues;
        private Class<?> targetClass;

        private Builder() {
            defaultValues = newHashMap();
            excludedMethods = newHashSet();
        }

        /**
         * Sets the target class to check.
         *
         * <p>This is a mandatory field.
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
         * Adds a default value for the method argument.
         *
         * <p>During the checks, each applicable method of the target class is executed
         * with the {@code null} values passed as arguments. This is done in an iterative fashion,
         * so that each invocation checks the {@code null} tolerance for the only one of the parameters.
         * The rest of the method arguments are set with the default values.
         *
         * <p>If the default value for the type is not customized,
         * the predefined list of the default values per type will be used:
         * <ul>
         *     <li>an empty string is used for the {@code String};
         *     <li>the result of the {@link Collections#emptyList()} call for the types
         *     derived from {@link List};</li>
         *     <li>the result of the {@link Collections#emptySet()} call for the types
         *     derived from {@link Set};</li>
         *     <li>the result of the {@link Collections#emptyMap()} call for the types
         *     derived from {@link Map};</li>
         *     <li>the result of the {@link com.google.common.collect.Queues#newPriorityQueue()} call
         *     for the types derived from {@link Queue};</li>
         *     <li>the result of the {@link com.google.common.base.Defaults#defaultValue(Class)} call
         *     is for the primitives and related wrapper types;</li>
         *     <li>the result of {@code getDefaultInstance} call for the types
         *     derived from {@link Message}.</li>
         * </ul>
         *
         * <p><b>Example.</b>
         *
         * <p>The method declared as
         * <pre> {@code public static void doSomething(PersonName messageValue,
         *                                             Integer wrapperValue,
         *                                             CustomType objectValue)} </pre>
         *
         * <p> will be invoked three times:
         * <ol>
         *     <li>{@code doSomething(null, 0, <default value for CustomType>)},
         *     <li>{@code doSomething(PersonName.getDefaultInstance(), null , <default value for CustomType>)},
         *     <li>{@code doSomething(PersonName.getDefaultInstance(), 0 , null)}.
         * </ol>
         *
         * <p>If the default value for the {@code CustomType} is not provided,
         * an {@code IllegalStateException} is thrown.
         *
         * @param value the default value
         * @return the {@code Builder}
         */
        @SuppressWarnings("WeakerAccess") // it is a public API.
        public <I> Builder addDefaultValue(I value) {
            checkNotNull(value);
            defaultValues.put(value.getClass(), value);
            return this;
        }

        @VisibleForTesting
        @Nullable
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
         * Initializes the {@link NullToleranceTest} instance.
         *
         * @return the {@code nullToleranceTest} instance.
         */
        public NullToleranceTest build() {
            checkState(targetClass != null, "The targetClass is not set. Use setClass().");
            addDefaultTypeValues();

            final NullToleranceTest result = new NullToleranceTest(this);
            return result;
        }

        private void addDefaultTypeValues() {
            final DefaultValueCustomizer<String> stringCustomizer =
                    new DefaultValueCustomizer<>(STRING_DEFAULT_VALUE, defaultValues);
            stringCustomizer.customize(String.class);

            final DefaultValueCustomizer<Queue> queueCustomizer =
                    new DefaultValueCustomizer<>(newPriorityQueue(), defaultValues);
            queueCustomizer.customize(Queue.class);

            final DefaultValueCustomizer<Set> setCustomizer =
                    new DefaultValueCustomizer<>(emptySet(), defaultValues);
            setCustomizer.customize(Set.class);

            final DefaultValueCustomizer<List> listCustomizer =
                    new DefaultValueCustomizer<>(emptyList(), defaultValues);
            listCustomizer.customize(List.class);

            final DefaultValueCustomizer<Map> mapCustomizer =
                    new DefaultValueCustomizer<>(emptyMap(), defaultValues);
            mapCustomizer.customize(Map.class);
        }
    }

    /**
     * Customizes the default value for the provided type.
     */
    private static class DefaultValueCustomizer<T> {
        private final T defaultValue;
        private final Map<Class<?>, ? super Object> defaultValues;

        private <B extends T> DefaultValueCustomizer(B defaultValue, Map<Class<?>, ? super Object> defaultValues) {
            this.defaultValue = defaultValue;
            this.defaultValues = defaultValues;
        }

        /**
         * Adds the {@code defaultValue} for the {@code typeOfInterest},
         * if no default value has been set.
         *
         * @param typeOfInterest the type for which will be provided the default value
         */
        private void customize(Class<T> typeOfInterest) {
            for (Map.Entry<Class<?>, ?> entry : defaultValues.entrySet()) {
                final boolean customValuePresent = typeOfInterest.isAssignableFrom(entry.getKey());
                if (customValuePresent) {
                    return;
                }
            }

            defaultValues.put(typeOfInterest, defaultValue);
        }
    }
}
