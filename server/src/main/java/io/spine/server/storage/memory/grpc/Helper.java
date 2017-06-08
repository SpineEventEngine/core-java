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
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.Event;
import io.spine.base.Response;
import io.spine.base.Responses;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventStoreIO;
import io.spine.server.tenant.TenantAwareFunction;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Provides access to {@link io.spine.server.entity.RecordBasedRepository}ies of a
 * {@code BoundedContext}.
 *
 * @author Alexander Yevsyukov
 */
class Helper {

    private final BoundedContext boundedContext;

    Helper(BoundedContext boundedContext) {
        this.boundedContext = boundedContext;
    }

    @Nullable
    RecordBasedRepository findRepository(TypeUrl typeUrl, StreamObserver<?> responseObserver) {
        final Class<Message> stateClass = typeUrl.getJavaClass();

        // Since the storage of events is not registered in BoundedContext, get it directly.
        if (stateClass.equals(Event.class)) {
            return (RecordBasedRepository) EventStoreIO.eventStorageOf(boundedContext);
        }

        final Optional<Repository> optional = boundedContext.findRepository(stateClass);
        if (!optional.isPresent()) {
            responseObserver.onError(newIllegalStateException(
                    "Unable to find Repository for the the state: %s", typeUrl));
            return null;
        }
        return (RecordBasedRepository) optional.get();
    }

    void read(RecordStorageRequest request, StreamObserver<EntityRecord> responseObserver) {
        final TypeUrl typeUrl = TypeUrl.parse(request.getEntityStateTypeUrl());
        final RecordBasedRepository repository = findRepository(typeUrl, responseObserver);
        if (repository == null) {
            // Just quit. The error was reported by `findRepository()`.
            return;
        }
        final Object id = AnyPacker.unpack(request.getRead()
                                                  .getId());
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

    void write(final RecordStorageRequest request,
               final StreamObserver<Response> responseObserver) {
        final TypeUrl typeUrl = TypeUrl.parse(request.getEntityStateTypeUrl());
        final RecordBasedRepository repository = findRepository(typeUrl, responseObserver);
        if (repository == null) {
            // Just quit. The error was reported by `findRepository()`.
            return;
        }
        new TenantAwareOperation(request.getTenantId()) {
            @Override
            public void run() {
                repository.storeRecord(request.getWrite());
                responseObserver.onNext(Responses.ok());
                responseObserver.onCompleted();
            }
        }.execute();
    }
}
