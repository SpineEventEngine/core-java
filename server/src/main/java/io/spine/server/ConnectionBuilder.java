/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Abstract base for builders of objects that depend on or expose server-side gRPC objects.
 *
 * <p>A gRPC server can be exposes at a given port or in-process with a given name. In-process
 * arrangements, while being fully-featured are used primarily for testing.
 *
 * @see io.grpc.inprocess.InProcessChannelBuilder
 * @see io.grpc.inprocess.InProcessServerBuilder
 */
public abstract class ConnectionBuilder {

    private @MonotonicNonNull Integer port;
    private @MonotonicNonNull String serverName;

    ConnectionBuilder(@Nullable Integer port, @Nullable String serverName) {
        if (port != null) {
            checkArgument(serverName == null,
                          "`serverName` must be `null` if `port` is defined.");
            this.port = port;
        } else {
            checkArgument(serverName != null,
                          "Either `port` or `serverName` must be defined.");
            this.serverName = serverName;
        }
    }

    /**
     * Obtains the port of the connection, or empty {@code Optional} for in-process connection.
     *
     * @see #serverName()
     */
    public final Optional<Integer> port() {
        return Optional.ofNullable(port);
    }

    /**
     * Obtains the name of the in-process connection, or empty {@code Optional} if the connection
     * is made via a port.
     *
     * @see #port()
     */
    public final Optional<String> serverName() {
        return Optional.ofNullable(serverName);
    }
}
