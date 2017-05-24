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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.coders.BigEndianIntegerCoder;
import org.apache.beam.sdk.coders.BigEndianLongCoder;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.joda.time.Instant;
import org.spine3.base.Identifier;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.tenant.TenantAwareOperation;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.toMillis;
import static org.spine3.util.Exceptions.illegalStateWithCauseOf;
import static org.spine3.util.Exceptions.newIllegalStateException;

/**
 * Abstract base for I/O operations based on Apache Beam.
 *
 * @param <I> the type of indexes in the storage.
 * @author Alexander Yevsyukov
 */
public abstract class RecordStorageIO<I> {

    private static final Coder<EntityRecord> entityRecordCoder = ProtoCoder.of(EntityRecord.class);

    private final Class<I> idClass;

    @Nullable
    private Coder<I> idCoder;

    @Nullable
    private KvCoder<I, EntityRecord> kvCoder;

    protected RecordStorageIO(Class<I> idClass) {
        this.idClass = idClass;
    }

    public Class<I> getIdClass() {
        return idClass;
    }

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
     * Creates a deterministic coder for identifiers used by the storage.
     */
    @SuppressWarnings("unchecked") // the cast is preserved by ID type checking
    public Coder<I> getIdCoder() {
        if (idCoder != null) {
            return idCoder;
        }

        final Identifier.Type idType = Identifier.getType(idClass);
        switch (idType) {
            case INTEGER:
                idCoder = (Coder<I>) BigEndianIntegerCoder.of();
                break;
            case LONG:
                idCoder = (Coder<I>) BigEndianLongCoder.of();
                break;
            case STRING:
                idCoder = (Coder<I>) StringUtf8Coder.of();
                break;
            case MESSAGE:
                idCoder = (Coder<I>) ProtoCoder.of((Class<? extends Message>)idClass);
                break;
            default:
                throw newIllegalStateException("Unsupported ID type: %s", idType.name());
        }

        // Check that the key coder is deterministic.
        try {
            idCoder.verifyDeterministic();
        } catch (Coder.NonDeterministicException e) {
            throw illegalStateWithCauseOf(e);
        }

        return idCoder;
    }

    public static Coder<EntityRecord> getEntityRecordCoder() {
        return entityRecordCoder;
    }

    public KvCoder<I, EntityRecord> getKvCoder() {
        if (kvCoder != null) {
            return kvCoder;
        }

        kvCoder = KvCoder.of(getIdCoder(), getEntityRecordCoder());
        return kvCoder;
    }

    /**
     * Obtains a function for loading query results.
     */
    public abstract FindByQuery<I> findFn(TenantId tenantId);

    /**
     * Obtains a transformation for reading records matching the query.
     *
     * @param tenantId the ID of the tenant for whom records belong
     * @param query    the query to obtain records
     */
    public abstract Read<I> read(TenantId tenantId, Query<I> query);

    public Write<I> write(TenantId tenantId) {
        return new Write<>(writeFn(tenantId));
    }

    /**
     * Obtains a {@link DoFn} that writes an {@code EntityRecord} into the storage.
     */
    public abstract WriteFn<I> writeFn(TenantId tenantId);

    /**
     * Abstract base for functions loading query results.
     *
     * @param <I> the type of IDs
     */
    public abstract static class FindByQuery<I>
            implements SerializableFunction<Query<I>, Iterable<EntityRecord>> {
        private static final long serialVersionUID = 0L;
    }

    /**
     * A query to get multiple records from a storage.
     *
     * @param <I> the type of IDs
     */
    public abstract static class Query<I> implements Serializable {
        private static final long serialVersionUID = 0L;

        public static <I> Query<I> singleRecord(I id) {
            return new SingleRecord<>(id);
        }

        public static <I> Query byIdsAndPredicate(Iterable<I> ids, RecordPredicate predicate) {
            return new ByIdsAndPredicate<>(ids, predicate);
        }

        public abstract Set<I> getIds();

        public abstract RecordPredicate getRecordPredicate();

        public Predicate<KV<I, EntityRecord>> toPredicate() {
            return new Predicate<KV<I, EntityRecord>>() {
                @Override
                public boolean apply(@Nullable KV<I, EntityRecord> input) {
                    checkNotNull(input);
                    if (!getIds().isEmpty()) {
                        if (!getIds().contains(input.getKey())) {
                            return false;
                        }
                    }
                    final Boolean result = getRecordPredicate().apply(input.getValue());
                    return result;
                }
            };
        }

        private static class SingleRecord<I> extends Query<I> {
            private static final long serialVersionUID = 0L;
            private final ImmutableSet<I> oneSet;

            private SingleRecord(I id) {
                oneSet = ImmutableSet.of(id);
            }

            @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK as returning immutable impl.
            @Override
            public Set<I> getIds() {
                return oneSet;
            }

            @Override
            public RecordPredicate getRecordPredicate() {
                return RecordPredicate.Always.isTrue();
            }
        }

        /**
         * A simple query containing a limiting list of IDs and a record predicate.
         *
         * <p>If the set of IDs is empty, the query does not limit the result by IDs.
         *
         * @param <I> the type of IDs
         */
        private static class ByIdsAndPredicate<I> extends Query<I> {

            private static final long serialVersionUID = 0L;
            private final ImmutableSet<I> ids;
            private final RecordPredicate predicate;

            private ByIdsAndPredicate(Iterable<I> ids, RecordPredicate predicate) {
                this.ids = ImmutableSet.copyOf(ids);
                this.predicate = predicate;
            }

            @Override
            @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK as we return immutable impl.
            public Set<I> getIds() {
                return ids;
            }

            @Override
            public RecordPredicate getRecordPredicate() {
                return predicate;
            }
        }
    }

    public abstract static class Read<I>
            extends PTransform<PBegin, PCollection<KV<I, EntityRecord>>> {

        private static final long serialVersionUID = 0L;
        private final ValueProvider<TenantId> tenantId;
        private final Query<I> query;

        protected Read(ValueProvider<TenantId> tenantId, Query<I> query) {
            this.tenantId = tenantId;
            this.query = query;
        }

        protected TenantId getTenantId() {
            final TenantId result = tenantId.get();
            return result;
        }

        protected Query<I> getQuery() {
            return query;
        }
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

    /**
     * Writes entity records into the storage.
     */
    public static class Write<I> extends PTransform<PCollection<KV<I, EntityRecord>>, PDone> {

        private static final long serialVersionUID = 0L;
        private final WriteFn<I> fn;

        private Write(WriteFn<I> fn) {
            this.fn = fn;
        }

        @Override
        public PDone expand(PCollection<KV<I, EntityRecord>> input) {
            input.apply(ParDo.of(fn));
            return PDone.in(input.getPipeline());
        }
    }

    /**
     * A {@link DoFn} that writes {@code EnityRecord} into the storage.
     */
    public abstract static class WriteFn<I> extends DoFn<KV<I, EntityRecord>, Void> {

        private static final long serialVersionUID = 0L;
        private final TenantId tenantId;

        protected WriteFn(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final KV<I, EntityRecord> kv = c.element();
            final TenantAwareOperation op = new TenantAwareOperation(tenantId) {
                @Override
                public void run() {
                    doWrite(kv.getKey(), kv.getValue());
                }
            };
            op.execute();
        }

        protected abstract void doWrite(I key, EntityRecord value);
    }
}
