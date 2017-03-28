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

package org.spine3.server.entity.storage.reflect;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author
 * Dmytro Dashenkov
 */
public class Column<T> {

    private static final String GETTER_PREFIX_REGEX = "(get)|(is)";
    private static final Pattern GETTER_PREFIX_PATTERN = Pattern.compile(GETTER_PREFIX_REGEX);

    private final Method getter;

    private final String name;

    private final boolean nullable;

    @Nullable
    private final T defaultValue = null; // TODO:2017-03-22:dmytro.dashenkov: Add default value handling.

    private Column(Method getter, String name, boolean nullable) {
            //, @Nullable T defaultValue) {
        this.getter = getter;
        this.name = name;
        this.nullable = nullable;
        //this.defaultValue = defaultValue;
    }

    public static <T> Column<T> from(Method getter) {
        checkNotNull(getter);
        final String name = nameFromGetterName(getter.getName());
        final boolean nullable = getter.isAnnotationPresent(Nullable.class);
        final Column<T> result = new Column<>(getter, name, nullable);

        return result;
    }

    private static String nameFromGetterName(String getterName) {
        final Matcher prefixMatcher = GETTER_PREFIX_PATTERN.matcher(getterName);
        String resullt;
        if (prefixMatcher.find()) {
            resullt = prefixMatcher.replaceFirst("");
        } else {
            throw new IllegalArgumentException(
                    format("Method %s is not a property getter", getterName));
        }
        resullt = Character.toLowerCase(resullt.charAt(0)) + resullt.substring(1);
        return resullt;
    }

    public String getName() {
        return name;
    }

    public boolean isNullable() {
        return nullable;
    }

    public T getFor(Object source) {
        try {
            @SuppressWarnings("unchecked")
            final T result = (T) getter.invoke(source);
            return result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(
                    format("Could not invoke getter of property %s from object %s",
                           getName(),
                           source),
                    e);
        }
    }

    public MemoizedValue<T> memoizeFor(Object source) {
        final T value = getFor(source);
        final MemoizedValue<T> result = new MemoizedValue<>(this, source, value);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return (Class<T>) getter.getReturnType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Column<?> column = (Column<?>) o;

        return getter.equals(column.getter);
    }

    @Override
    public int hashCode() {
        return getter.hashCode();
    }

    public static class MemoizedValue<T> {

        private final Column sourceColumn;
        private final Object sourceObject;

        @Nullable
        private final T value;

        private MemoizedValue(Column sourceColumn, Object sourceObject, @Nullable T value) {
            this.sourceColumn = sourceColumn;
            this.sourceObject = sourceObject;
            this.value = value;
        }

        @Nullable
        public T getValue() {
            return value;
        }

        public boolean isNull() {
            return value == null;
        }

        public Column getSourceColumn() {
            return sourceColumn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MemoizedValue<?> that = (MemoizedValue<?>) o;

            if (!getSourceColumn().equals(that.getSourceColumn())) {
                return false;
            }
            if (!sourceObject.equals(that.sourceObject)) {
                return false;
            }
            return getValue() != null ? getValue().equals(that.getValue()) : that.getValue() ==
                                                                             null;
        }

        @Override
        public int hashCode() {
            int result = getSourceColumn().hashCode();
            result = 31 * result + sourceObject.hashCode();
            result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
            return result;
        }
    }
}
