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
import com.google.protobuf.GeneratedMessageV3;
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
        private static final String STRING_DEFAULT_VALUE = "";
        private static final String METHOD_NAME = "getDefaultInstance";
        private final Map<?, ?> defaultValues;

        private DefaultValuesProvider(Map<?, ?> defaultValues) {
            this.defaultValues = defaultValues;
        }

        /**
         * Returns the default value for the method argument by the method argument type.
         * If default value does not provide, throws {@link IllegalStateException} otherwise.
         *
         * @param key the {@code Class} of the method argument
         * @return the default value
         */
        private Object getDefaultValue(Class<?> key) {
            Object result = defaultValues.get(key);
            if (result != null) {
                return result;
            }

            final boolean primitive = allPrimitiveTypes().contains(key);
            if (primitive) {
                result = defaultValue(key);
            }

            final boolean wrapper = isWrapperType(key);
            if (result == null && wrapper) {
                final Class<?> unwrappedPrimitive = unwrap(key);
                result = defaultValue(unwrappedPrimitive);
            }

            final Class<GeneratedMessageV3> messageClass = GeneratedMessageV3.class;
            final boolean messageParent = messageClass.isAssignableFrom(key);
            if (result == null && messageParent) {
                result = getDefaultMessageInstance(key);
            }

            final Class<String> stringClass = String.class;
            if (result == null && stringClass.isAssignableFrom(key)) {
                result = STRING_DEFAULT_VALUE;
            }
            checkState(result != null);
            return result;
        }

        private static Object getDefaultMessageInstance(Class<?> key) {
            try {
                final Method method = key.getMethod(METHOD_NAME, EMPTY_PARAMETER_TYPES);
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

        private final Set<String> excludedMethods;
        private final Map<? super Class, ? super Object> defaultValues;
        private Class targetClass;

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
         * <p> Each static and non-private method in the {@code targetClass} executes during the check.
         * Each method in the {@code targetClass} executes as many times as non-primitive
         * and non-nullable arguments declared in the method.
         *
         * <p> To the each method passes the {@code null} sequentially for each non-primitive and non-nullable argument,
         * in place of the other arguments set default values.
         *
         * <p> Since the some classes have no value by default, that method serves to add it.
         *
         * <p> The list of the classes and their default values:
         * <ul>
         *     <li> the {@code 0} for the {@code Byte};
         *     <li> the {@code 0} for the {@code Short};
         *     <li> the {@code 0} for the {@code Integer};
         *     <li> the {@code 0} for the {@code Long};
         *     <li> the {@code '\u0000'} for the {@code Character};
         *     <li> the {@code 0.0} for the {@code Float};
         *     <li> the {@code 0.0} for the {@code Double};
         *     <li> the {@code false} for the {@code Boolean};
         *     <li> the {@code 0} for the {@code byte};
         *     <li> the {@code 0} for the {@code short};
         *     <li> the {@code 0} for the {@code int};
         *     <li> the {@code 0} for the {@code long};
         *     <li> the {@code '\u0000'} for the {@code char};
         *     <li> the {@code 0.0} for the {@code float};
         *     <li> the {@code 0.0} for the {@code double};
         *     <li> the {@code false} for the {@code boolean};
         *     <li> the instance provided by the {@code getDefaultInstance} method
         *     will be the default instance for the {@code Class<? extends GeneratedMessageV3>};
         *     </li>
         * </ul>
         *
         * <p><b>For example:</b>
         * <p> {@code public static void method({@link org.spine3.people.PersonName} message,
         *                                                               Integer wrapper,
         *                                                               CustomType obj)}.
         * <p> That method will be executed two times (each execution for the non-primitive and non-nullable argument).
         * The first time instead of the {@code message} will be set the null, instead of the other values will be set
         * the default values. The second time instead of the {@code obj} will be set the default value,
         * provided through that method.
         *
         * <p> In that example default value for the message will be instance,
         * returned by the {@code #getDefaultInstance} method; for the {@code wrapper} is zero;
         * for the {@code obj} need to provide default value, {@code IllegalStateException} will be thrown otherwise.
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
         * Returns the constructed {@link NullToleranceTest}.
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
