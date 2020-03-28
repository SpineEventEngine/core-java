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

package io.spine.server.storage.memory;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.client.OrderBy;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.storage.MessageWithColumns;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Alex Tymchenko
 */
public class MessageRecordComparator<I, R extends Message>
        implements Comparator<MessageWithColumns<I, R>>, Serializable {

    private static final long serialVersionUID = 0L;

    private final String column;

    private MessageRecordComparator(String column) {
        this.column = column;
    }

    /**
     * A static factory for {@code EntityRecordComparator} instances, which sort the
     * {@link TenantRecords} contents in the specified {@link OrderBy orderBy} list.
     *
     * @param orderByList
     *         a specification of columns and the directions for ordering
     * @return a new comparator instance, which uses specified columns and aligns items
     *         in the provided directions
     * @throws IllegalArgumentException
     *         if the provided {@code OrderBy} list is empty
     */
    static <I, R extends Message>
    Comparator<MessageWithColumns<I, R>> orderedBy(List<OrderBy> orderByList) {
        checkArgument(!orderByList.isEmpty(),
                      "`MessageRecordComparator` requires at least one `OrderBy` instance.");
        Comparator<MessageWithColumns<I, R>> result = null;
        for (OrderBy orderBy : orderByList) {
            Comparator<MessageWithColumns<I, R>> thisComparator;
            OrderBy.Direction direction = orderBy.getDirection();
            String columnName = orderBy.getColumn();
            thisComparator = direction == OrderBy.Direction.ASCENDING
                             ? ascending(columnName)
                             : descending(columnName);
            result = result == null
                     ? thisComparator
                     : result.thenComparing(thisComparator);

        }
        return result;
    }

    private static <I, R extends Message>
    Comparator<MessageWithColumns<I, R>> ascending(String columnName) {
        return new MessageRecordComparator<>(columnName);
    }

    private static <I, R extends Message>
    Comparator<MessageWithColumns<I, R>> descending(String columnName) {
        return MessageRecordComparator.<I, R>ascending(columnName).reversed();
    }

    @SuppressWarnings("ChainOfInstanceofChecks")    // Different special cases are covered.
    @Override
    public int compare(MessageWithColumns<I, R> a, MessageWithColumns<I, R> b) {
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
        throw newIllegalStateException("The message record value is not a `Comparable`.");
    }

}
