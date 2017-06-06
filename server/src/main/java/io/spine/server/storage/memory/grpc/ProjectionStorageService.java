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

import com.google.common.base.Optional;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.Response;
import io.spine.base.Responses;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityRecord;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.storage.memory.grpc.ProjectionStorageServiceGrpc.ProjectionStorageServiceImplBase;
import io.spine.server.tenant.TenantAwareFunction;
import io.spine.server.tenant.TenantAwareFunction0;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

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

    private final BoundedContext boundedContext;

    ProjectionStorageService(BoundedContext boundedContext) {
        this.boundedContext = boundedContext;
    }

    @Nullable
    private ProjectionRepository findRepository(TypeUrl typeUrl,
                                                StreamObserver<?> responseObserver) {
        final Optional<? extends ProjectionRepository<?, ?, ?>> repoOptional =
                boundedContext.getProjectionRepository(typeUrl.getJavaClass());
        if (!repoOptional.isPresent()) {
            responseObserver.onError(newIllegalStateException(
                    "Unable to find ProjectionRepository for the the projection state: %s",
                    typeUrl));
            return null;
        }
        return repoOptional.get();
    }

    @Override
    public void read(RecordStorageRequest request, StreamObserver<EntityRecord> responseObserver) {
        final TypeUrl typeUrl = TypeUrl.parse(request.getEntityStateTypeUrl());
        final Object id = AnyPacker.unpack(request.getEntityId());
        final ProjectionRepository repository = findRepository(typeUrl, responseObserver);
        if (repository == null) {
            // Just quit. The error was reported by `findRepository()`.
            return;
        }
        final TenantAwareFunction<Object, EntityRecord> func =
                new TenantAwareFunction<Object, EntityRecord>(request.getTenantId()) {
                    @Override
                    @SuppressWarnings("unchecked")
                        // The ID type should be preserved by the `request` composition.
                    public EntityRecord apply(@Nullable Object id) {
                        checkNotNull(id);
                        final EntityRecord record = repository.findOrCreateRecord(id);
                        return record;
                    }
                };
        final EntityRecord record = func.execute(id);
        responseObserver.onNext(record);
        responseObserver.onCompleted();
    }

    @Override
    public void write(final RecordStorageRequest request, final StreamObserver<Response> responseObserver) {
        final TypeUrl typeUrl = TypeUrl.parse(request.getEntityStateTypeUrl());
        final ProjectionRepository repository = findRepository(typeUrl, responseObserver);
        if (repository == null) {
            // Just quit. The error was reported by `findRepository()`.
            return;
        }
        new TenantAwareOperation(request.getTenantId()) {
            @Override
            public void run() {
                repository.storeRecord(request.getRecord());
                responseObserver.onNext(Responses.ok());
                responseObserver.onCompleted();
            }
        }.execute();
    }

    @Override
    public void readLastHandledEventTimestamp(LastHandledEventRequest request,
                                              StreamObserver<Timestamp> responseObserver) {
        final TypeUrl typeUrl = TypeUrl.parse(request.getProjectionStateTypeUrl());
        final ProjectionRepository repository = findRepository(typeUrl, responseObserver);
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
        final ProjectionRepository repository = findRepository(typeUrl, responseObserver);
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
