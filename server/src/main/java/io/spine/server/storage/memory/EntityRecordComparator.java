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

package io.spine.server.storage.memory;

import io.spine.client.OrderBy;
import io.spine.client.OrderBy.Direction;
import io.spine.server.entity.storage.EntityRecordWithColumns;

import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A comparator for sorting the contents of {@link TenantRecords}
 * in a provided {@link OrderBy order}.
 */
@SuppressWarnings("ComparatorNotSerializable")
final class EntityRecordComparator implements Comparator<EntityRecordWithColumns> {

    private final String column;

    private EntityRecordComparator(String column) {
        this.column = column;
    }

    /**
     * A static factory for {@code EntityRecordComparator} instances, which sort the
     * {@link TenantRecords} contents in a provided {@link OrderBy orderBy}.
     *
     * @param orderBy
     *         a specification of a column and the direction for ordering
     * @return a new comparator instance, which uses specified column and aligns items
     *         in the provided direction
     * @throws IllegalArgumentException
     *         if the provided {@code OrderBy} is a default instance
     */
    static Comparator<EntityRecordWithColumns> orderedBy(OrderBy orderBy) {
        checkNotNull(orderBy);
        checkArgument(!orderIsEmpty(orderBy),
                      "An empty OrderBy instance cannot be mapped to an EntityRecordComparator.");
        Direction direction = orderBy.getDirection();
        String columnName = orderBy.getColumn();
        if (direction == Direction.ASCENDING) {
            return ascending(columnName);
        }
        return descending(columnName);
    }

    private static Comparator<EntityRecordWithColumns> ascending(String columnName) {
        return new EntityRecordComparator(columnName);
    }

    private static Comparator<EntityRecordWithColumns> descending(String columnName) {
        return ascending(columnName).reversed();
    }

    private static boolean orderIsEmpty(OrderBy orderBy) {
        return orderBy.equals(OrderBy.getDefaultInstance());
    }

    @Override
    public int compare(EntityRecordWithColumns a, EntityRecordWithColumns b) {
        checkNotNull(a);
        checkNotNull(b);
        Object aValue = a.columnValue(column);
        Object bValue = b.columnValue(column);
        if (aValue == null) {
            return bValue == null ? 0 : -1;
        }
        if (bValue == null) {
            return +1;
        }
        if (aValue instanceof Comparable) {
            @SuppressWarnings({"unchecked", "rawtypes"}) // Logically correct.
                    int result = ((Comparable) aValue).compareTo(bValue);
            return result;
        }
        throw newIllegalStateException("The entity record value is not a Comparable");
    }
}
