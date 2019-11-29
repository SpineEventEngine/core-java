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

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.client.OrderBy;
import io.spine.client.OrderBy.Direction;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.EntityRecordWithColumns;

import java.util.Comparator;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * A comparator for sorting the contents of {@link TenantRecords}
 * in a provided {@link OrderBy order}.
 *
 * @implNote More sophisticated storage implementations can order records by
 *         non-{@link Comparable} fields like {@link com.google.protobuf.Message message}-type
 *         fields, depending on their storage method (e.g. comparing the string content of
 *         messages).The in-memory implementation stores all column values "as-is" and cannot do
 *         that. Trying to {@linkplain OrderBy order by} column of non-comparable type will lead to
 *         an exception being thrown.
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
        checkNotDefaultArg(
                orderBy,
                "An empty `OrderBy` instance cannot be mapped to an `EntityRecordComparator`.");
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

    @SuppressWarnings("ChainOfInstanceofChecks")    // Different special cases are covered.
    @Override
    public int compare(EntityRecordWithColumns a, EntityRecordWithColumns b) {
        checkNotNull(a);
        checkNotNull(b);
        ColumnName columnName = ColumnName.of(column);
        Object aValue = a.columnValue(columnName);
        Object bValue = b.columnValue(columnName);
        if (aValue == null) {
            return bValue == null ? 0 : -1;
        }
        if (bValue == null) {
            return +1;
        }
        if (aValue instanceof Comparable) {
            @SuppressWarnings({"unchecked", "rawtypes"}) // For convenience.
                    int result = ((Comparable) aValue).compareTo(bValue);
            return result;
        }

        if (aValue instanceof Timestamp) {
            @SuppressWarnings({"rawtypes"}) // For convenience.
                    int result = Timestamps.compare((Timestamp) aValue, (Timestamp) bValue);
            return result;
        }
        throw newIllegalStateException("The entity record value is not a Comparable.");
    }
}
