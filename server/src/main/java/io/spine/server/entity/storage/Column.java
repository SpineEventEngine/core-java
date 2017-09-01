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

package io.spine.server.entity.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.gson.internal.Primitives;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;

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
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

/**
 * The representation of a field of an {@link Entity} which is stored in the storage in the way
 * efficient for querying.
 *
 * <p>A Column is a value retrieved from an {@link Entity} getter,
 * which is marked as {@link EntityColumn}.
 *
 * <p>Columns are inherited (both from classes and from interfaces).
 * A getter for a column should be annotated only once, i.e. in the place of its declaration.
 *
 * <h2>Examples</h2>
 *
 * <p>These methods represent the Columns:
 * <pre>
 *      {@code
 *
 *         --UserGroupAggregate.java--
 *
 *         \@EntityColumn
 *         public int getUserCount() {
 *             return getState().getUserCount();
 *         }
 *
 *         --PaymentProcessManager.java--
 *
 *         \@Nullable
 *         \@EntityColumn
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
 * <p>And these methods are not considered to represent Columns:
 * <pre>
 *      {@code
 *         --UserAggregate.java--
 *
 *         // only methods annotated with @EntityColumn may be considered Columns
 *         public Department getDepartment() { ... }
 *
 *         // non-public methods may not represent Columns
 *         \@EntityColumn
 *         private double getHourRate() { ... }
 *
 *         // only methods starting with "get" or "is" may be considered Columns
 *         \@EntityColumn
 *         public boolean hasChildren() { ... }
 *
 *         // getter methods must not accept arguments
 *         \@EntityColumn
 *         public User getStateOf(UserAggregate other) { ... }
 *
 *         // only instance methods are considered Columns
 *         \@EntityColumn
 *         public static Integer isNew(UserAggregate aggregate) { ... }
 *      }
 * </pre>
 *
 * <h2>Type policy</h2>
 *
 * <p>A Column can turn into any type. If use a ready implementation of
 * the {@link io.spine.server.storage.Storage Spine Storages}, the most commonly used types should
 * be already supported. However, you may override the behavior for any type whenever you wish.
 * For more info, see {@link ColumnTypeRegistry}.
 *
 * <p>To handle specific types of the Columns, which are not supported by default,
 * implement the {@link ColumnType} {@code interface}, register it in a {@link ColumnTypeRegistry}
 * and pass the instance of the registry into the
 * {@link io.spine.server.storage.StorageFactory StorageFactory} on creation.
 *
 * <p>Note that the type of a Column must either be primitive or implement {@link Serializable}.
 *
 * <h2>Nullability</h2>
 *
 * <p>A Column may turn into {@code null} value if the getter which declares it is annotated as
 * {@link javax.annotation.Nullable javax.annotation.Nullable}.
 * Otherwise, the Column is considered non-null.
 *
 * <p>If a non-null getter method returns {@code null} when trying to get the value of a Column,
 * a {@linkplain RuntimeException} is thrown. See {@link #isNullable()}.
 *
 * <p>The example below shows a faulty Column, which will throw {@linkplain RuntimeException} when
 * trying to get its value.
 * <pre>
 *     {@code
 *         --EmployeeProjection.java--
 *
 *         // method should be annotated as @Nullable to return a null value
 *         \@EntityColumn
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
public class Column implements Serializable {

    private static final long serialVersionUID = 0L;

    private static final String GETTER_PREFIX_REGEX = "(get)|(is)";
    private static final Pattern GETTER_PREFIX_PATTERN = Pattern.compile(GETTER_PREFIX_REGEX);

    /**
     * <p>The getter which declares this {@code Column}.
     *
     * <p>This field contains the getter method declared in an {@link Entity} class, which
     * represents the Entity Column described by this instance of {@link Column}.
     *
     * <p>The equality of this field in two different instances of {@code Column} determine
     * the equality of the instances. More formally for any non-null instances of {@code Column}
     * {@code c1} and {@code c2} {@code c1.getter.equals(c2.getter) == c1.equals(c2)}.
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

    private Column(Method getter, String name, String storedName, boolean nullable) {
        this.getter = getter;
        this.entityType = getter.getDeclaringClass();
        this.getterMethodName = getter.getName();
        this.name = name;
        this.storedName = storedName;
        this.nullable = nullable;
    }

    /**
     * Creates new instance of the {@code Column} from the given getter method.
     *
     * @param getter the getter of the Column
     * @return new instance of the {@code Column} reflecting the given property
     */
    public static Column from(Method getter) {
        checkGetter(getter);
        final String nameForQuery = nameFromGetter(getter);
        final Optional<String> nameFromAnnotation = nameFromAnnotation(getter);
        final String nameForStore = nameFromAnnotation.isPresent()
                                    ? nameFromAnnotation.get()
                                    : nameForQuery;
        final boolean nullable = getter.isAnnotationPresent(Nullable.class);
        return new Column(getter, nameForQuery, nameForStore, nullable);
    }

    private static Optional<String> nameFromAnnotation(Method getter) {
        final Optional<Method> optionalMethod = getAnnotatedVersion(getter);
        if (!optionalMethod.isPresent()) {
            throw newIllegalStateException("Method `%s` is not an entity column getter.", getter);
        }
        final Method annotatedVersion = optionalMethod.get();
        final String trimmedName = annotatedVersion.getAnnotation(EntityColumn.class)
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
                             EntityColumn.class.getName()));
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
     * <p>For example, if the getter method has name "isArchivedOrDeleted", the returned value is
     * "archivedOrDeleted".
     *
     * @return the column name for querying
     */
    public String getName() {
        return name;
    }

    /**
     * Obtains the name of the column, which is used for storing in a repositories.
     *
     * <p>The value is obtained from the annotation {@linkplain EntityColumn#name() property}.
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
    MemoizedValue memoizeFor(Entity<?, ?> source) {
        final Serializable value = getFor(source);
        final MemoizedValue result = new MemoizedValue(this, value);
        return result;
    }

    /**
     * @return the type of the Column
     */
    public Class getType() {
        return getter.getReturnType();
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(entityType.getSimpleName())
          .append('.')
          .append(getName());
        return sb.toString();
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
     * A value of the associated Column saved at some point of time.
     *
     * <p>The class associates the Column value with its metadata i.e. {@linkplain Column}.
     *
     * @see Column#memoizeFor(Entity)
     */
    @Internal
    public static class MemoizedValue implements Serializable {

        private static final long serialVersionUID = 0L;

        private final Column sourceColumn;

        @Nullable
        private final Serializable value;

        @VisibleForTesting
        MemoizedValue(Column sourceColumn, @Nullable Serializable value) {
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
