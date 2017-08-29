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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import io.spine.server.entity.storage.Column.MemoizedValue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;

/**
 * A utility for dealing with the {@linkplain EntityRecordWithColumns} and
 * the {@linkplain Column Columns}.
 *
 * @author Dmytro Dashenkov
 */
public final class ColumnRecords {

    private ColumnRecords() {
        // Prevent initialization of the utility class
    }

    /**
     * Feeds the given {@link EntityRecordWithColumns} to the given record representation.
     *
     * <p>This method deals with the {@linkplain Column columns} only.
     * The {@link io.spine.server.entity.EntityRecord EntityRecord} should be handled separately
     * while persisting a record.
     *
     * @param destination         the representation of a record in the database; implementation
     *                            specific
     * @param recordWithColumns   the {@linkplain EntityRecordWithColumns} to persist
     * @param columnTypeRegistry  the {@link io.spine.server.storage.Storage Storage}
     *                            {@link ColumnTypeRegistry}
     * @param mapColumnIdentifier a {@linkplain Function} mapping
     *                            the {@linkplain Column#getName() column name} to the database
     *                            specific column identifier
     * @param <D>                 the type of the database record
     * @param <I>                 the type of the column identifier
     */
    public static <D, I> void feedColumnsTo(
            D destination,
            EntityRecordWithColumns recordWithColumns,
            ColumnTypeRegistry<? extends ColumnType<?, ?, D, I>> columnTypeRegistry,
            Function<String, I> mapColumnIdentifier) {
        checkNotNull(destination);
        checkNotNull(recordWithColumns);
        checkNotNull(columnTypeRegistry);
        checkNotNull(mapColumnIdentifier);
        checkArgument(recordWithColumns.hasColumns(),
                      "Passed record has no Entity Columns.");

        for (Map.Entry<String, MemoizedValue> column : recordWithColumns.getColumnValues()
                                                                        .entrySet()) {
            final I columnIdentifier = mapColumnIdentifier.apply(column.getKey());
            checkNotNull(columnIdentifier);
            final MemoizedValue columnValue = column.getValue();
            final Column columnMetadata = columnValue.getSourceColumn();
            @SuppressWarnings("unchecked") // We don't know the exact types of the value
            final ColumnType<Object, Object, D, I> columnType =
                    (ColumnType<Object, Object, D, I>) columnTypeRegistry.get(columnMetadata);
            checkArgument(columnType != null,
                          String.format("ColumnType for %s could not be found.",
                                        columnMetadata.getType()
                                                      .getCanonicalName()));
            setValue(columnValue, destination, columnIdentifier, columnType);
        }
    }

    /**
     * Obtains the method version annotated with {@link javax.persistence.Column
     * javax.persistence.Column} for the specified method.
     *
     * <p>Scans the specified method, the methods with the same signature
     * from the super classes and interfaces.
     *
     * @param method the method to find the annotated version
     * @return the annotated version of the specified method
     * @throws IllegalStateException if there is more than one annotated method
     */
    static Optional<Method> getAnnotatedVersion(Method method) {
        final Set<Method> annotatedVersions = newHashSet();
        if (method.isAnnotationPresent(javax.persistence.Column.class)) {
            annotatedVersions.add(method);
        }
        final Class<?> declaringClass = method.getDeclaringClass();
        final Iterable<Class<?>> ascendants = getSuperClassesAndInterfaces(declaringClass);
        for (Class<?> ascendant : ascendants) {
            final Optional<Method> optionalMethod = getMethodBySignature(ascendant, method);
            if (optionalMethod.isPresent()) {
                final Method ascendantMethod = optionalMethod.get();
                if (ascendantMethod.isAnnotationPresent(javax.persistence.Column.class)) {
                    annotatedVersions.add(ascendantMethod);
                }
            }
        }
        checkState(annotatedVersions.size() <= 1,
                   "Entity column redefines javax.persistence.Column annotation.");
        if (annotatedVersions.isEmpty()) {
            return Optional.absent();
        } else {
            final Method annotatedVersion = annotatedVersions.iterator()
                                                             .next();
            return Optional.of(annotatedVersion);
        }
    }

    private static Iterable<Class<?>> getSuperClassesAndInterfaces(Class<?> cls) {
        final Collection<Class<?>> interfaces = Arrays.asList(cls.getInterfaces());
        final Collection<Class<?>> result = newHashSet(interfaces);
        Class<?> currentSuper = cls.getSuperclass();
        while (currentSuper != null) {
            result.add(currentSuper);
            currentSuper = currentSuper.getSuperclass();
        }
        return result;
    }

    /**
     * Obtains the method from the specified class with the same signature as the specified method.
     *
     * @param target the class to obtain the method with the signature
     * @param method the method to get the signature
     * @return the method with the same signature obtained from the specified class
     */
    private static Optional<Method> getMethodBySignature(Class<?> target, Method method) {
        checkArgument(!method.getDeclaringClass()
                             .equals(target));
        try {
            final Method methodFromTarget = target.getMethod(method.getName(),
                                                             method.getParameterTypes());
            return Optional.of(methodFromTarget);
        } catch (NoSuchMethodException ignored) {
            return Optional.absent();
        }
    }

    private static <J, S, D, I> void setValue(MemoizedValue columnValue,
                                              D destination,
                                              I columnIdentifier,
                                              ColumnType<J, S, D, I> columnType) {
        @SuppressWarnings("unchecked") // Checked at runtime
        final J initialValue = (J) columnValue.getValue();
        if (initialValue == null) {
            columnType.setNull(destination, columnIdentifier);
        } else {
            final S storedValue = columnType.convertColumnValue(initialValue);
            columnType.setColumnValue(destination, storedValue, columnIdentifier);
        }
    }
}
