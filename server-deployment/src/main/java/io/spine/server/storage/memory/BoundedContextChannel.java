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

package io.spine.server.storage.memory;

import io.grpc.ManagedChannel;
import io.spine.server.storage.memory.grpc.InMemoryGrpcServer;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

/**
 * Abstract base for channels performing in-memory storage operations via gRPC.
 *
 * @param <S> the type of a gRPC stub provided by the channel
 * @author Alexander Yevsyukov
 */
abstract class BoundedContextChannel<S extends io.grpc.stub.AbstractStub> {

    private final ManagedChannel channel;
    @Nullable
    private S stub;

    BoundedContextChannel(String boundedContextName) {
        channel = InMemoryGrpcServer.createChannel(boundedContextName);
    }

    void open() {
        stub = createStub(channel);
    }

    protected abstract S createStub(ManagedChannel channel);

    void shutDown() {
        channel.shutdownNow();
    }

    S getStub() {
        checkState(!channel.isShutdown(), "The channel is shut down.");
        checkState(stub != null, "The channel was not open.");
        return stub;
    }
}
