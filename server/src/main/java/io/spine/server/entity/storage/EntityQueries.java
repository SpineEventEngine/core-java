/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import io.spine.annotation.Internal;

/**
 * A utility class for working with {@link EntityQuery} instances.
 *
 * @see EntityQuery
 */
@Internal
public final class EntityQueries {

    /** Prevents instantiation of this utility class. */
    private EntityQueries() {
    }

//    /**
//     * Creates new {@link EntityQuery} instances for the given {@link TargetFilters} and
//     * {@link RecordStorage}.
//     *
//     * @param filters
//     *         filters for the Entities specifying the query predicate
//     * @param storage
//     *         a storage for which the query is created
//     * @return new instance of the {@code EntityQuery} with the specified attributes
//     */
//    //TODO:2020-03-18:alex.tymchenko: remove this one, as it is used only in tests.
//    public static <I> EntityQuery<I> from(TargetFilters filters,
//                                          RecordStorage<I> storage) {
//        checkNotNull(filters);
//        checkNotNull(storage);
//
//        EntityColumns entityColumns = storage.columns();
//        EntityQuery<I> result = from(filters, entityColumns);
//        return result;
//    }

//    @VisibleForTesting
//    public static <I> EntityQuery<I> from(TargetFilters filters, EntityColumns columns) {
//        checkNotNull(filters);
//        checkNotNull(columns);
//
//        QueryParameters queryParams = MessageQuery.toQueryParams(filters, columns);
//        List<I> ids = MessageQuery.toIdentifiers(filters);
//
//        EntityQuery<I> result = EntityQuery.of(ids, queryParams);
//        return result;
//    }
}
