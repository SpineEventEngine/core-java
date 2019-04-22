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
package io.spine.client;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.checkSupported;
import static java.util.stream.Collectors.toList;

/**
 * Client-side utilities for working with {@link Query} and
 * {@link Topic Topic} targets.
 */
@Internal
public final class Targets {

    /** Prevents instantiation of this utility class. */
    private Targets() {
    }

    /**
     * Creates a {@link Target} for a subset of events/entities by specifying their IDs.
     *
     * @param targetClass
     *         the class of a target event or entity
     * @param ids
     *         the IDs of interest of type
     *         {@linkplain io.spine.base.Identifier#checkSupported(Class) which is supported as
     *         identifier}
     * @return the instance of {@code Target} assembled according to the parameters.
     * @throws IllegalArgumentException
     *         if any of IDs have invalid type or are {@code null}
     */
    public static Target someOf(Class<? extends Message> targetClass,
                                Set<?> ids) {
        checkNotNull(targetClass);
        checkNotNull(ids);

        Target result = composeTarget(targetClass, ids, null);
        return result;
    }

    /**
     * Create a {@link Target} for all events/entities of the specified type.
     *
     * @param targetClass
     *         the class of a target event/entity
     * @return the instance of {@code Target} assembled according to the parameters.
     */
    public static Target allOf(Class<? extends Message> targetClass) {
        checkNotNull(targetClass);

        Target result = composeTarget(targetClass, null, null);
        return result;
    }

    /**
     * Composes a target for the events/entities matching declared predicates.
     *
     * @param targetClass
     *         the class of a target event/entity
     * @param ids
     *         the IDs of interest of type
     *         {@linkplain io.spine.base.Identifier#checkSupported(Class) which is supported as
     *         identifier}
     * @param filters
     *         a set of predicates which target entity state or event message must match
     * @return a {@code Target} instance formed according to the provided parameters
     */
    public static Target composeTarget(Class<? extends Message> targetClass,
                                       @Nullable Iterable<?> ids,
                                       @Nullable Iterable<CompositeFilter> filters) {
        checkNotNull(targetClass);

        boolean includeAll = (ids == null && filters == null);

        TypeUrl typeUrl = TypeUrl.of(targetClass);
        TargetVBuilder builder = Target.vBuilder()
                                       .setType(typeUrl.value());
        if (includeAll) {
            builder.setIncludeAll(true);
        } else {
            List<?> idsList = nonNullList(ids);
            IdFilter idFilter = composeIdFilter(idsList);

            List<CompositeFilter> filterList = nonNullList(filters);
            TargetFilters targetFilters = targetFilters(filterList, idFilter);
            builder.setFilters(targetFilters);
        }

        return builder.build();
    }

    private static IdFilter composeIdFilter(Collection<?> items) {
        List<Any> ids = items
                .stream()
                .distinct()
                .map(Targets::checkId)
                .map(Identifier::pack)
                .collect(toList());
        IdFilter filter = idFilter(ids);
        return filter;
    }

    private static IdFilter idFilter(List<Any> ids) {
        return IdFilter.vBuilder()
                       .addAllIds(ids)
                       .build();
    }

    /**
     * Checks that object is not {@code null} and its type
     * {@linkplain io.spine.base.Identifier#checkSupported(Class) is supported as an identifier}.
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

    private static TargetFilters targetFilters(List<CompositeFilter> filters, IdFilter idFilter) {
        return TargetFilters.vBuilder()
                            .setIdFilter(idFilter)
                            .addAllFilter(filters)
                            .build();
    }

    /**
     * Returns an empty list in case of {@code null} input.
     *
     * @return a new {@link List} instance
     */
    private static <T> ImmutableList<T> nonNullList(@Nullable Iterable<T> input) {
        if (input == null) {
            return ImmutableList.<T>builder().build();
        }
        return ImmutableList.copyOf(input);
    }
}
