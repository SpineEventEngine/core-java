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

package io.spine.server.projection;

import com.google.protobuf.Timestamp;
import io.spine.server.storage.RecordStorageIO;
import io.spine.server.storage.memory.InMemoryProjectionStorage;
import io.spine.server.storage.memory.InMemoryProjectionStorageIO;
import io.spine.users.TenantId;
import io.spine.util.Exceptions;
import org.apache.beam.sdk.transforms.DoFn;

/**
 * Beam I/O support for projection storages.
 *
 * @author Alexander Yevsyukov
 */
public abstract class ProjectionStorageIO<I> extends RecordStorageIO<I> {

    protected ProjectionStorageIO(Class<I> idClass) {
        super(idClass);
    }

    public static <I> ProjectionStorageIO<I> of(ProjectionStorage<I> storage) {
        if (storage instanceof InMemoryProjectionStorage) {
            InMemoryProjectionStorage<I> memStg = (InMemoryProjectionStorage<I>) storage;
            return InMemoryProjectionStorageIO.of(memStg);
        }
        throw Exceptions.unsupported("Unsupported projection storage class: " + storage.getClass());
    }

    public abstract WriteTimestampFn writeTimestampFn(TenantId tenantId);

    public abstract static class WriteTimestampFn extends DoFn<Timestamp, Void> {

        private static final long serialVersionUID = 0L;
        
        private final TenantId tenantId;

        protected WriteTimestampFn(TenantId tenantId) {
            super();
            this.tenantId = tenantId;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            final Timestamp timestamp = c.element();
            if (timestamp != null) {
                doWrite(tenantId, timestamp);
            }
        }

        protected abstract void doWrite(TenantId tenantId, Timestamp timestamp);
    }
}
