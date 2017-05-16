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

package org.spine3.server.storage;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Instant;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.EntityRecord;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;

import static com.google.protobuf.util.Timestamps.toMillis;
import static org.spine3.util.Exceptions.newIllegalStateException;

/**
 * Abstract base for I/O operations based on Apache Beam.
 *
 * @param <I> the type of indexes in the storage.
 * @author Alexander Yevsyukov
 */
public abstract class RecordStorageIO<I> {

    public abstract static class Read<I>
            extends PTransform<PBegin, PCollection<KV<I, EntityRecord>>> {

        private static final long serialVersionUID = 0L;
        private final ValueProvider<TenantId> tenantId;

        /** The IDs to read. All if {@code null}. */
        @Nullable
        private final ValueProvider<Iterable<I>> ids;

        protected Read(ValueProvider<TenantId> tenantId,
                       @Nullable ValueProvider<Iterable<I>> ids) {
            this.tenantId = tenantId;
            this.ids = ids;
        }

        protected TenantId getTenantId() {
            final TenantId result = tenantId.get();
            return result;
        }

        protected Iterable<? super I> getIds() {
            if (ids == null) {
                return ImmutableList.of();
            } else {
                if (!ids.isAccessible()) {
                    throw newIllegalStateException(
                            "Unable to obtain IDs from a provider (class: %s) passed to %s",
                            ids.getClass().getName(),
                            getClass().getName());
                }
                return ids.get();
            }
        }
    }

    /**
     * Obtains a transformation for reading all the records in the storage.
     *
     * @param tenantId the ID of the tenant for whom records belong
     */
    public abstract Read<I> readAll(TenantId tenantId);

    /**
     * Obtains a transformation for reading entity records with the passed indexes.
     */
    public abstract Read<I> read(TenantId tenantId, Iterable<I> ids);

    /**
     * Obtains transformation for extracting an entity state from {@link EntityRecord}s.
     *
     * @param <S> the type of the entity state.
     */
    public static <S extends Message> UnpackFn<S> unpack() {
        return new UnpackFn<>();
    }

    /**
     * Converts Protobuf {@link Timestamp} instance to Joda Time {@link Instant}.
     *
     * <p>Nanoseconds are lost during the conversion as {@code Instant} keeps time with the
     * millis precision.
     */
    public static Instant toInstant(Timestamp timestamp) {
        final long millis = toMillis(timestamp);
        return new Instant(millis);
    }

    /**
     * Extracts the state of an entity from a {@link EntityRecord}.
     *
     * @param <S> the type of the entity state
     */
    public static class UnpackFn<S extends Message> extends DoFn<EntityRecord, S> {
        private static final long serialVersionUID = 0L;

        @ProcessElement
        public void processElement(ProcessContext c) {
            final S entityState = doUnpack(c);
            c.output(entityState);
        }

        protected S doUnpack(ProcessContext c) {
            final EntityRecord record = c.element();
            return AnyPacker.unpack(record.getState());
        }
    }
}
