/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.testing.server.blackbox;

import io.spine.client.Client;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.Closeable;
import io.spine.server.CommandService;
import io.spine.server.GrpcContainer;
import io.spine.server.QueryService;
import io.spine.server.SubscriptionService;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Objects.isNull;

/**
 * Upon request, creates {@code Client} instances for the passed Bounded Context.
 *
 * <p>As {@code Client} requires a gRPC server to be connected to, the provider assembles
 * and starts a {@link GrpcContainer}.
 */
class ClientProvider implements Closeable {

    private final BoundedContext context;
    private final List<Client> openClients;

    private @MonotonicNonNull GrpcContainer grpcContainer;
    private @MonotonicNonNull String serverName;

    /**
     * Creates a provider, {@code Client}s of which would be linked to the specified context.
     */
    ClientProvider(BoundedContext context) {
        this.context = context;
        this.openClients = new ArrayList<>();
    }

    /**
     * Returns a {@code Client} to a single tenant {@code BoundedContext}.
     */
    Client get() {
        ensureServer();

        Client result = openClients
                .stream()
                .filter(client -> client.isOpen() && !client.tenant().isPresent())
                .findFirst()
                .orElseGet(this::openClient);

        return result;
    }

    private Client openClient() {
        Client client = Client.inProcess(serverName)
                              .build();
        openClients.add(client);
        return client;
    }

    /**
     * Returns a {@code Client} to a multitenant {@code BoundedContext} with a tenant specified.
     */
    Client getFor(TenantId tenantId) {
        ensureServer();

        Client result = openClients
                .stream()
                .filter(client -> {
                    Optional<TenantId> optionalTenant = client.tenant();
                    return client.isOpen()
                            && optionalTenant.isPresent()
                            && optionalTenant.get().equals(tenantId);
                })
                .findFirst()
                .orElseGet(() -> openClient(tenantId));

        return result;
    }

    private Client openClient(TenantId tenantId) {
        Client client = Client.inProcess(serverName)
                              .forTenant(tenantId)
                              .build();
        openClients.add(client);
        return client;
    }

    /**
     * Every {@link Client} needs a gRPC server to be connected to. This method makes certain
     * the provider has a gRPC server running and ready to establish connections with.
     */
    private void ensureServer() {
        checkOpen();

        if (isNull(serverName)) {
            initServer();
        }
    }

    private void initServer() {
        CommandService commandService = CommandService.fromSingle(context);
        QueryService queryService = QueryService.fromSingle(context);
        SubscriptionService subscriptionService = SubscriptionService.fromSingle(context);

        String serverName = UUID.randomUUID().toString();
        GrpcContainer grpcContainer = GrpcContainer
                .inProcess(serverName)
                .addService(commandService)
                .addService(queryService)
                .addService(subscriptionService)
                .build();

        try {
            grpcContainer.start();
        } catch (IOException e) {
            illegalStateWithCauseOf(e);
        }

        this.serverName = serverName;
        this.grpcContainer = grpcContainer;
    }

    /**
     * Closes all created clients and shuts down the associated {@code GrpcContainer}.
     */
    @Override
    @SuppressWarnings("TestOnlyProblems")
    public void close() throws Exception {
        if (!isOpen() || isNull(grpcContainer)) {
            return;
        }

        grpcContainer.shutdownNowAndWait();
        openClients.forEach(Client::close);
        openClients.clear();
    }

    @Override
    public boolean isOpen() {
        return isNull(grpcContainer) || !grpcContainer.isShutdown();
    }
}
