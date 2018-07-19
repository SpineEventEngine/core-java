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

package io.spine.core.given;

import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.RejectionContext;
import io.spine.core.TenantId;

/**
 * @author Mykhailo Drachuk
 */
public class EventsTestEnv {

    private EventsTestEnv() {
    }

    public static RejectionContext rejectionContext(TenantId id) {
        Command command = Command.newBuilder()
                                 .setContext(commandContext(id))
                                 .build();
        RejectionContext result = RejectionContext.newBuilder()
                                                  .setCommand(command)
                                                  .build();
        return result;
    }

    public static RejectionContext rejectionContext() {
        RejectionContext result = RejectionContext.newBuilder()
                                                  .build();
        return result;
    }

    public static CommandContext commandContext(TenantId id) {
        ActorContext actorContext = ActorContext.newBuilder()
                                                .setTenantId(id)
                                                .build();
        CommandContext result = CommandContext.newBuilder()
                                              .setActorContext(actorContext)
                                              .build();
        return result;
    }

    public static TenantId tenantId() {
        String value = EventsTestEnv.class.getName();
        return TenantId.newBuilder()
                       .setValue(value)
                       .build();
    }

    public static Event event(EventContext context) {
        return Event.newBuilder()
                    .setContext(context)
                    .build();
    }
}
