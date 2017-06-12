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

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityStorageConverter;
import io.spine.server.entity.RecordBasedRepositoryIO;
import io.spine.users.TenantId;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

/**
 * Beam I/O operations for {@link ProjectionRepository}.
 *
 * @author Alexander Yevsyukov
 */
public class ProjectionRepositoryIO<I, P extends Projection<I, S, ?>, S extends Message>
        extends RecordBasedRepositoryIO<I, P, S> {

    public static <I, P extends Projection<I, S, ?>, S extends Message>
    ProjectionRepositoryIO<I, P, S> of(ProjectionRepository<I, P, S> repository) {
        final EntityStorageConverter<I, P, S> converter = repository.entityConverter();
        final ProjectionStorageIO<I> storageIO =
                ProjectionStorageIO.of(repository.projectionStorage());
        return new ProjectionRepositoryIO<>(storageIO, converter);
    }

    ProjectionRepositoryIO(ProjectionStorageIO<I> storageIO,
                           EntityStorageConverter<I, P, S> converter) {
        super(storageIO, converter);
    }

    Coder<I> getIdCoder() {
        return storageIO().getIdCoder();
    }

    KvCoder<I, EntityRecord> getKvCoder() {
        return storageIO().getKvCoder();
    }

    WriteTimestamp writeLastHandledEventTime(TenantId tenantId) {
        return new WriteTimestamp(
                ((ProjectionStorageIO<I>) storageIO()).writeTimestampFn(tenantId));
    }

    static class WriteTimestamp extends PTransform<PCollection<Timestamp>, PDone> {

        private static final long serialVersionUID = 0L;
        private final ProjectionStorageIO.WriteTimestampFn fn;

        private WriteTimestamp(ProjectionStorageIO.WriteTimestampFn fn) {
            this.fn = fn;
        }

        @Override
        public PDone expand(PCollection<Timestamp> input) {
            input.apply(ParDo.of(fn));
            return PDone.in(input.getPipeline());
        }
    }
}
