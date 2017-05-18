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

import com.google.common.base.Objects;
import org.spine3.annotation.Internal;
import org.spine3.server.entity.Entity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * The representation of a field of an {@link Entity} which is stored in the storage in the way
 * efficient for querying.
 *
 * <p>A Column is a value retrieved from an {@link Entity} getter.
 *
 * <h2>Examples</h2>
 *
 *
 * <p>These methods represent the Columns:
 * <code>
 *      <pre>
 *
 *         --UserGroupAggregate.java--
 *
 *         public int getUserCount() {
 *             return getState().getUserCount();
 *         }
 *
 *         --PaymentProcessManager.java--
 *
 *         \@Nullable
 *         public Date getPaymentSystemName() {
 *             if (getState().hasExpirationDate()) {
 *                 return new Date();
 *             } else {
 *                 return null;
 *             }
 *         }
 *     </pre>
 * </code>
 *
 * <p>And these methods are not considered to represent Columns:
 * <code>
 *      <pre>
 *         --UserAggregate.java--
 *
 *         // non-public methods may not represent Columns
 *         private double getHourRate() { ... }
 *
 *         // only methods starting with "get" or "is" may be considered Columns
 *         public boolean hasChildren() { ... }
 *
 *         // getter methods must not accept arguments
 *         public User getStateOf(UserAggregate other) { ... }
 *
 *         // only instance methods are considered Columns
 *         public static Integer isNew(UserAggregate aggregate) { ... }
 *      </pre>
 * </code>

 *
 * <h2>Type policy</h2>
 *
 * <p>A Column can turn into any type. If use a ready implementation of
 * the {@link org.spine3.server.storage.Storage Spine Storages}, the most commonly used types should
 * be already supported. However, you may override the behavior for any type whenever you  wish. For
 * more info, see {@link ColumnTypeRegistry}.
 *
 * <p>To handle specific types of the Columns, which are not supported by default,
 * implement the {@link ColumnType} {@code interface}, register it in a {@link ColumnTypeRegistry}
 * and pass the instance of the registry into the
 * {@link org.spine3.server.storage.StorageFactory StorageFactory} on creation.
 *
 * <h2>Nullability</h2>
 *
 * <p>A Column may turn into {@code null} value if the getter which declares it is annotated as
 * {@link javax.annotation.Nullable javax.annotation.Nullable}. Otherwise, the Column is considered
 * non-null.
 *
 * <p>If a non-null getter method returns {@code null} when trying to get the value of a Column,
 * a {@linkplain RuntimeException} is thrown. See {@link #isNullable()}.
 *
 * <p>The example below shows a faulty Column, which will throw {@linkplain RuntimeException} when
 * trying to get its value.
 * <code>
 *     <pre>
 *         --EmployeeProjection.java--
 *
 *         // method should be annotated as @Nullable to return a null value
 *         public Message getAddress() {
 *             return null;
 *         }
 *     </pre>
 * </code>
 *
 * <p>This class is effectively {@code final} since it has a single {@code private} constructor.
 * Though the modifier "{@code final}" is absent to make it possible to create mocks for testing.
 *
 * @see ColumnType
 * @author Dmytro Dashenkov
 */
public /*final*/ class Column implements Serializable {

    private static final long serialVersionUID = 8200711636725154347L;

    private static final String GETTER_PREFIX_REGEX = "(get)|(is)";
    private static final Pattern GETTER_PREFIX_PATTERN = Pattern.compile(GETTER_PREFIX_REGEX);

    private /*final*/ transient Method getter;

    private final Class<?> type;

    private final String getterMethodName;

    private final String name;

    private final boolean nullable;

    private Column(Method getter, String name, boolean nullable) {
        this.getter = getter;
        this.type = getter.getReturnType();
        this.getterMethodName = getter.getName();
        this.name = name;
        this.nullable = nullable;
    }

    /**
     * Creates new instance of the {@code Column} from the given getter method.
     *
     * @param getter the getter of the Column
     * @return new instance of the {@code Column} reflecting the given property
     */
    public static Column from(Method getter) {
        checkNotNull(getter);
        final String name = nameFromGetterName(getter.getName());
        final boolean nullable = getter.isAnnotationPresent(Nullable.class);
        final Column result = new Column(getter, name, nullable);

        return result;
    }

    private static String nameFromGetterName(String getterName) {
        final Matcher prefixMatcher = GETTER_PREFIX_PATTERN.matcher(getterName);
        String result;
        if (prefixMatcher.find()) {
            result = prefixMatcher.replaceFirst("");
        } else {
            throw new IllegalArgumentException(
                    format("Method %s is not a property getter", getterName));
        }
        result = Character.toLowerCase(result.charAt(0)) + result.substring(1);
        return result;
    }

    /**
     * Retrieves the name of the property which represents the Column.
     *
     * <p>For example, if the getter method has name "isArchivedOrDeleted", the returned value is
     * "archivedOrDeleted".
     *
     * @return the name of the property exposed by this object
     */
    public String getName() {
        return name;
    }

    /**
     * Shows if the Column may return {@code null}s.
     *
     * @return {@code true} if the getter method is annotated as {@link Nullable},
     * {@code false} otherwise
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Retrieves the Column value from the given {@link Entity}.
     *
     * @param source the {@link Entity} to get the Columns from
     * @return the value of the Column represented by this instance of {@code Column}
     */
    public Serializable getFor(Entity<?, ?> source) {
        try {
            @SuppressWarnings("unchecked")
            final Serializable result = (Serializable) getter.invoke(source);
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
     * Retrieves the Column value from the given {@link Entity}.
     *
     * <p>The value is wrapped into a special container, which bears information about the field's
     * metadata.
     *
     * @param source the {@link Entity} to get the Fields from
     * @return the value of the Column represented by this instance of {@code Column} wrapped
     * into {@link MemoizedValue}
     * @see MemoizedValue
     */
    public MemoizedValue memoizeFor(Entity<?, ?> source) {
        final Serializable value = getFor(source);
        final MemoizedValue result = new MemoizedValue(this, value);
        return result;
    }

    /**
     * @return the type of the Column
     */
    public Class getType() {
        return type;
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals") // `getter` field is effectively final
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Column column = (Column) o;
        return Objects.equal(getter, column.getter);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode") // `getter` field is effectively final
    @Override
    public int hashCode() {
        return Objects.hashCode(getter);
    }

    private void readObject(ObjectInputStream inputStream) throws IOException,
                                                                  ClassNotFoundException {
        inputStream.defaultReadObject();
        getter = restoreGetter();
    }

    private Method restoreGetter() {
        checkState(getter == null, "Getter method is already restored.");
        try {
            final Method method = type.getMethod(getterMethodName);
            return method;
        } catch (NoSuchMethodException e) {
            final String errorMsg = format("Cannot find method %s.%s().",
                                           type.getCanonicalName(),
                                           getterMethodName);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * A value of the associated Column saved at some point of time.
     *
     * <p>The class associates the Column value with its metadata i.e. {@linkplain Column}.
     *
     * @see Column#memoizeFor(Entity)
     */
    @Internal
    static class MemoizedValue implements Serializable {

        private static final long serialVersionUID = -6041163252051925293L;

        private final Column sourceColumn;

        @Nullable
        private final Serializable value;

        private MemoizedValue(Column sourceColumn, @Nullable Serializable value) {
            this.sourceColumn = sourceColumn;
            this.value = value;
        }

        @Nullable
        public Serializable getValue() {
            return value;
        }

        boolean isNull() {
            return value == null;
        }

        /**
         * @return the {@link Column} representing this Column
         */
        Column getSourceColumn() {
            return sourceColumn;
        }
    }
}
