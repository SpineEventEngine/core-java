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

package org.spine3.server.storage.memory.grpc;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.spine3.server.BoundedContext;
import org.spine3.server.transport.GrpcContainer;

import java.io.IOException;

import static org.spine3.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;

/**
 * Starts a gRPC server serving access to in-memory data storage.
 *
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class GrpcServer {

    private static final String HOST = "localhost";
    private static final int PORT = DEFAULT_CLIENT_SERVICE_PORT;

    private final GrpcContainer container;

    public GrpcServer(BoundedContext boundedContext) {
        this.container = GrpcContainer.newBuilder()
                                      .setPort(PORT)
                                      .addService(new ProjectionStorageService(boundedContext))
                                      .addService(new EventStreamService(boundedContext))
                                      .build();
    }

    /**
     * Creates a default channel for read/write operations to in-memory storages via
     * {@link ProjectionStorageService
     * ProjectionStorageService}.
     */
    public static ManagedChannel createDefaultChannel() {
        return ManagedChannelBuilder.forAddress(HOST, PORT)
                                    .build();
    }

    public void start() throws IOException {
        container.start();
    }

    public void shutdown() {
        container.shutdown();
    }
}
