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

package io.spine.system.server;

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.ActorContext;
import io.spine.core.EventContext;
import io.spine.server.aggregate.ImportOrigin;
import io.spine.server.event.EventFactory;
import io.spine.server.route.EventRoute;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.system.server.SystemCommandFactory.requestFactory;

/**
 * Creates events that will be imported into system aggregates.
 *
 * @author Alexander Yevsyukov
 */
final class SystemEventFactory extends EventFactory {

    private SystemEventFactory(Message aggregateId, boolean multitenant) {
        super(newOrigin(multitenant), Identifier.pack(aggregateId));
    }

    /**
     * Creates a new factory of system events.
     *
     * @param message
     *         the system event message
     * @param multitenant
     *         {@code true} if the current context environment is multitenant,
     *         {@code false} otherwise
     * @return new instance of {@code SystemEventFactory}
     */
    static SystemEventFactory forMessage(EventMessage message, boolean multitenant) {
        Message aggregateId = getAggregateId(message);
        return new SystemEventFactory(aggregateId, multitenant);
    }

    private static Message getAggregateId(EventMessage systemEvent) {
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

    private static ImportOrigin newOrigin(boolean multitenant) {
        ActorContext actorContext = requestFactory(multitenant).newActorContext();
        return ImportOrigin.newInstance(actorContext);
    }
}
