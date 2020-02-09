/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.protobuf.StringValue;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.test.mixin.command.MixinCreateProject;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;

public final class CoreMixinsTestEnv {

    /**
     * Prevents instantiation of this class.
     */
    private CoreMixinsTestEnv() {
    }

    public static MessageId messageId() {
        Event event = event();
        return MessageId
                .newBuilder()
                .setId(pack(event.getId()))
                .setTypeUrl(event.typeUrl().value())
                .setVersion(event.getContext()
                                 .getVersion())
                .build();
    }

    public static Command command() {
        TestActorRequestFactory factory = new TestActorRequestFactory(CoreMixinsTestEnv.class);
        MixinCreateProject cmdMessage = MixinCreateProject
                .newBuilder()
                .setProjectId(newUuid().hashCode())
                .setName("A Project")
                .build();
        return factory.createCommand(cmdMessage);
    }

    public static Event event() {
        TestEventFactory factory = TestEventFactory.newInstance(CoreMixinsTestEnv.class);
        StandardRejections.EntityAlreadyDeleted msg = StandardRejections.EntityAlreadyDeleted
                .newBuilder()
                .setEntityId(
                        pack(StringValue.of("deleted-entity-id")))
                .build();
        return factory.createEvent(msg);
    }
}
