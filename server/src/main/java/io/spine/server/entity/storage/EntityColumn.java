/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.gson.internal.Primitives.wrap;
import static io.spine.server.entity.storage.ColumnValueConverters.of;
import static io.spine.server.entity.storage.Methods.checkGetter;
import static io.spine.server.entity.storage.Methods.mayReturnNull;
import static io.spine.server.entity.storage.Methods.nameFromAnnotation;
import static io.spine.server.entity.storage.Methods.nameFromGetter;
import static io.spine.server.entity.storage.Methods.retrieveAnnotatedVersion;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.lang.String.format;

/**
 * The representation of a field of an {@link Entity} which is stored in the storage in the way
 * efficient for querying.
 *
 * <p>A entity column is a value retrieved from an {@link Entity} getter,
 * which is marked as {@link Column}.
 *
 * <p>Columns are inherited (both from classes and from interfaces).
 * A getter for a column should be annotated only once, i.e. in the place of its declaration.
 *
 * <h1>Column names</h1>
 *
 * <p>A column has different names for storing and for querying.
 *
 * <p>A {@linkplain #getName() name} for working with {@linkplain EntityQueries queries}
 * is determined by a name of column getter, e.g. {@code value} for {@code getValue()}.
 * A client should specify this value to a {@linkplain io.spine.client.ColumnFilters
 * column filters}.
 *
 * <p>A {@linkplain #getStoredName() name}, which is used for keeping a column in a {@code Storage}
 * is determined by the column annotation {@linkplain Column#name() property}.
 *
 * <h2>Examples</h2>
 *
 * <p>These methods represent the columns:
 * <pre>
 *      {@code
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
 *         // only methods annotated with @Column may be considered Columns
 *         public Department getDepartment() { ... }
 *
 *         // non-public methods may not represent Columns
 *         \@Column
 *         private double getHourRate() { ... }
 *
 *         // only methods starting with "get" or "is" may be considered Columns
 *         \@Column
 *         public boolean hasChildren() { ... }
 *
 *         // getter methods must not accept arguments
 *         \@Column
 *         public User getStateOf(UserAggregate other) { ... }
 *
 *         // only instance methods are considered Columns
 *         \@Column
 *         public static Integer isNew(UserAggregate aggregate) { ... }
 *      }
 * </pre>
 *
 * <h1>Type policy</h1>
 *
 * <p>An entity column can turn into any type. If use a ready implementation of
 * the {@link io.spine.server.storage.Storage Spine Storages}, the most commonly used types should
 * be already supported. However, you may override the behavior for any type whenever you wish.
 * For more info, see {@link ColumnTypeRegistry}.
 *
 * <p>To handle specific types of the columns, which are not supported by default,
 * implement the {@link ColumnType} {@code interface}, register it in a {@link ColumnTypeRegistry}
 * and pass the instance of the registry into the
 * {@link io.spine.server.storage.StorageFactory StorageFactory} on creation.
 *
 * <p>Note that the type of an column must either be primitive or implement {@link Serializable}.
 *
 * <h1>Nullability</h1>
 *
 * <p>A column may turn into {@code null} value if the getter which declares it is annotated as
 * {@link javax.annotation.Nullable javax.annotation.Nullable}.
 * Otherwise, the entity column is considered non-null.
 *
 * <p>If a non-null getter method returns {@code null} when trying to get the value of a column,
 * a {@link RuntimeException} is thrown. See {@link #isNullable()}.
 *
 * <p>The example below shows a faulty column, which will throw {@linkplain RuntimeException} when
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
 * <p>This class is effectively {@code final} since it has a single {@code private} constructor.
 * Though the modifier "{@code final}" is absent to make it possible to create mocks for testing.
 *
 * @see ColumnType
 */
@Immutable
public class EntityColumn implements Serializable {

    private static final long serialVersionUID = 0L;

    /**
     * <p>The getter which declares this {@code EntityColumn}.
     *
     * <p>This field contains the getter method declared in an {@link Entity} class, which
     * represents the entity column described by this instance.
     *
     * <p>The equality of this field in two different instances of {@code EntityColumn} determines
     * the equality of the instances. More formally for any non-null instances of
     * {@code EntityColumn} {@code c1} and {@code c2}
     * {@code c1.getter.equals(c2.getter) == c1.equals(c2)}.
     *
     * <p>This field is left non-final for serialization purposes.
     *
     * <p>The only place where this field is updated, except the constructor, is
     * {@link #readObject(ObjectInputStream)} method.
     */
    @SuppressWarnings("Immutable") // See Javadoc.
    private /*final*/ transient Method getter;

    private final Class<?> entityType;

    private final String getterMethodName;

    private final String name;

    private final String storedName;

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
                         String storedName,
                         boolean nullable) {
        this.getter = getter;
        this.entityType = getter.getDeclaringClass();
        this.getterMethodName = getter.getName();
        this.name = name;
        this.storedName = storedName;
        this.nullable = nullable;
        this.valueConverter = of(getter);
    }

    /**
     * Creates new instance of the {@code EntityColumn} from the given getter method.
     *
     * @param getter the getter of the EntityColumn
     * @return new instance of the {@code EntityColumn} reflecting the given property
     */
    public static EntityColumn from(Method getter) {
        checkGetter(getter);
        Method annotatedVersion = retrieveAnnotatedVersion(getter);
        String nameForQuery = nameFromGetter(getter);
        String nameForStore = nameFromAnnotation(annotatedVersion).orElse(nameForQuery);
        boolean nullable = mayReturnNull(getter);
        return new EntityColumn(getter, nameForQuery, nameForStore, nullable);
    }

    /**
     * Obtains the name of the column, which is used for querying.
     *
     * <p>For example, if the getter method has name "isArchivedOrDeleted",
     * the returned value is "archivedOrDeleted".
     *
     * @return the column name for querying
     */
    public String getName() {
        return name;
    }

    /**
     * Obtains the name of the column, which is used to store it in a {@code Storage}.
     *
     * <p>The value is obtained from the annotation {@linkplain Column#name() property}.
     * If the property value is empty, then the value is equal
     * to {@linkplain #getName() name for querying}.
     *
     * @return the column name for storing
     */
    public String getStoredName() {
        return storedName;
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
     * @param source the {@link Entity} to get the Columns from
     * @return the value of the column represented by this instance
     */
    public @Nullable Serializable getFor(Entity<?, ?> source) {
        try {
            Serializable result = (Serializable) getter.invoke(source);
            if (!nullable) {
                checkNotNull(result, format("Not null getter %s returned null.", getter.getName()));
            }
            Serializable value = toPersistedValue(result);
            return value;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                    format("Could not invoke getter of property %s from object %s",
                           getName(),
                           source),
                    e);
        }
    }

    /**
     * Retrieves the column value from the given {@link Entity}.
     *
     * <p>The value is wrapped into a special container,
     * which bears information about the field's metadata.
     *
     * @param source the {@link Entity} to get the Fields from
     * @return the value of the {@code EntityColumn} represented by this instance wrapped
     *         into {@link MemoizedValue}
     */
    MemoizedValue memoizeFor(Entity<?, ?> source) {
        Serializable value = getFor(source);
        MemoizedValue result = new MemoizedValue(this, value);
        return result;
    }

    /**
     * Obtains the type of the column.
     */
    public Class getType() {
        return valueConverter.getSourceType();
    }

    /**
     * Returns the type under which the column values are persisted in the data storage.
     *
     * <p>For the non-{@link Enumerated} entity columns this type will be equal to the one
     * retrieved via the {@link #getType()}.
     *
     * <p>For the {@link Enumerated} columns, see {@link EnumConverter} implementations.
     *
     * @return the persistence type of the column values
     */
    public Class getPersistedType() {
        return valueConverter.getTargetType();
    }

    /**
     * Converts the column value into the value for persistence in the data storage.
     *
     * <p>This method can be used to transform the value obtained through the {@code EntityColumn}
     * getter into the corresponding value used for persistence in the data storage.
     *
     * <p>The value type should be the same as the one obtained through the {@link #getType()}.
     * The output value type will be the same as {@link #getPersistedType()}.
     *
     * <p>As the column value may be {@linkplain #isNullable() nullable}, and all {@code null}
     * values are persisted in the data storage as {@code null}, i.e. without any conversion, for
     * the {@code null} input argument this method will always return {@code null}.
     *
     * <p>The method is accessible outside of the {@code EntityColumn} class to enable the proper
     * {@link io.spine.client.ColumnFilter} conversion for the {@link Enumerated} column values.
     *
     * @param columnValue the column value to convert
     * @return the column value converted to the form used for persistence in the data storage
     * @throws IllegalArgumentException
     *         if the value type is not equal to the {@linkplain #getType() entity column type}
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
        if (o == null || getClass() != o.getClass()) {
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
          .append(getName());
        return sb.toString();
    }

    /**
     * Checks that the passed value's type is the same as
     * the {@linkplain #getType() entity column type}.
     *
     * @param value the value to check
     * @throws IllegalArgumentException in case the check fails
     */
    private void checkTypeMatches(Object value) {
        Class<?> type = getType();
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
        ColumnValueConverter converter = of(getter);
        return converter;
    }

    /**
     * A value of the associated {@code EntityColumn} saved at some point of time.
     *
     * <p>The class associates the column value with its metadata, i.e. {@linkplain EntityColumn}.
     *
     * @see EntityColumn#memoizeFor(Entity)
     */
    @Internal
    public static class MemoizedValue implements Serializable {

        private static final long serialVersionUID = 0L;

        private final EntityColumn sourceColumn;

        private final @Nullable Serializable value;

        @VisibleForTesting
        MemoizedValue(EntityColumn sourceColumn, @Nullable Serializable value) {
            this.sourceColumn = sourceColumn;
            this.value = value;
        }

        public @Nullable Serializable getValue() {
            return value;
        }

        boolean isNull() {
            return value == null;
        }

        /**
         * Returns the {@link EntityColumn} representing this column.
         */
        public EntityColumn getSourceColumn() {
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
            MemoizedValue value1 = (MemoizedValue) o;
            return Objects.equal(getSourceColumn(), value1.getSourceColumn()) &&
                    Objects.equal(getValue(), value1.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getSourceColumn(), getValue());
        }
    }
}
