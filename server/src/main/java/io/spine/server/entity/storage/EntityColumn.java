/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Optional;
import com.google.gson.internal.Primitives;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.enumeration.EnumPersistenceTypes;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.entity.storage.ColumnRecords.getAnnotatedVersion;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

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
 * <h2>Column names</h2>
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
 * <h2>Type policy</h2>
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
 * <h2>Nullability</h2>
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
 * @author Dmytro Dashenkov
 * @see ColumnType
 */
public class EntityColumn implements Serializable {

    private static final long serialVersionUID = 0L;

    private static final String GETTER_PREFIX_REGEX = "(get)|(is)";
    private static final Pattern GETTER_PREFIX_PATTERN = Pattern.compile(GETTER_PREFIX_REGEX);

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
    private /*final*/ transient Method getter;

    private final Class<?> entityType;

    private final String getterMethodName;

    private final String name;

    private final String storedName;

    private final boolean nullable;

    private final PersistenceInfo persistenceInfo;

    private EntityColumn(Method getter,
                         String name,
                         String storedName,
                         boolean nullable,
                         PersistenceInfo persistenceInfo) {
        this.getter = getter;
        this.entityType = getter.getDeclaringClass();
        this.getterMethodName = getter.getName();
        this.name = name;
        this.storedName = storedName;
        this.nullable = nullable;
        this.persistenceInfo = persistenceInfo;
    }

    /**
     * Creates new instance of the {@code EntityColumn} from the given getter method.
     *
     * @param getter the getter of the EntityColumn
     * @return new instance of the {@code EntityColumn} reflecting the given property
     */
    public static EntityColumn from(Method getter) {
        checkGetter(getter);
        final String nameForQuery = nameFromGetter(getter);
        final Method annotatedVersion = retrieveAnnotatedVersion(getter);
        final String nameForStore = nameFromAnnotation(annotatedVersion).or(nameForQuery);
        final boolean nullable = getter.isAnnotationPresent(Nullable.class);
        final PersistenceInfo value = PersistenceInfo.from(annotatedVersion);
        return new EntityColumn(getter, nameForQuery, nameForStore, nullable, value);
    }

    private static Method retrieveAnnotatedVersion(Method getter) {
        final Optional<Method> optionalMethod = getAnnotatedVersion(getter);
        if (!optionalMethod.isPresent()) {
            throw newIllegalStateException("Method `%s` is not an entity column getter.", getter);
        }
        return optionalMethod.get();
    }

    private static Optional<String> nameFromAnnotation(Method getter) {
        final String trimmedName = getter.getAnnotation(Column.class)
                                         .name()
                                         .trim();
        return trimmedName.isEmpty()
               ? Optional.<String>absent()
               : Optional.of(trimmedName);
    }

    private static String nameFromGetter(Method getter) {
        final Matcher prefixMatcher = GETTER_PREFIX_PATTERN.matcher(getter.getName());
        final String nameWithoutPrefix = prefixMatcher.replaceFirst("");
        return Character.toLowerCase(nameWithoutPrefix.charAt(0)) + nameWithoutPrefix.substring(1);
    }

    private static void checkGetter(Method getter) {
        checkNotNull(getter);
        checkArgument(GETTER_PREFIX_PATTERN.matcher(getter.getName())
                                           .find() && getter.getParameterTypes().length == 0,
                      "Method `%s` is not a getter.", getter);
        checkArgument(getAnnotatedVersion(getter).isPresent(),
                      format("Entity column getter should be annotated with `%s`.",
                             Column.class.getName()));
        final int modifiers = getter.getModifiers();
        checkArgument(isPublic(modifiers) && !isStatic(modifiers),
                      "Entity column getter should be public instance method.");
        final Class<?> returnType = getter.getReturnType();
        final Class<?> wrapped = Primitives.wrap(returnType);
        checkArgument(Serializable.class.isAssignableFrom(wrapped),
                      format("Cannot create column of non-serializable type %s by method %s.",
                             returnType,
                             getter));
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
    public Serializable getFor(Entity<?, ?> source) {
        try {
            final Serializable result = (Serializable) getter.invoke(source);
            if (!nullable) {
                checkNotNull(result, format("Not null getter %s returned null.", getter.getName()));
            }
            final Serializable value = toPersistedValue(result);
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
        final Serializable value = getFor(source);
        final MemoizedValue result = new MemoizedValue(this, value);
        return result;
    }

    /**
     * @return the type of the column
     */
    public Class getType() {
        return getter.getReturnType();
    }

    /**
     * Returns the type under which the column values are persisted in the data storage.
     *
     * <p>For the non-{@link io.spine.server.entity.storage.enumeration.Enumerated} entity columns
     * this type will be equal to the one retrieved via the {@link #getType()}.
     *
     * <p>For {@link io.spine.server.entity.storage.enumeration.Enumerated} columns, see {@link
     * EnumPersistenceTypes}.
     *
     * @return the persistence type of the column values
     */
    public Class getPersistedType() {
        return persistenceInfo.getPersistedType();
    }

    /**
     * Converts the column value into the value for persistence in the data storage.
     *
     * <p>This method can be used to transform the value obtained through the {@link EntityColumn}
     * getter into the corresponding value used for persistence in the data storage.
     *
     * <p>The value type should be the same as the one obtained through the {@link #getType()}. The
     * output value type will be the same as {@link #getPersistedType()}.
     *
     * <p>For the {@code null} input argument the method will always return {@code null},
     * independently of the declared types.
     *fggf
     * <p>The method is accessible outside of the {@link EntityColumn} class to enable the proper
     * {@link io.spine.client.ColumnFilter} conversion for the {@link
     * io.spine.server.entity.storage.enumeration.Enumerated} column values.
     *
     * @param columnValue the column value to convert
     * @return the column value converted to the form used for persistence in the data storage
     * @throws IllegalArgumentException if the value type is not equal to the {@linkplain #getType()
     *                                  entity column type}
     */
    public Serializable toPersistedValue(@Nullable Object columnValue) {
        if (columnValue == null) {
            return null;
        }
        checkTypeMatches(columnValue);
        final ColumnValueConverter converter = persistenceInfo.getValueConverter();
        final Serializable convertedValue = converter.convert(columnValue);
        return convertedValue;
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
        final StringBuilder sb = new StringBuilder();
        sb.append(entityType.getSimpleName())
          .append('.')
          .append(getName());
        return sb.toString();
    }

    /**
     * Checks that the passed value's type is the same as the {@linkplain #getType() entity column
     * type}.
     *
     * <p>{@linkplain Class#isPrimitive() Primitive types} are ignored by this method to enable
     * proper interaction between the primitive types and their wrapper types.
     *
     * @param value the value to check
     * @throws IllegalArgumentException in case the check fails
     */
    private void checkTypeMatches(Object value) {
        final Class<?> columnType = getType();
        final Class<?> valueType = value.getClass();
        final boolean typesNotPrimitive = !columnType.isPrimitive() && !valueType.isPrimitive();
        final boolean typesMatch = columnType.isAssignableFrom(valueType);
        if (typesNotPrimitive && !typesMatch) {
            throw newIllegalArgumentException(
                    "Passed value type %s doesn't match column type %s.",
                    valueType.getCanonicalName(),
                    columnType.getCanonicalName()
            );
        }
    }

    private void readObject(ObjectInputStream inputStream) throws IOException,
                                                                  ClassNotFoundException {
        inputStream.defaultReadObject();
        getter = restoreGetter();
    }

    private Method restoreGetter() {
        checkState(getter == null, "Getter method is already restored.");
        try {
            final Method method = entityType.getMethod(getterMethodName);
            return method;
        } catch (NoSuchMethodException e) {
            final String errorMsg = format("Cannot find method %s.%s().",
                                           entityType.getCanonicalName(),
                                           getterMethodName);
            throw new IllegalStateException(errorMsg, e);
        }
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

        @Nullable
        private final Serializable value;

        @VisibleForTesting
        MemoizedValue(EntityColumn sourceColumn, @Nullable Serializable value) {
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
         * @return the {@link EntityColumn} representing this column
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
