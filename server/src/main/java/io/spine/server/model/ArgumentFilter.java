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

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.base.FieldPath;
import io.spine.base.FieldPaths;
import io.spine.core.ByField;
import io.spine.core.Subscribe;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.base.FieldPaths.getValue;
import static io.spine.base.FieldPaths.typeOfFieldAt;
import static io.spine.string.Stringifiers.fromString;

/**
 * Allows to filter messages passed by a handler method by a value of the message field.
 */
@Immutable
public final class ArgumentFilter implements Predicate<EventMessage> {

    private final FieldPath field;
    @SuppressWarnings("Immutable") // Values are primitives.
    private final Object expectedValue;
    private final boolean acceptsAll;

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
    public static ArgumentFilter createFilter(Method method) {
        Subscribe annotation = method.getAnnotation(Subscribe.class);
        ByField byFieldFilter = annotation.filter();
        String rawFieldPath = byFieldFilter.path();
        if (rawFieldPath.isEmpty()) {
            return acceptingAll();
        }
        FieldPath fieldPath = FieldPaths.parse(rawFieldPath);
        Class<Message> firstParam = AbstractHandlerMethod.firstParamType(method);
        Class<?> fieldType = typeOfFieldAt(firstParam, fieldPath);
        Object expectedValue = fromString(byFieldFilter.value(), fieldType);
        return acceptingOnly(fieldPath, expectedValue);
    }

    /**
     * Tells if the passed filter works on the same field as this one.
     */
    boolean sameField(ArgumentFilter another) {
        boolean result = field.equals(another.field);
        return result;
    }

    /** Obtains the depth of the filtered field. */
    public int pathLength() {
        return field.getFieldNameCount();
    }

    private ArgumentFilter(FieldPath field, Object expectedValue) {
        this.field = field;
        this.expectedValue = expectedValue;
        this.acceptsAll = field.getFieldNameCount() == 0;
    }

    /** Tells if this filter accepts all the events. */
    public boolean acceptsAll() {
        return acceptsAll;
    }

    /**
     * Accepts the passed event message if this filter {@linkplain #acceptingAll() accepts all}
     * events, or if the field of the message matches the configured value.
     */
    @Override
    public boolean test(EventMessage event) {
        if (acceptsAll) {
            return true;
        }
        Object eventField = getValue(field, event);
        boolean result = expectedValue.equals(eventField);
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("field", shortDebugString(field))
                          .add("expectedValue", expectedValue)
                          .toString();
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
