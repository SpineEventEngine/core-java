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

import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.client.CommandFactory;
import io.spine.client.Query;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.UserId;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The point of integration of the domain and the system bounded context.
 *
 * <p>All the facilities provided by the system bounded context are available through this gateway.
 */
final class DefaultSystemWriteSide implements SystemWriteSide {

    /**
     * The ID of the user which is used for generating system commands and events.
     */
    static final UserId SYSTEM_USER = UserId
            .newBuilder()
            .setValue("SYSTEM")
            .build();

    private final SystemContext system;

    DefaultSystemWriteSide(SystemContext system) {
        this.system = system;
    }

    @Override
    public void postCommand(CommandMessage systemCommand) {
        checkNotNull(systemCommand);
        CommandFactory commandFactory = SystemCommandFactory.newInstance(system.isMultitenant());
        Command command = commandFactory.create(systemCommand);
        system.getCommandBus()
              .post(command, noOpObserver());
    }

    @Override
    public void postEvent(EventMessage systemEvent) {
        checkNotNull(systemEvent);
        SystemEventFactory factory = SystemEventFactory.forMessage(systemEvent,
                                                                   system.isMultitenant());
        Event event = factory.createEvent(systemEvent, null);
        system.getImportBus()
              .post(event, noOpObserver());
    }

    @Override
    public Iterator<Any> readDomainAggregate(Query query) {
        @SuppressWarnings("unchecked") // Logically checked.
        MirrorRepository repository = (MirrorRepository)
                system.findRepository(Mirror.class)
                      .orElseThrow(
                              () -> newIllegalStateException(
                                      "Mirror projection repository is not registered in %s.",
                                      system.getName().getValue()
                              )
                      );
        Iterator<Any> result = repository.execute(query);
        return result;
    }
}
