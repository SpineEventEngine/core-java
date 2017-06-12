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

package io.spine.server.storage;

import io.spine.client.EntityFilters;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.memory.InMemoryRecordStorage;
import io.spine.server.storage.memory.InMemoryRecordStorageIO;
import io.spine.users.TenantId;
import io.spine.util.Exceptions;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * Abstract base for I/O operations based on Apache Beam.
 *
 * @param <I> the type of indexes in the storage.
 * @author Alexander Yevsyukov
 */
public abstract class RecordStorageIO<I> {

    private static final Coder<EntityRecord> ENTITY_RECORD_CODER =
            ProtoCoder.of(EntityRecord.class);

    private final Class<I> idClass;

    @Nullable
    private Coder<I> idCoder;

    @Nullable
    private KvCoder<I, EntityRecord> kvCoder;

    protected RecordStorageIO(Class<I> idClass) {
        this.idClass = idClass;
    }

    public static <I> RecordStorageIO<I> of(RecordStorage<I> storage) {
        if (storage instanceof InMemoryRecordStorage) {
            InMemoryRecordStorage<I> memStg = (InMemoryRecordStorage<I>) storage;
            return InMemoryRecordStorageIO.create(memStg);
        }

        throw Exceptions.unsupported("Unsupported storage class: " + storage.getClass());
    }

    public Class<I> getIdClass() {
        return idClass;
    }

    /**
     * Creates a deterministic coder for identifiers used by the storage.
     */
    public Coder<I> getIdCoder() {
        if (idCoder != null) {
            return idCoder;
        }
        idCoder = BeamUtil.crateIdCoder(idClass);
        return idCoder;
    }

    public KvCoder<I, EntityRecord> getKvCoder() {
        if (kvCoder != null) {
            return kvCoder;
        }
        kvCoder = KvCoder.of(getIdCoder(), ENTITY_RECORD_CODER);
        return kvCoder;
    }

    public Write<I> write(TenantId tenantId) {
        return new Write<>(writeFn(tenantId));
    }

    public abstract ReadFn<I> readFn(TenantId tenantId);

    public abstract FindFn findFn(TenantId tenantId);

    /**
     * Obtains a {@link DoFn} that writes an {@code EntityRecord} into the storage.
     */
    public abstract WriteFn<I> writeFn(TenantId tenantId);

    /**
     * Reads entity record by ID.
     */
    public abstract static class ReadFn<I> extends DoFn<I, KV<I, EntityRecord>> {

        private static final long serialVersionUID = 0L;
        private final TenantId tenantId;

        protected ReadFn(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final I id = c.element();
            final EntityRecord record = doRead(tenantId, id);
            c.output(KV.of(id, record));
        }

        protected abstract EntityRecord doRead(TenantId tenantId, I id);
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
     * A {@link DoFn} that writes {@code EntityRecord} into the storage.
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
            doWrite(tenantId, kv.getKey(), kv.getValue());
        }

        protected abstract void doWrite(TenantId tenantId, I key, EntityRecord value);
    }

    /**
     * A {@link DoFn} that reads {@code EntityRecord}s that match
     */
    public abstract static class FindFn extends DoFn<EntityFilters, EntityRecord> {

        private static final long serialVersionUID = 0L;
        private final TenantId tenantId;

        protected FindFn(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final EntityFilters filters = c.element();
            final Iterator<EntityRecord> records = doFind(tenantId, filters);
            while (records.hasNext()) {
                c.output(records.next());
            }
        }

        protected abstract Iterator<EntityRecord> doFind(TenantId tenantId, EntityFilters filters);
    }
}
