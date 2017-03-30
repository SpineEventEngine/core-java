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

package org.spine3.server.entity.storage;

import org.spine3.server.entity.Entity;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * A reflective representation of a Storage Field.
 *
 * <p>This class manages the access to the Storage Field value and the reference to its declaration.
 *
 * @author Dmytro Dashenkov
 */
public class Column<T> {

    private static final String GETTER_PREFIX_REGEX = "(get)|(is)";
    private static final Pattern GETTER_PREFIX_PATTERN = Pattern.compile(GETTER_PREFIX_REGEX);

    private final Method getter;

    private final String name;

    private final boolean nullable;

    private Column(Method getter, String name, boolean nullable) {
        this.getter = getter;
        this.name = name;
        this.nullable = nullable;
    }

    /**
     * Creates new instance of the {@code Column} from the given getter method.
     *
     * @param getter the getter of the Storage Field
     * @param <T>    the type of the Storage Field
     * @return new instance of the {@code Column} reflecting the given property
     */
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

    /**
     * Retrieves the name of the property which represents the Storage Field.
     *
     * <p>For example, if the getter method has name "isArchivedOrDeleted", the returned value is
     * "archivedOrDeleted".
     *
     * @return the name of the property reflected by this object
     */
    public String getName() {
        return name;
    }

    /**
     * Shows if the Storage Field may return {@code null}s.
     *
     * @return {@code true} if the getter method is annotated as {@link Nullable},
     * {@code false} otherwise
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Retrieves the Storage Field value from the given {@link Entity}.
     *
     * @param source the {@link Entity} to get the Fields from
     * @return the value of the Storage Field represented by this instance of {@code Column}
     */
    public T getFor(Entity<?, ?> source) {
        try {
            @SuppressWarnings("unchecked")
            final T result = (T) getter.invoke(source);
            if (!nullable) {
                checkNotNull(result, format("Not null getter %s returned null.", getter.getName()));
            }
            return result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                    format("Could not invoke getter of property %s from object %s",
                           getName(),
                           source),
                    e);
        }
    }

    /**
     * Retrieves the Storage Field value from the given {@link Entity}.
     *
     * <p>The value is wrapped into a special container, which bears information about the Field's
     * metadata
     *
     * @param source the {@link Entity} to get the Fields from
     * @return the value of the Storage Field represented y this instance of {@code Column} wrapped
     * into {@link MemoizedValue}
     * @see MemoizedValue
     */
    public MemoizedValue<T> memoizeFor(Entity<?, ?> source) {
        final T value = getFor(source);
        final MemoizedValue<T> result = new MemoizedValue<>(this, value);
        return result;
    }

    /**
     * @return the type of the Storage Field
     */
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

    /**
     * A value of a Storage Field saved at some point in time.
     *
     * <p>The class associates the Storage Field value with its metadata - {@linkplain Column}.
     *
     * @param <T> the type of the Storage Field
     */
    public static class MemoizedValue<T> {

        private final Column<T> sourceColumn;

        @Nullable
        private final T value;

        private MemoizedValue(Column<T> sourceColumn, @Nullable T value) {
            this.sourceColumn = sourceColumn;
            this.value = value;
        }

        @Nullable
        public T getValue() {
            return value;
        }

        public boolean isNull() {
            return value == null;
        }

        /**
         * @return the {@link Column} representing this Storage Field
         */
        public Column<T> getSourceColumn() {
            return sourceColumn;
        }
    }
}
