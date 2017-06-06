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

package io.spine.client;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.ActorContext;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.test.TimeTests;
import io.spine.test.commands.RequiredFieldCommand;
import io.spine.time.Timestamps2;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.users.TenantId;
import io.spine.users.UserId;
import io.spine.validate.ConstraintViolationThrowable;
import org.junit.Test;

import static io.spine.test.Tests.newTenantUuid;
import static io.spine.test.Tests.newUserUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandFactoryShould extends ActorRequestFactoryShould {

    @Test
    public void create_command_context() {
        final TenantId tenantId = newTenantUuid();
        final UserId userId = newUserUuid();
        final ZoneOffset zoneOffset = ZoneOffsets.ofHours(-3);
        final int targetVersion = 100500;

        final CommandContext commandContext = CommandFactory.createContext(tenantId,
                                                                           userId,
                                                                           zoneOffset,
                                                                           targetVersion);

        final ActorContext actorContext = commandContext.getActorContext();

        assertEquals(tenantId, actorContext.getTenantId());
        assertEquals(userId, actorContext.getActor());
        assertEquals(zoneOffset, actorContext.getZoneOffset());
        assertEquals(targetVersion, commandContext.getTargetVersion());
    }

    @Test
    public void create_new_instances_with_current_time() {
        // We are creating a range of +/- second between the call to make sure the timestamp
        // would fit into this range. The purpose of this test is to make sure it works with
        // this precision and to add coverage.
        final Timestamp beforeCall = TimeTests.Past.secondsAgo(1);
        final Command command = factory().command()
                                         .create(StringValue.getDefaultInstance());
        final Timestamp afterCall = TimeTests.Future.secondsFromNow(1);

        assertTrue(Timestamps2.isBetween(
                command.getContext()
                       .getActorContext()
                       .getTimestamp(), beforeCall, afterCall));
    }

    @Test
    public void create_new_instance_with_entity_version() {
        final Command command = factory().command()
                                         .create(StringValue.getDefaultInstance(), 2);

        assertEquals(2, command.getContext()
                               .getTargetVersion());
    }

    @Test
    public void set_tenant_ID_in_commands_when_created_with_tenant_ID() {
        final TenantId tenantId = TenantId.newBuilder()
                                          .setValue(getClass().getSimpleName())
                                          .build();
        final ActorRequestFactory mtFactory = ActorRequestFactory.newBuilder()
                                                                 .setTenantId(tenantId)
                                                                 .setActor(getActor())
                                                                 .setZoneOffset(getZoneOffset())
                                                                 .build();
        final Command command = mtFactory.command()
                                         .create(StringValue.getDefaultInstance());

        assertEquals(tenantId, command.getContext()
                                      .getActorContext()
                                      .getTenantId());
    }

    @Test(expected = ConstraintViolationThrowable.class)
    public void throw_ConstraintViolation_exception_once_passed_invalid_Message() {
        final RequiredFieldCommand invalidCommand = RequiredFieldCommand.getDefaultInstance();
        factory().command().create(invalidCommand);
    }

    @Test(expected = ConstraintViolationThrowable.class)
    public void throw_ConstraintViolation_exception_once_passed_invalid_Message_with_version() {
        final RequiredFieldCommand invalidCommand = RequiredFieldCommand.getDefaultInstance();
        factory().command().create(invalidCommand, 42);
    }
}
