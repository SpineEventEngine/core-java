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
package io.spine.client;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.base.Identifier.checkSupported;
import static io.spine.base.Identifier.pack;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Client-side utilities for working with {@link Query} and
 * {@link Topic Topic} targets.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
@Internal
public final class Targets {

    private Targets() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Create a {@link Target} for a subset of the entity states by specifying their IDs.
     *
     * @param entityClass
     *         the class of a target entity
     * @param ids
     *         the IDs of interest of type {@link io.spine.base.Identifier#checkSupported(Class)
     *         which is supported as identifier}
     * @return the instance of {@code Target} assembled according to the parameters.
     * @throws IllegalArgumentException
     *         if any of IDs have invalid type or are {@code null}
     */
    public static Target someOf(Class<? extends Message> entityClass,
                                Set<?> ids) {
        checkNotNull(entityClass);
        checkNotNull(ids);

        Target result = composeTarget(entityClass, ids, null);
        return result;
    }

    /**
     * Create a {@link Target} for all of the specified entity states.
     *
     * @param entityClass
     *         the class of a target entity
     * @return the instance of {@code Target} assembled according to the parameters.
     */
    public static Target allOf(Class<? extends Message> entityClass) {
        checkNotNull(entityClass);

        Target result = composeTarget(entityClass, null, null);
        return result;
    }

    /**
     * Composes a target for entities matching declared predicates.
     *
     * @param entityClass
     *         the class of a target entity
     * @param ids
     *         the IDs of interest of type {@link io.spine.base.Identifier#checkSupported(Class)
     *         which is supported as identifier}
     * @param columnFilters
     *         a set of entity column predicates each target entity must match
     * @return a {@code Target} instance formed according to the provided parameters
     */
    static Target composeTarget(Class<? extends Message> entityClass,
                                @Nullable Set<?> ids,
                                @Nullable Set<CompositeColumnFilter> columnFilters) {

        boolean includeAll = (ids == null && columnFilters == null);

        TypeUrl typeUrl = TypeUrl.of(entityClass);
        TargetVBuilder builder = TargetVBuilder.newBuilder()
                                               .setType(typeUrl.value());
        if (includeAll) {
            builder.setIncludeAll(true);
        } else {
            List<?> idsList = notNullList(ids);
            EntityIdFilter idFilter = composeIdFilter(idsList);

            List<CompositeColumnFilter> columnFiltersList = notNullList(columnFilters);
            EntityFilters filters = entityFilters(columnFiltersList, idFilter);
            builder.setFilters(filters);
        }

        return builder.build();
    }

    private static EntityIdFilter composeIdFilter(Collection<?> items) {
        List<EntityId> ids = items.stream()
                                  .distinct()
                                  .map(Targets::entityId)
                                  .collect(toList());
        EntityIdFilter filter = idFilter(ids);
        return filter;
    }

    private static EntityIdFilter idFilter(List<EntityId> ids) {
        return EntityIdFilterVBuilder.newBuilder()
                                     .addAllIds(ids)
                                     .build();
    }

    /**
     * Creates an Entity ID from a provided object.
     *
     * @param value
     *         a value to set as {@link EntityId#getId() id} attribute of {@code EntityId}
     * @return an {@link EntityId} with provided value
     * @throws IllegalArgumentException
     *         if the object cannot be an {@link io.spine.base.Identifier} or is {@code null}
     */
    private static EntityId entityId(Object value) {
        return EntityIdVBuilder.newBuilder()
                               .setId(pack(checkId(value)))
                               .build();
    }

    /**
     * Checks that object is not {@code null} and its type
     * {@link io.spine.base.Identifier#checkSupported(Class)
     * is supported as an identifier}.
     *
     * @param item
     *         an object to check
     * @param <I>
     *         a type of an object to check
     * @return the passed object
     */
    private static <I> I checkId(I item) {
        checkNotNull(item);
        checkSupported(item.getClass());
        return item;
    }

    private static EntityFilters entityFilters(List<CompositeColumnFilter> entityColumnValues,
                                               EntityIdFilter idFilter) {
        return EntityFiltersVBuilder.newBuilder()
                                    .setIdFilter(idFilter)
                                    .addAllFilter(entityColumnValues)
                                    .build();
    }

    /**
     * Returns an empty list in case of {@code null} input.
     *
     * @return a new {@link List} instance
     */
    private static <T> List<T> notNullList(@Nullable Iterable<T> input) {
        if (input == null) {
            return emptyList();
        }
        return newLinkedList(input);
    }
}
