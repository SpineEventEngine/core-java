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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.CommandFactory;
import io.spine.client.Query;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.route.EventRoute;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The point of integration of the domain and the system bounded context.
 *
 * <p>All the facilities provided by the system bounded context are available through this gateway.
 *
 * @author Dmytro Dashenkov
 */
final class DefaultSystemGateway implements SystemGateway {

    /**
     * The ID of the user which is used for generating system commands and events.
     */
    static final UserId SYSTEM_USER = UserId
            .newBuilder()
            .setValue("SYSTEM")
            .build();

    private final BoundedContext system;

    DefaultSystemGateway(BoundedContext system) {
        this.system = system;
    }

    @Override
    public void postCommand(Message systemCommand) {
        checkNotNull(systemCommand);
        CommandFactory commandFactory = SystemCommandFactory.newInstance(system.isMultitenant());
        Command command = commandFactory.create(systemCommand);
        system.getCommandBus()
              .post(command, noOpObserver());
    }

    @Override
    public void postEvent(Message systemEvent) {
        checkNotNull(systemEvent);
        Message aggregateId = getAggregateId(systemEvent);

        SystemEventFactory factory = new SystemEventFactory(aggregateId, system.isMultitenant());
        Event event = factory.createEvent(systemEvent, null);
        system.getImportBus()
              .post(event, noOpObserver());
    }

    @Override
    public Iterable<Any> read(Query query) {
        @SuppressWarnings("unchecked") // Logically checked.
        MirrorRepository repository = (MirrorRepository)
                system.findRepository(Mirror.class)
                      .orElseThrow(
                              () -> newIllegalStateException(
                                      "Mirror projection repository is not registered in %s.",
                                      system.getName().getValue()
                              )
                      );
        Iterable<Any> result = repository.execute(query);
        return result;
    }

    private static Message getAggregateId(Message systemEvent) {
        Set<Object> routingOut =
                EventRoute.byFirstMessageField()
                          .apply(systemEvent, EventContext.getDefaultInstance());
        checkArgument(routingOut.size() == 1,
                      "System event message must have aggregate ID in the first field.");
        Object id = routingOut.iterator()
                              .next();
        checkArgument(id instanceof Message, "System aggregate ID must be a Message");
        return (Message) id;
    }

    @VisibleForTesting
    BoundedContext target() {
        return system;
    }
}
