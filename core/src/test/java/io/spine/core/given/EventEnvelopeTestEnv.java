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

import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.ActorContext;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.TenantId;
import io.spine.protobuf.AnyPacker;
import io.spine.test.core.ProjectCreated;
import io.spine.test.core.ProjectId;

import static io.spine.base.Identifier.newUuid;

public class EventEnvelopeTestEnv {

    /** Prevents instantiation of this utility class. */
    private EventEnvelopeTestEnv() {
    }

    public static Event event(Message eventMessage) {
        return event(eventMessage, EventContext.getDefaultInstance());
    }

    public static Event event(Message eventMessage, EventContext eventContext) {
        EventId.Builder eventIdBuilder = EventId.newBuilder()
                                                .setValue(newUuid());
        return Event.newBuilder()
                    .setId(eventIdBuilder)
                    .setMessage(AnyPacker.pack(eventMessage))
                    .setContext(eventContext)
                    .build();
    }

    public static CommandContext commandContext() {
        return CommandContext.newBuilder()
                             .setActorContext(actorContext())
                             .build();
    }

    public static ActorContext actorContext() {
        TenantId tenantId = TenantId.newBuilder()
                                    .setValue(Identifier.newUuid())
                                    .build();
        return ActorContext.newBuilder()
                           .setTenantId(tenantId)
                           .build();
    }

    public static ProjectCreated eventMessage() {
        ProjectId projectId = ProjectId.newBuilder()
                                       .setId(newUuid())
                                       .build();
        return ProjectCreated.newBuilder()
                             .setProjectId(projectId)
                             .build();
    }

    public static EventContext eventContext(CommandContext commandContext) {
        return EventContext.newBuilder()
                           .setCommandContext(commandContext)
                           .build();
    }

    public static EventContext eventContext(EventContext eventContext) {
        return EventContext.newBuilder()
                           .setEventContext(eventContext)
                           .build();
    }

    public static EventContext eventContext(ActorContext importContext) {
        return EventContext.newBuilder()
                           .setImportContext(importContext)
                           .build();
    }
}
