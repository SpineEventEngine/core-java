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
package org.spine3.client;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.protobuf.AnyPacker;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Client-side utilities for working with {@link Query} and
 * {@link Topic Topic} targets.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
public class Targets {

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

        final Target result = composeTarget(entityClass, ids);
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

        final Target result = composeTarget(entityClass, null);
        return result;
    }

    static Target composeTarget(Class<? extends Message> entityClass,
                                @Nullable Set<? extends Message> ids) {
        final boolean includeAll = (ids == null);

        final EntityIdFilter.Builder idFilterBuilder = EntityIdFilter.newBuilder();

        if (!includeAll) {
            for (Message rawId : ids) {
                final Any packedId = AnyPacker.pack(rawId);
                final EntityId entityId = EntityId.newBuilder()
                                                  .setId(packedId)
                                                  .build();
                idFilterBuilder.addIds(entityId);
            }
        }
        final EntityIdFilter idFilter = idFilterBuilder.build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .build();
        final String typeName = TypeName.of(entityClass)
                                        .value();
        final Target.Builder builder = Target.newBuilder()
                                             .setType(typeName);
        if (includeAll) {
            builder.setIncludeAll(true);
        } else {
            builder.setFilters(filters);
        }

        return builder.build();
    }
}
