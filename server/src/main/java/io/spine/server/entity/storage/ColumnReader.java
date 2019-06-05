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
import com.google.common.collect.ImmutableSet;
import io.spine.server.entity.Entity;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.server.entity.storage.Methods.IS_PREFIX;
import static io.spine.server.entity.storage.Methods.annotatedVersion;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.stream.Stream.concat;

/**
 * A class whose purpose is to obtain {@linkplain EntityColumn entity columns} from the given
 * {@link Entity} type.
 *
 * <p>Each {@code ColumnReader} instance is created for the specified {@link Entity} class.
 *
 * <p>Along with obtaining {@link Entity} class columns, the {@code ColumnReader} performs various
 * checks verifying that {@link EntityColumn} definitions in the processed {@link Entity} class are
 * correct. If column definitions are incorrect, the exception is thrown upon
 * {@linkplain EntityColumn entity columns} reading.
 *
 * @see Columns
 * @see EntityColumn
 */
final class ColumnReader {

    /**
     * A predicate to check whether the given method represents an entity property with the
     * {@code Boolean} return type and the name starting with {@code is-}.
     */
    @VisibleForTesting
    static final Predicate<Method> isBooleanWrapperProperty = isBooleanWrapperProperty();

    private final BeanInfo entityDescriptor;
    private final Class<? extends Entity<?, ?>> entityClass;

    /**
     * Creates a new {@code ColumnReader} instance.
     *
     * @param entityDescriptor
     *         the descriptor of the entity as a Java Bean
     * @param entityClass
     *         the entity class
     */
    private ColumnReader(BeanInfo entityDescriptor, Class<? extends Entity<?, ?>> entityClass) {
        this.entityDescriptor = entityDescriptor;
        this.entityClass = entityClass;
    }

    /**
     * Creates an instance of {@code ColumnReader} for the given {@link Entity} class.
     *
     * @param entityClass
     *         the {@link Entity} class for which to create the instance
     * @return a new instance of {@code ColumnReader} for the specified class
     */
    static ColumnReader forClass(Class<? extends Entity<?, ?>> entityClass) {
        checkNotNull(entityClass);
        try {
            BeanInfo entityDescriptor = Introspector.getBeanInfo(entityClass);
            return new ColumnReader(entityDescriptor, entityClass);
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Retrieves {@linkplain EntityColumn columns} for the processed {@link Entity} class.
     *
     * <p>Performs checks for entity column definitions correctness along the way.
     *
     * <p>If the check for correctness fails, throws {@link IllegalStateException}.
     *
     * @return a {@code Collection} of {@link EntityColumn} corresponded to entity class
     * @throws IllegalStateException if entity column definitions are incorrect
     */
    Collection<EntityColumn> readColumns() {
        ImmutableSet<EntityColumn> columns = scanColumns();
        checkRepeatedColumnNames(columns);
        return columns;
    }

    /**
     * Scans the entity methods for entity columns.
     *
     * <p>The column search is based on the Java Bean
     * <a href="https://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">
     * specification</a>.
     *
     * <p>The only exception are the {@code Boolean} properties starting with {@code is-}
     * which are not covered by the specification and have to be looked up manually.
     */
    private ImmutableSet<EntityColumn> scanColumns() {
        PropertyDescriptor[] properties = entityDescriptor.getPropertyDescriptors();
        Stream<Method> propertyAccessors = Arrays
                .stream(properties)
                .map(PropertyDescriptor::getReadMethod)
                .filter(Objects::nonNull);

        MethodDescriptor[] methodDescriptors = entityDescriptor.getMethodDescriptors();
        Stream<Method> booleanWrapperGetters = Arrays
                .stream(methodDescriptors)
                .map(MethodDescriptor::getMethod)
                .filter(isBooleanWrapperProperty);
        Stream<Method> candidates = concat(propertyAccessors, booleanWrapperGetters);
        ImmutableSet<EntityColumn> columns = candidates
                .map(method -> annotatedVersion(entityClass, method))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(EntityColumn::from)
                .collect(toImmutableSet());
        return columns;
    }

    /**
     * Ensures that the specified columns have no repeated names.
     *
     * <p>If the check fails, throws {@link IllegalStateException}.
     *
     * @param columns
     *         the columns to check
     * @throws IllegalStateException
     *         if columns contain repeated names
     */
    private void checkRepeatedColumnNames(Iterable<EntityColumn> columns) {
        Collection<String> checkedNames = newLinkedList();
        for (EntityColumn column : columns) {
            String columnName = column.storedName();
            if (checkedNames.contains(columnName)) {
                throw newIllegalStateException(
                        "The entity `%s` has columns with the same name for storing `%s`.",
                        entityClass.getName(),
                        columnName
                );
            }
            checkedNames.add(columnName);
        }
    }

    private static Predicate<Method> isBooleanWrapperProperty() {
        return method -> {
            Class<?> returnType = method.getReturnType();
            boolean returnsBoolean = Boolean.class.isAssignableFrom(returnType);
            boolean noParameters = method.getParameterCount() == 0;
            boolean startsWithIs = method.getName()
                                         .startsWith(IS_PREFIX);
            return returnsBoolean
                    && noParameters
                    && startsWithIs;
        };
    }
}
