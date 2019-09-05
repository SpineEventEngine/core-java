/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.core.ByField;
import io.spine.core.Subscribe;
import io.spine.core.Where;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.model.AbstractHandlerMethod.firstParamType;
import static io.spine.string.Stringifiers.fromString;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Allows to filter messages passed by a handler method by a value of the message field.
 */
@Immutable
public final class ArgumentFilter implements Predicate<EventMessage> {

    private final @Nullable Field field;
    @SuppressWarnings("Immutable") // Values are primitives.
    private final @Nullable Object expectedValue;

    /**
     * Creates a new filter which accepts only the passed value of the specified field.
     */
    public static ArgumentFilter acceptingOnly(FieldPath field, Object fieldValue) {
        checkNotNull(field);
        checkNotNull(fieldValue);
        return new ArgumentFilter(field, fieldValue);
    }

    private static ArgumentFilter acceptingAll() {
        return new ArgumentFilter(FieldPath.getDefaultInstance(), Empty.getDefaultInstance());
    }

    /**
     * Creates a new filter by the passed method.
     *
     * <p>If the method is not annotated for filtering, the returned instance
     * {@linkplain ArgumentFilter#acceptsAll() accepts all} arguments.
     */
    @SuppressWarnings("deprecation") // still need to support `ByField` when building older models.
    public static ArgumentFilter createFilter(Method method) {
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        checkAnnotated(method, annotation);
        @Nullable Where where = filterAnnotationOf(method);
        @Nullable ByField byField = annotation.filter();
        boolean byFieldEmpty = byField.path().isEmpty();
        String fieldPath;
        String value;
        if (where != null) {
            fieldPath = where.field();
            value = where.equals();
            checkState(
                    byFieldEmpty,
                    "The subscriber method `%s()` has `@%s` and `@%s`" +
                            " annotations at the same time." +
                            " Please use only one, preferring `%s` because `%s` is deprecated.",
                    method.getName(), ByField.class.getName(), Where.class.getName(),
                    Where.class.getName(), ByField.class.getName()
            );
        } else {
            if (byFieldEmpty) {
                return acceptingAll();
            }
            fieldPath = byField.path();
            value = byField.value();
        }
        Class<Message> paramType = firstParamType(method);
        Field field = Field.parse(fieldPath);
        Class<?> fieldType = field.findType(paramType).orElseThrow(
                () -> newIllegalStateException(
                        "The message with the type `%s` does not have the field `%s`.",
                        paramType.getName(), field)
        );
        Object expectedValue = fromString(value, fieldType);
        return acceptingOnly(field.path(), expectedValue);
    }

    private static @Nullable Where filterAnnotationOf(Method method) {
        Parameter firstParam = firstParameterOf(method);
        return firstParam.getAnnotation(Where.class);
    }

    private static Parameter firstParameterOf(Method method) {
        Parameter[] parameters = method.getParameters();
        checkArgument(parameters.length >= 1,
                      "The method `%s.%s()` does not have parameters.",
                      method.getDeclaringClass().getName(), method.getName());
        return parameters[0];
    }

    private static void checkAnnotated(Method method, @Nullable Subscribe annotation) {
        checkArgument(annotation != null,
                      "The method `%s.%s()` must be annotated with `@%s`.",
                      method.getDeclaringClass().getName(),
                      method.getName(),
                      Subscribe.class.getName()
        );
    }

    @VisibleForTesting
    @Nullable Object expectedValue() {
        return expectedValue;
    }

    /**
     * Tells if the passed filter works on the same field as this one.
     */
    boolean sameField(ArgumentFilter another) {
        if (field == null) {
            return another.field == null;
        }
        boolean result = field.equals(another.field);
        return result;
    }

    /** Obtains the depth of the filtered field. */
    public int pathLength() {
        if (field == null) {
            return 0;
        }
        return field.path().getFieldNameCount();
    }

    private ArgumentFilter(FieldPath path, Object expectedValue) {
        this.field = path.getFieldNameCount() > 0
            ? Field.withPath(path)
            : null;
        this.expectedValue = field != null
            ? expectedValue
            : null;
    }

    /** Tells if this filter accepts all the events. */
    public boolean acceptsAll() {
        return field == null;
    }

    /**
     * Accepts the passed event message if this filter {@linkplain #acceptingAll() accepts all}
     * events, or if the field of the message matches the configured value.
     */
    @Override
    public boolean test(EventMessage event) {
        if (acceptsAll()) {
            return true;
        }
        Object eventField = field.valueIn(event);
        boolean result = eventField.equals(expectedValue);
        return result;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        if (acceptsAll()) {
            helper.add("acceptsAll", true);
        } else {
            helper.add("field", field)
                  .add("expectedValue", expectedValue);
        }
        return helper.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArgumentFilter filter = (ArgumentFilter) o;
        return Objects.equals(field, filter.field) &&
                Objects.equals(expectedValue, filter.expectedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, expectedValue);
    }
}
