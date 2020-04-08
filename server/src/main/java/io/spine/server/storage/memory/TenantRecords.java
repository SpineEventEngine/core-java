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

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.client.ResponseFormat;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.RecordQuery;
import io.spine.server.storage.RecordWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.filterValues;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.FieldMasks.applyMask;
import static io.spine.server.storage.memory.RecordComparator.orderedBy;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;

/**
 * The memory-based storage for message records.
 *
 * <p>Acts like a facade API for the operations available over the data of a single tenant.
 */
class TenantRecords<I, R extends Message> implements TenantStorage<I, RecordWithColumns<I, R>> {

    private final Map<I, RecordWithColumns<I, R>> records = synchronizedMap(new HashMap<>());

    @Override
    public Iterator<I> index() {
        Iterator<I> result = records.keySet()
                                    .iterator();
        return result;
    }

    @Override
    public void put(I id, RecordWithColumns<I, R> record) {
        records.put(id, record);
    }

    /**
     * Returns the message with the passed identifier and applies the given field mask to it.
     *
     * <p>If there is no such a message stored, returns {@code Optional.empty()}.
     */
    public Optional<R> get(I id, FieldMask mask) {
        return get(id).map(r -> new FieldMaskApplier(mask).apply(r.record()));
    }

    @Override
    public Optional<RecordWithColumns<I, R>> get(I id) {
        RecordWithColumns<I, R> record = records.get(id);
        return Optional.ofNullable(record);
    }

    boolean delete(I id) {
        return records.remove(id) != null;
    }

    Iterator<R> readAll(RecordQuery<I> query, ResponseFormat format) {
        FieldMask fieldMask = format.getFieldMask();
        List<RecordWithColumns<I, R>> records = findRecords(query, format);
        return records
                .stream()
                .map(RecordWithColumns::record)
                .map(new FieldMaskApplier(fieldMask))
                .iterator();
    }

    private List<RecordWithColumns<I, R>> findRecords(RecordQuery<I> query, ResponseFormat format) {
        synchronized (records) {
            Map<I, RecordWithColumns<I, R>> filtered = filterRecords(query);
            Stream<RecordWithColumns<I, R>> stream = filtered.values()
                                                             .stream();
            return orderAndLimit(stream, format).collect(toList());
        }
    }

    private static <I, R extends Message> Stream<RecordWithColumns<I, R>>
    orderAndLimit(Stream<RecordWithColumns<I, R>> data, ResponseFormat format) {
        Stream<RecordWithColumns<I, R>> stream = data;
        if (format.getOrderByCount() > 0) {
            stream = stream.sorted(orderedBy(format.getOrderByList()));
        }
        int limit = format.getLimit();
        if (limit > 0) {
            stream = stream.limit(limit);
        }
        return stream;
    }

    /**
     * Filters the records returning only the ones matching the
     * {@linkplain RecordQuery message query}.
     */
    private Map<I, RecordWithColumns<I, R>> filterRecords(RecordQuery<I> query) {
        RecordQueryMatcher<I, R> matcher = new RecordQueryMatcher<>(query);
        return filterValues(records, matcher::test);
    }

    @Override
    public boolean isEmpty() {
        return records.isEmpty();
    }

    /**
     * A {@link Function} transforming the {@link EntityRecord} state by applying the given
     * {@link FieldMask} to it.
     *
     * <p>The resulting {@link EntityRecord} has the same fields as the given one except
     * the {@code state} field, which is masked.
     */
    private class FieldMaskApplier implements Function<R, R> {

        private final FieldMask fieldMask;

        private FieldMaskApplier(FieldMask fieldMask) {
            this.fieldMask = fieldMask;
        }

        @SuppressWarnings("unchecked")
        @Override
        public @Nullable R apply(@Nullable R input) {
            checkNotNull(input);
            if (fieldMask.getPathsList()
                         .isEmpty()) {
                return input;
            }
            if (input instanceof EntityRecord) {
                return (R) maskEntityRecord((EntityRecord) input);
            }
            return applyMask(fieldMask, input);
        }

        private EntityRecord maskEntityRecord(@Nullable EntityRecord input) {
            checkNotNull(input);
            Any maskedState = maskAny(input.getState());
            EntityRecord result = EntityRecord
                    .newBuilder(input)
                    .setState(maskedState)
                    .build();
            return result;
        }

        private Any maskAny(Any message) {
            Message stateMessage = unpack(message);
            Message maskedMessage = applyMask(fieldMask, stateMessage);
            Any result = pack(maskedMessage);
            return result;
        }
    }
}
