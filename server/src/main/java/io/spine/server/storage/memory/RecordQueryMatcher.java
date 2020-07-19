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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.query.Column;
import io.spine.query.LogicalOperator;
import io.spine.query.QueryPredicate;
import io.spine.query.RecordQuery;
import io.spine.query.Subject;
import io.spine.query.SubjectParameter;
import io.spine.server.storage.RecordWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Matches the records to the subject of the given {@link io.spine.query.RecordQuery RecordQuery}.
 *
 * @param <I>
 *         the type of the identifiers of the matched records
 * @param <R>
 *         the type of the matched records
 */
public class RecordQueryMatcher<I, R extends Message>
        implements Predicate<@Nullable RecordWithColumns<I, R>> {

    private final ImmutableSet<I> acceptedIds;
    private final ImmutableList<QueryPredicate<R>> predicates;

    RecordQueryMatcher(RecordQuery<I, R> query) {
        this(checkNotNull(query).subject());
    }

    RecordQueryMatcher(Subject<I, R> subject) {
        checkNotNull(subject);
        // Pack IDs from the query for faster search using packed IDs from loaded records.
        this.acceptedIds = subject.id()
                                  .values();
        this.predicates = subject.predicates();
    }

    @Override
    public boolean test(@Nullable RecordWithColumns<I, R> input) {
        if (input == null) {
            return false;
        }
        boolean result = idMatches(input) && columnValuesMatch(input);
        return result;
    }

    private boolean idMatches(RecordWithColumns<I, R> record) {
        if (acceptedIds.isEmpty()) {
            return true;
        }
        I actualId = record.id();
        return acceptedIds.contains(actualId);
    }

    private boolean columnValuesMatch(RecordWithColumns<I, R> record) {
        boolean match;

        for (QueryPredicate<R> predicate : predicates) {
            LogicalOperator operator = predicate.operator();
            switch (operator) {
                case AND:
                    match = checkAnd(predicate.parameters(), record);
                    break;
                case OR:
                    match = checkEither(predicate.parameters(), record);
                    break;
                default:
                    throw newIllegalArgumentException("Logical operator `%s` is invalid.",
                                                      operator);
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    private static <I, R extends Message> boolean
    checkAnd(ImmutableList<SubjectParameter<R, ?, ?>> params, RecordWithColumns<I, R> record) {
        if (params.isEmpty()) {
            return true;
        }
        boolean result = params.stream()
                               .allMatch(param -> matches(record, param));
        return result;
    }

    private static <I, R extends Message> boolean
    checkEither(ImmutableList<SubjectParameter<R, ?, ?>> params, RecordWithColumns<I, R> record) {
        if (params.isEmpty()) {
            return true;
        }
        boolean result = params.stream()
                               .anyMatch(param -> matches(record, param));
        return result;
    }

    private static <I, R extends Message> boolean
    matches(RecordWithColumns<I, R> recWithColumns, SubjectParameter<R, ?, ?> param) {
        Column<R, ?> column = param.column();
        if (!recWithColumns.hasColumn(column.name())) {
            return false;
        }
        @Nullable Object columnValue = recWithColumns.columnValue(param.column()
                                                                       .name());
        boolean result = checkSingleParameter(param, columnValue);
        return result;
    }

    private static <R extends Message> boolean
    checkSingleParameter(SubjectParameter<R, ?, ?> parameter, @Nullable Object actualValue) {
        if (actualValue == null) {
            return false;
        }
        Object paramValue = parameter.value();
        boolean result = parameter.operator()
                                  .eval(actualValue, paramValue);
        return result;
    }
}
