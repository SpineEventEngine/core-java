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

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.Response;
import io.spine.base.Responses;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityRecord;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.storage.memory.grpc.ProjectionStorageServiceGrpc.ProjectionStorageServiceImplBase;
import io.spine.server.tenant.TenantAwareFunction0;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.type.TypeUrl;

/**
 * Exposes projection storages of a BoundedContext for reading and writing over gRPC.
 *
 * <p>This test harness is needed for being able to write to in-memory storage from Beam-based
 * transformations.
 *
 * <p>See gRPC service declaration in {@code spine/server/storage/storage.proto}.
 *
 * @author Alexander Yevsyukov
 */
class ProjectionStorageService extends ProjectionStorageServiceImplBase {

    private final Helper delegate;

    ProjectionStorageService(BoundedContext boundedContext) {
        this.delegate = new Helper(boundedContext);
    }

    @Override
    public void read(RecordStorageRequest request, StreamObserver<EntityRecord> responseObserver) {
        delegate.read(request, responseObserver);
    }

    @Override
    public void write(final RecordStorageRequest request,
                      final StreamObserver<Response> responseObserver) {
        delegate.write(request, responseObserver);
    }

    @Override
    public void readLastHandledEventTimestamp(LastHandledEventRequest request,
                                              StreamObserver<Timestamp> responseObserver) {
        final TypeUrl typeUrl = TypeUrl.parse(request.getProjectionStateTypeUrl());
        final ProjectionRepository repository =
                (ProjectionRepository) delegate.findRepository(typeUrl, responseObserver);
        if (repository == null) {
            // Just quit. The error was reported by `findRepository()`.
            return;
        }
        final TenantAwareFunction0<Timestamp> func =
                new TenantAwareFunction0<Timestamp>(request.getTenantId()) {
            @Override
            public Timestamp apply() {
                return repository.readLastHandledEventTime();
            }
        };
        final Timestamp timestamp = func.apply();
        responseObserver.onNext(timestamp);
        responseObserver.onCompleted();
    }

    @Override
    public void writeLastHandledEventTimestamp(final LastHandledEventRequest request,
                                               final StreamObserver<Response> responseObserver) {
        final TypeUrl typeUrl = TypeUrl.parse(request.getProjectionStateTypeUrl());
        final ProjectionRepository repository =
                (ProjectionRepository) delegate.findRepository(typeUrl, responseObserver);
        if (repository == null) {
            // Just quit. The error was reported by `findRepository()`.
            return;
        }
        new TenantAwareOperation(request.getTenantId()) {
            @Override
            public void run() {
                repository.writeLastHandledEventTime(request.getTimestamp());
                responseObserver.onNext(Responses.ok());
                responseObserver.onCompleted();
            }
        }.execute();
    }
}
