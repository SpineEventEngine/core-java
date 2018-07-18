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

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.entity.Repository;
import io.spine.server.tenant.TenantFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.validate.Validate.isDefault;

/**
 * The point of integration of the domain and the system bounded context.
 *
 * <p>All the facilities provided by the system bounded context are available through this gateway.
 *
 * @author Dmytro Dashenkov
 */
@Internal
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
    public void postCommand(Message systemCommand, @Nullable TenantId tenantId) {
        checkNotNull(systemCommand);
        CommandFactory commandFactory = buildRequestFactory(tenantId).command();
        Command command = commandFactory.create(systemCommand);
        system.getCommandBus()
              .post(command, noOpObserver());
    }

    @Override
    public CommandIndex commandIndex() {
        Optional<Repository> repository = system.findRepository(ScheduledCommandRecord.class)
                                                .toJavaUtil();
        checkState(repository.isPresent(),
                   "%s is not registered in system bounded context %s.",
                   ScheduledCommandRepository.class.getSimpleName(), system.getName());
        ScheduledCommandRepository commandRepository =
                (ScheduledCommandRepository) repository.get();
        return DomainCommandIndex.atopOf(commandRepository);
    }

    private ActorRequestFactory buildRequestFactory(@Nullable TenantId tenantId) {
        if (tenantId == null || isDefault(tenantId)) {
            return system.isMultitenant()
                   ? buildMultitenantFactory()
                   : buildSingleTenantFactory();
        } else {
            return constructFactory(tenantId);
        }
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
