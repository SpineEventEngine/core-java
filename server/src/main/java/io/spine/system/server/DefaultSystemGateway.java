/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.system.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.tenant.TenantFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.StreamObservers.noOpObserver;

/**
 * The point of integration of the domain and the system bounded context.
 *
 * <p>All the facilities provided by the system bounded context are available through this gateway.
 *
 * @author Dmytro Dashenkov
 */
public final class DefaultSystemGateway implements SystemGateway {

    /**
     * The which posts the system events.
     */
    private static final UserId SYSTEM = UserId
            .newBuilder()
            .setValue("SYSTEM")
            .build();

    private final BoundedContext system;

    public DefaultSystemGateway(BoundedContext system) {
        this.system = system;
    }

    @Override
    public void postCommand(Message systemCommand) {
        checkNotNull(systemCommand);
        CommandFactory commandFactory = buildRequestFactory().command();
        Command command = commandFactory.create(systemCommand);
        system.getCommandBus()
              .post(command, noOpObserver());
    }

    @VisibleForTesting
    BoundedContext target() {
        return system;
    }

    private ActorRequestFactory buildRequestFactory() {
        return system.isMultitenant()
               ? buildMultitenantFactory()
               : buildSingleTenantFactory();
    }

    private static ActorRequestFactory buildMultitenantFactory() {
        TenantFunction<ActorRequestFactory> contextFactory =
                new TenantFunction<ActorRequestFactory>(true) {
                    @Override
                    public ActorRequestFactory apply(@Nullable TenantId tenantId) {
                        checkNotNull(tenantId);
                        return constructFactory(tenantId);
                    }
                };
        ActorRequestFactory result = contextFactory.execute();
        checkNotNull(result);
        return result;
    }

    private static ActorRequestFactory buildSingleTenantFactory() {
        return constructFactory(TenantId.getDefaultInstance());
    }

    private static ActorRequestFactory constructFactory(TenantId tenantId) {
        return ActorRequestFactory.newBuilder()
                                  .setActor(SYSTEM)
                                  .setTenantId(tenantId)
                                  .build();
    }
}
