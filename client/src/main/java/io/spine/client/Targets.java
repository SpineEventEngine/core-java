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

import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeUrl;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

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
     * @param entityClass the class of a target entity
     * @param ids         the IDs of interest
     * @return the instance of {@code Target} assembled according to the parameters.
     */
    public static Target someOf(Class<? extends Message> entityClass,
                                Set<? extends Message> ids) {
        checkNotNull(entityClass);
        checkNotNull(ids);

        Target result = composeTarget(entityClass, ids, null);
        return result;
    }

    /**
     * Create a {@link Target} for all of the specified entity states.
     *
     * @param entityClass the class of a target entity
     * @return the instance of {@code Target} assembled according to the parameters.
     */
    public static Target allOf(Class<? extends Message> entityClass) {
        checkNotNull(entityClass);

        Target result = composeTarget(entityClass, null, null);
        return result;
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    static Target composeTarget(Class<? extends Message> entityClass,
                                @Nullable Set<? extends Message> ids,
                                @Nullable Set<CompositeColumnFilter> columnFilters) {
        boolean includeAll = (ids == null && columnFilters == null);

        Set<? extends Message> entityIds = nullToEmpty(ids);
        Set<CompositeColumnFilter> entityColumnValues = nullToEmpty(columnFilters);

        EntityIdFilter.Builder idFilterBuilder = EntityIdFilter.newBuilder();

        if (!includeAll) {
            for (Message rawId : entityIds) {
                Any packedId = AnyPacker.pack(rawId);
                EntityId entityId = EntityId.newBuilder()
                                                  .setId(packedId)
                                                  .build();
                idFilterBuilder.addIds(entityId);
            }
        }
        EntityIdFilter idFilter = idFilterBuilder.build();
        EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .addAllFilter(entityColumnValues)
                                                   .build();
        String typeUrl = TypeUrl.of(entityClass)
                                      .value();
        Target.Builder builder = Target.newBuilder()
                                             .setType(typeUrl);
        if (includeAll) {
            builder.setIncludeAll(true);
        } else {
            builder.setFilters(filters);
        }

        return builder.build();
    }

    private static <T> Set<T> nullToEmpty(@Nullable Iterable<T> input) {
        if (input == null) {
            return Collections.emptySet();
        } else {
            return Sets.newHashSet(input);
        }
    }
}
