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

package io.spine.server.storage.memory.grpc;

import com.google.protobuf.FieldMask;
import io.grpc.stub.StreamObserver;
import io.spine.base.Response;
import io.spine.client.EntityFilters;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.type.TypeUrl;

import java.util.Iterator;

/**
 * In-process gRPC service for read/write/find operations in in-memory storages.
 *
 * @author Alexander Yevsyukov
 */
class RecordStorageService extends RecordStorageServiceGrpc.RecordStorageServiceImplBase {

    private final Helper delegate;

    RecordStorageService(BoundedContext boundedContext) {
        super();
        this.delegate = new Helper(boundedContext);
    }


    @Override
    public void read(RecordStorageRequest request, StreamObserver<EntityRecord> responseObserver) {
        delegate.read(request, responseObserver);
    }

    @Override
    public void write(RecordStorageRequest request, StreamObserver<Response> responseObserver) {
        delegate.write(request, responseObserver);
    }

    @Override
    public void find(RecordStorageRequest request,
                     final StreamObserver<EntityRecord> responseObserver) {
        final TypeUrl typeUrl = TypeUrl.parse(request.getEntityStateTypeUrl());
        final RecordBasedRepository repository = delegate.findRepository(typeUrl, responseObserver);
        if (repository == null) {
            // Just quit. The error was reported by `findRepository()`.
            return;
        }
        final EntityFilters filters = request.getQuery();
        new TenantAwareOperation(request.getTenantId()) {
            @Override
            public void run() {
                @SuppressWarnings("unchecked")
                // The value type in the map is ensured by the return of `findRecords()`
                final Iterator<EntityRecord> records =
                        repository.findRecords(filters, FieldMask.getDefaultInstance());
                while(records.hasNext()) {
                    final EntityRecord record = records.next();
                    responseObserver.onNext(record);
                }
                responseObserver.onCompleted();
            }
        }.execute();
    }
}
