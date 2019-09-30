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

package io.spine.server.entity.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.Experimental;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.gson.internal.Primitives.wrap;
import static io.spine.server.entity.storage.Methods.checkAnnotatedGetter;
import static io.spine.server.entity.storage.Methods.mayReturnNull;
import static io.spine.server.entity.storage.Methods.nameFromAnnotation;
import static io.spine.server.entity.storage.Methods.nameFromGetter;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;

/**
 * An {@link Entity} field stored in a way allowing ordering and filtering.
 *
 * <p>The value stored in an entity column is retrieved from an {@link Entity} getter method
 * marked with {@link Column} annotation.
 *
 * <p>An entity can inherit the columns from its parent classes and interfaces.
 * In this case column getter is annotated only once when it is first declared.
 *
 * <p>The {@linkplain #name() name} of a column is determined by the {@link Column#name()}
 * annotation property. If the property is not defined, the name is extracted from the column getter
 * method, for example, {@code value} for {@code getValue()}.
 *
 * <h1>Examples</h1>
 *
 * <p>These methods represent the columns:
 * <pre>
 *      {@code
 *
 *         --OrderAggregate.java--
 *
 *         \@Column
 *         public boolean isDuplicate() {
 *             return getState().isDuplicate();
 *         }
 *
 *         --UserGroupAggregate.java--
 *
 *         \@Column
 *         public int getUserCount() {
 *             return getState().getUserCount();
 *         }
 *
 *         --PaymentProcessManager.java--
 *
 *         \@Nullable
 *         \@Column
 *         public Date getPaymentSystemName() {
 *             if (getState().hasExpirationDate()) {
 *                 return new Date();
 *             } else {
 *                 return null;
 *             }
 *         }
 *     }
 * </pre>
 *
 * <p>And these methods are not considered to represent columns:
 * <pre>
 *      {@code
 *         --UserAggregate.java--
 *
 *         // only methods annotated with @Column can be considered Columns
 *         public Department getDepartment() { ... }
 *
 *         // non-public methods cannot represent Columns
 *         \@Column
 *         private double getHourRate() { ... }
 *
 *         // only methods starting with "get" or "is" can be considered Columns
 *         \@Column
 *         public boolean hasChildren() { ... }
 *
 *         // "is" prefix is allowed only for methods which return "boolean" or "Boolean"
 *         \@Column
 *         public int isNew() { ... }
 *
 *         // getter methods must not accept arguments
 *         \@Column
 *         public User getStateOf(UserAggregate other) { ... }
 *
 *         // only instance methods are considered Columns
 *         \@Column
 *         public static boolean isDeleted(UserAggregate aggregate) { ... }
 *      }
 * </pre>
 *
 * <h1>Type policy</h1>
 *
 * <p>An entity column can be of any type. Most common types are already supported if an existing
 * implementation of the {@link io.spine.server.storage.Storage Spine Storage} is used.
 * However, their behavior can be overridden using {@link ColumnTypeRegistry}.
 *
 * <p>To handle column types not supported by default implement the {@link ColumnType}
 * interface, register it in a {@link ColumnTypeRegistry} and pass the instance of the registry
 * into the {@link io.spine.server.storage.StorageFactory StorageFactory} upon creation.
 *
 * <p>Note that the column type must either be a primitive or implement {@link Serializable}.
 * To order the query results on a column value, it is also required to implement
 * {@link Comparable}.
 *
 * <h1>Nullability</h1>
 *
 * <p>A column may contain {@code null} value if the getter which declares it is marked as nullable.
 * More formally, if the getter method is annotated with an annotation the simple name of which is
 * {@code CheckForNull}, {@code Nullable}, {@code NullableDecl}, or {@code NullableType}, the column
 * may be null. Otherwise, the entity column is considered non-null.
 *
 * <p>If a non-null getter method returns {@code null} when trying to get the value of a column,
 * a {@link RuntimeException} is thrown. See {@link #isNullable()}.
 *
 * <p>The example below shows a faulty column which will throw {@linkplain RuntimeException} when
 * trying to get its value.
 * <pre>
 *     {@code
 *         --EmployeeProjection.java--
 *
 *         // method should be annotated as @Nullable to return a null value
 *         \@Column
 *         public Message getAddress() {
 *             return null;
 *         }
 *     }
 * </pre>
 *
 * @see ColumnType
 */
@Immutable
@Experimental
public final class EntityColumn implements Serializable {

    private static final long serialVersionUID = 0L;

    /**
     * <p>The getter which declares this {@code EntityColumn}.
     *
     * <p>This field contains the getter method declared in an {@link Entity} class which
     * represents the entity column described by this instance.
     *
     * <p>The equality of this field in two different instances of {@code EntityColumn} determines
     * the equality of the instances. More formally for any non-null instances of
     * {@code EntityColumn} {@code c1} and {@code c2}
     * {@code c1.getter.equals(c2.getter) == c1.equals(c2)}.
     *
     * <p>This field is left non-final for serialization purposes.
     *
     * <p>The only place where this field is updated, except for the constructor, is
     * {@link #readObject(ObjectInputStream)} method.
     */
    @SuppressWarnings("Immutable") // See Javadoc.
    private /*final*/ transient Method getter;

    private final Class<?> entityType;

    private final String getterMethodName;

    private final String name;

    private final boolean nullable;

    /**
     * The converter for the column values which transforms the value obtained from the getter into
     * the type suitable for persistence in the data storage.
     *
     * <p>The field is effectively final and is left non-final for serialization purposes only.
     *
     * <p>The only place where this field is updated, except the constructor, is
     * {@link #readObject(ObjectInputStream)} method.
     */
    @SuppressWarnings("Immutable") // See Javadoc.
    private transient ColumnValueConverter valueConverter;

    private EntityColumn(Method getter,
                         String name,
                         boolean nullable) {
        this.getter = getter;
        // To allow calling on non-public classes.
        this.getter.setAccessible(true);
        this.entityType = getter.getDeclaringClass();
        this.getterMethodName = getter.getName();
        this.name = name;
        this.nullable = nullable;
        this.valueConverter = ColumnValueConverters.of(getter);
    }

    /**
     * Creates a new instance of the {@code EntityColumn} from the given getter method.
     *
     * <p>The given method must be annotated with the {@link Column} annotation.
     * An {@code IllegalArgumentException} is thrown otherwise.
     *
     * @param getter
     *         the getter of the EntityColumn
     * @return new instance of the {@code EntityColumn} reflecting the given property
     */
    @VisibleForTesting
    public static EntityColumn from(Method getter) {
        checkAnnotatedGetter(getter);
        String nameForStore = nameFromAnnotation(getter)
                .orElseGet(() -> nameFromGetter(getter));
        boolean nullable = mayReturnNull(getter);
        return new EntityColumn(getter, nameForStore, nullable);
    }

    /**
     * Obtains the name of the column which is used for querying.
     *
     * <p>For example, if the getter method has name "isArchivedOrDeleted",
     * the returned value is "archivedOrDeleted".
     *
     * @return the column name for querying
     */
    public String name() {
        return name;
    }

    /**
     * Determines whether the column value may be {@code null}.
     *
     * @return {@code true} if the getter method is annotated as {@link Nullable},
     *         {@code false} otherwise
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Retrieves the column value from the given {@link Entity}.
     *
     * @param source
     *         the {@link Entity} to get the Columns from
     * @return the value of the column represented by this instance
     */
    public @Nullable Serializable valueIn(Entity<?, ?> source) {
        try {
            Serializable result = (Serializable) getter.invoke(source);
            if (!nullable) {
                checkNotNull(result, "Non-null getter `%s` returned null.", getter.getName());
            }
            Serializable value = toPersistedValue(result);
            return value;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw newIllegalStateException(
                    e, "Could not invoke getter of property `%s` from object `%s`.",
                    name(),
                    source
            );
        }
    }

    /**
     * Retrieves the column value from the given {@link Entity}.
     *
     * <p>The value is wrapped into a special container
     * which bears information about the field's metadata.
     *
     * @param source
     *         the {@link Entity} to get the Fields from
     * @return the value of the {@code EntityColumn} represented by this instance wrapped
     *         into {@link MemoizedValue}
     */
    MemoizedValue memoizeFor(Entity<?, ?> source) {
        Serializable value = valueIn(source);
        MemoizedValue result = new MemoizedValue(this, value);
        return result;
    }

    /**
     * Obtains the type of the column.
     */
    public Class type() {
        return valueConverter.sourceType();
    }

    /**
     * Returns the type under which the column values are persisted in the data storage.
     *
     * <p>For the non-{@link Enumerated} entity columns this type will be equal to the one
     * retrieved using the {@link #type()}.
     *
     * <p>For the {@link Enumerated} columns, see {@link EnumConverter} implementations.
     *
     * @return the persistence type of the column values
     */
    public Class persistedType() {
        return valueConverter.targetType();
    }

    /**
     * Converts the column value into the value for persistence in the data storage.
     *
     * <p>This method can be used to transform the value obtained through the {@code EntityColumn}
     * getter into the corresponding value used for persistence in the data storage.
     *
     * <p>The value type should be the same as the one obtained through the {@link #type()}.
     * The output value type will be the same as {@link #persistedType()}.
     *
     * <p>As the column value may be {@linkplain #isNullable() nullable}, and all {@code null}
     * values are persisted in the data storage as {@code null}, for example, without any
     * conversion.
     * For the {@code null} input argument this method will always return {@code null}.
     *
     * <p>The method is accessible outside of the {@code EntityColumn} class to enable the proper
     * {@linkplain io.spine.client.Filter filters} conversion for the {@link Enumerated} column
     * values.
     *
     * @param columnValue
     *         the column value to convert
     * @return the column value converted to the form used for persistence in the data storage
     * @throws IllegalArgumentException
     *         if the value type is not equal to the {@linkplain #type() entity column type}
     */
    public @Nullable Serializable toPersistedValue(@Nullable Object columnValue) {
        if (columnValue == null) {
            return null;
        }
        checkTypeMatches(columnValue);
        return valueConverter.convert(columnValue);
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals") // `getter` field is effectively final
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityColumn)) {
            return false;
        }
        EntityColumn column = (EntityColumn) o;
        return Objects.equal(getter, column.getter);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode") // `getter` field is effectively final
    @Override
    public int hashCode() {
        return Objects.hashCode(getter);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(entityType.getSimpleName())
          .append('.')
          .append(name());
        return sb.toString();
    }

    /**
     * Checks that the passed value type is the same as
     * the {@linkplain #type() entity column type}.
     *
     * @param value
     *         the value to check
     * @throws IllegalArgumentException
     *         in case the check fails
     */
    private void checkTypeMatches(Object value) {
        Class<?> type = type();
        Class<?> columnType = wrap(type);
        Class<?> valueType = wrap(value.getClass());
        if (!columnType.isAssignableFrom(valueType)) {
            throw newIllegalArgumentException(
                    "Passed value type %s doesn't match column type %s.",
                    valueType.getCanonicalName(),
                    columnType.getCanonicalName()
            );
        }
    }

    private void readObject(ObjectInputStream inputStream)
            throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        getter = restoreGetter();
        valueConverter = restoreValueConverter();
    }

    @VisibleForTesting
    Method restoreGetter() {
        if (getter != null) {
            return getter;
        }
        try {
            Method method = entityType.getMethod(getterMethodName);
            return method;
        } catch (NoSuchMethodException e) {
            String errorMsg = format("Cannot find method %s.%s().",
                                     entityType.getCanonicalName(),
                                     getterMethodName);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    @VisibleForTesting
    ColumnValueConverter restoreValueConverter() {
        if (valueConverter != null) {
            return valueConverter;
        }
        ColumnValueConverter converter = ColumnValueConverters.of(getter);
        return converter;
    }

    /**
     * A value of the associated {@code EntityColumn} saved at some point of time.
     *
     * <p>The class associates the column value with its metadata, for example,
     * {@link EntityColumn}.
     *
     * @see EntityColumn#memoizeFor(Entity)
     */
    @Internal
    public static final class MemoizedValue implements Serializable, Comparable<MemoizedValue> {

        private static final long serialVersionUID = 0L;
        private static final Comparator<MemoizedValue> COMPARATOR = valueComparator();

        private final EntityColumn sourceColumn;

        private final @Nullable Serializable value;

        @VisibleForTesting
        public MemoizedValue(EntityColumn sourceColumn, @Nullable Serializable value) {
            this.sourceColumn = sourceColumn;
            this.value = value;
        }

        public @Nullable Serializable value() {
            return value;
        }

        boolean isNull() {
            return value == null;
        }

        /**
         * Returns the {@link EntityColumn} representing this column.
         */
        public EntityColumn sourceColumn() {
            return sourceColumn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MemoizedValue)) {
                return false;
            }
            MemoizedValue value1 = (MemoizedValue) o;
            return Objects.equal(sourceColumn(), value1.sourceColumn()) &&
                    Objects.equal(value(), value1.value());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(sourceColumn(), value());
        }

        /**
         * Creates a new comparator which orders memoized values in an order specified
         * by {@link MemoizedValue#value() their values} comparison.
         *
         * <p>The memoized values with {@code null} are considered to be less than any other value.
         *
         * <p>A created comparator throws {@link IllegalArgumentException} if
         * {@link MemoizedValue#sourceColumn() source columns} do not match.
         *
         * @return an new comparator for non-null {@code MemoizedValue} instances
         */
        private static Comparator<MemoizedValue> valueComparator() {
            return (a, b) -> {
                checkNotNull(a);
                checkNotNull(b);
                checkArgument(a.columnType().equals(b.columnType()),
                              "Memoized value compared to mismatching value type.");

                Serializable aValue = a.value();
                Serializable bValue = b.value();

                if (aValue == null) {
                    return bValue == null ? 0 : -1;
                }
                if (bValue == null) {
                    return +1;
                }
                if (aValue instanceof Comparable) {
                    @SuppressWarnings("unchecked") // values are checked to be of the same column
                    int result = ((Comparable) aValue).compareTo(bValue);
                    return result;
                }
                throw newIllegalStateException("Memoized value is not a Comparable");
            };
        }

        /**
         * Returns a default {@code MemoizedValue} comparator which orders memoized values
         * in an order specified by {@link MemoizedValue#value() their values} comparison.
         *
         * <p>The memoized values with {@code null} are added to the end of the ordered set.
         *
         * <p>The returned comparator throws {@link IllegalArgumentException} if
         * {@link MemoizedValue#sourceColumn() source columns} do not match.
         *
         * @return an instance of a default comparator for {@code MemoizedValue} instance
         */
        public static Comparator<MemoizedValue> comparator() {
            return COMPARATOR;
        }

        private Class columnType() {
            return sourceColumn().type();
        }

        @Override
        public int compareTo(MemoizedValue that) {
            return COMPARATOR.compare(this, that);
        }
    }
}