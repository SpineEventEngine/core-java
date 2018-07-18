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

package io.spine.client;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.test.commands.RequiredFieldCommand;
import io.spine.testing.core.given.GivenTenantId;
import io.spine.testing.core.given.GivenUserId;
import io.spine.time.Timestamps2;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.time.testing.TimeTests;
import io.spine.validate.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Command factory should")
class CommandFactoryTest extends ActorRequestFactoryTest {

    @Test
    @DisplayName("create command context for given parameters")
    void createCommandContext() {
        TenantId tenantId = GivenTenantId.newUuid();
        UserId userId = GivenUserId.newUuid();
        ZoneOffset zoneOffset = ZoneOffsets.ofHours(-3);
        int targetVersion = 100500;

        CommandContext commandContext =
                CommandFactory.createContext(tenantId, userId, zoneOffset, targetVersion);

        ActorContext actorContext = commandContext.getActorContext();

        assertEquals(tenantId, actorContext.getTenantId());
        assertEquals(userId, actorContext.getActor());
        assertEquals(zoneOffset, actorContext.getZoneOffset());
        assertEquals(targetVersion, commandContext.getTargetVersion());
    }

    @Nested
    @DisplayName("create command")
    class CreateCommand {

        @Test
        @DisplayName("with current time")
        void withTimestamp() {
            // We are creating a range of +/- second between the call to make sure the timestamp
            // would fit into this range. The purpose of this test is to make sure it works with
            // this precision and to add coverage.
            Timestamp beforeCall = TimeTests.Past.secondsAgo(1);
            Command command = factory().command()
                                       .create(StringValue.getDefaultInstance());
            Timestamp afterCall = TimeTests.Future.secondsFromNow(1);

            assertTrue(Timestamps2.isBetween(
                    command.getContext()
                           .getActorContext()
                           .getTimestamp(), beforeCall, afterCall)
            );
        }

        @Test
        @DisplayName("with given entity version")
        void withEntityVersion() {
            Command command = factory().command()
                                       .create(StringValue.getDefaultInstance(), 2);

            assertEquals(2, command.getContext()
                                   .getTargetVersion());
        }

        @Test
        @DisplayName("with own tenant ID")
        void withOwnTenantId() {
            TenantId tenantId = TenantId
                    .newBuilder()
                    .setValue(getClass().getSimpleName())
                    .build();
            ActorRequestFactory mtFactory = builder()
                    .setTenantId(tenantId)
                    .setActor(actor())
                    .setZoneOffset(zoneOffset())
                    .build();
            Command command = mtFactory.command()
                                       .create(StringValue.getDefaultInstance());

            assertEquals(tenantId, command.getContext()
                                          .getActorContext()
                                          .getTenantId());
        }
    }

    @Nested
    @DisplayName("throw ValidationException when creating command")
    class NotAccept {

        private final RequiredFieldCommand invalidCommand =
                RequiredFieldCommand.getDefaultInstance();

        @Test
        @DisplayName("from invalid Message")
        void invalidMessage() {
            assertThrows(ValidationException.class, () -> factory().command()
                                                                   .create(invalidCommand));
        }

        @Test
        @DisplayName("from invalid Message with version")
        void invalidMessageWithVersion() {
            assertThrows(ValidationException.class, () -> factory().command()
                                                                   .create(invalidCommand, 42));
        }
    }
}
