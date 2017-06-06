/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.CommandId;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.users.TenantId;
import io.spine.users.UserId;

import static io.spine.test.Tests.newUserId;

/**
 * The command factory, which allows generating commands as if the were
 * created at the specified moments in time.
 *
 * @author Alexaner Yevsyukov
 */
@Internal
@VisibleForTesting
public class TestActorRequestFactory extends ActorRequestFactory {

    protected TestActorRequestFactory(UserId actor, ZoneOffset zoneOffset) {
        super(newBuilder().setActor(actor)
                          .setZoneOffset(zoneOffset));
    }

    protected TestActorRequestFactory(UserId actor, ZoneOffset zoneOffset, TenantId tenantId) {
        super(newBuilder().setActor(actor)
                          .setZoneOffset(zoneOffset)
                          .setTenantId(tenantId));
    }

    public static TestActorRequestFactory newInstance(String actor, ZoneOffset zoneOffset) {
        return newInstance(newUserId(actor), zoneOffset);
    }

    public static TestActorRequestFactory newInstance(UserId actor, ZoneOffset zoneOffset) {
        return new TestActorRequestFactory(actor, zoneOffset);
    }

    public static TestActorRequestFactory newInstance(Class<?> testClass) {
        return newInstance(testClass.getName(), ZoneOffsets.UTC);
    }

    public static TestActorRequestFactory newInstance(UserId actor) {
        return newInstance(actor, ZoneOffsets.UTC);
    }

    public static TestActorRequestFactory newInstance(UserId actor, TenantId tenantId) {
        return new TestActorRequestFactory(actor, ZoneOffsets.UTC, tenantId);
    }

    public static TestActorRequestFactory newInstance(Class<?> testClass, TenantId tenantId) {
        return new TestActorRequestFactory(newUserId(testClass.getName()),
                                           ZoneOffsets.UTC, tenantId);
    }

    /** Creates new command with the passed timestamp. */
    public Command createCommand(Message message, Timestamp timestamp) {
        final Command command = command().create(message);
        return TimeTests.adjustTimestamp(command, timestamp);
    }

    public Command createCommand(Message message) {
        final Command command = command().create(message);
        return command;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open access to creating command contexts in tests.
     */
    @Override
    public CommandContext createCommandContext() {
        return super.createCommandContext();
    }

    public CommandId createCommandId() {
        final String uid = Identifier.newUuid();
        final CommandId commandId = CommandId.newBuilder()
                                             .setUuid(uid)
                                             .build();
        return commandId;
    }
}
