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

package org.spine3.server.projection;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.EntityStorageConverter;
import org.spine3.server.entity.RecordBasedRepositoryIO;
import org.spine3.users.TenantId;

/**
 * Beam I/O operations for {@link ProjectionRepository}.
 *
 * @author Alexander Yevsyukov
 */
public class ProjectionRepositoryIO<I, P extends Projection<I, S>, S extends Message>
        extends RecordBasedRepositoryIO<I, P, S> {

    ProjectionRepositoryIO(ProjectionStorageIO<I> storageIO,
                           EntityStorageConverter<I, P, S> converter) {
        super(storageIO, converter);
    }

    public Coder<I> getIdCoder() {
        return storageIO().getIdCoder();
    }

    public KvCoder<I, EntityRecord> getKvCoder() {
        return storageIO().getKvCoder();
    }

    public WriteLastHandledEventTime<I> writeLastHandledEventTime(TenantId tenantId) {
        return new WriteLastHandledEventTime<>(
                ((ProjectionStorageIO<I>) storageIO()).writeLastHandledEventTimeFn(tenantId));
    }

    public static class WriteLastHandledEventTime<I>
            extends PTransform<PCollection<Timestamp>, PDone> {

        private static final long serialVersionUID = 0L;
        private final ProjectionStorageIO.WriteLastHandledEventTimeFn fn;

        public WriteLastHandledEventTime(ProjectionStorageIO.WriteLastHandledEventTimeFn fn) {
            this.fn = fn;
        }

        @Override
        public PDone expand(PCollection<Timestamp> input) {
            input.apply(ParDo.of(fn));
            return PDone.in(input.getPipeline());
        }
    }
}
