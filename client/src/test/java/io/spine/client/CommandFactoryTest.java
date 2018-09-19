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

import com.google.protobuf.Timestamp;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.TenantId;
import io.spine.test.commands.CmdCreateProject;
import io.spine.validate.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.client.given.ActorRequestFactoryTestEnv.ACTOR;
import static io.spine.client.given.ActorRequestFactoryTestEnv.ZONE_OFFSET;
import static io.spine.client.given.ActorRequestFactoryTestEnv.requestFactory;
import static io.spine.client.given.ActorRequestFactoryTestEnv.requestFactoryBuilder;
import static io.spine.client.given.CommandFactoryTestEnv.INVALID_COMMAND;
import static io.spine.time.Timestamps2.isBetween;
import static io.spine.time.testing.TimeTests.Future.secondsFromNow;
import static io.spine.time.testing.TimeTests.Past.secondsAgo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("Command factory should")
class CommandFactoryTest {

    private CommandFactory factory;

    @BeforeEach
    void createFactory() {
        factory = requestFactory().command();
    }

    @Nested
    @DisplayName("create command")
    class CreateCommand {

        /**
         * Tests that a command is created with the current time.
         *
         * @implNote We are creating a range of +/- second between the call to make sure the
         *         timestamp
         *         would fit into this range. This way the test the test ensures the sub-second
         *         precision
         *         of timestamps, which is enough for the purpose of this test.
         */
        @Test
        @DisplayName("with current time")
        void withTimestamp() {
            Timestamp beforeCall = secondsAgo(1);
            Command command = factory.create(CmdCreateProject.getDefaultInstance());
            Timestamp afterCall = secondsFromNow(1);

            Timestamp timestamp = command.getContext()
                                         .getActorContext()
                                         .getTimestamp();
            assertTrue(isBetween(timestamp, beforeCall, afterCall));
        }

        @Test
        @DisplayName("with given entity version")
        void withEntityVersion() {
            Command command = factory.create(CmdCreateProject.getDefaultInstance(), 2);

            CommandContext context = command.getContext();
            assertEquals(2, context.getTargetVersion());
        }

        @Test
        @DisplayName("with own tenant ID")
        void withOwnTenantId() {
            TenantId tenantId = TenantId
                    .newBuilder()
                    .setValue(getClass().getSimpleName())
                    .build();
            ActorRequestFactory mtFactory = requestFactoryBuilder()
                    .setTenantId(tenantId)
                    .setActor(ACTOR)
                    .setZoneOffset(ZONE_OFFSET)
                    .build();
            Command command = mtFactory.command()
                                       .create(CmdCreateProject.getDefaultInstance());
            assertEquals(tenantId, command.getContext()
                                          .getActorContext()
                                          .getTenantId());
        }

    }

    @Nested
    @DisplayName("throw ValidationException when creating command")
    class NotAccept {

        @Test
        @DisplayName("from invalid Message")
        void invalidMessage() {
            assertThrows(ValidationException.class, () -> factory.create(INVALID_COMMAND));
        }

        @Test
        @DisplayName("from invalid Message with version")
        void invalidMessageWithVersion() {
            assertThrows(ValidationException.class, () -> factory.create(INVALID_COMMAND, 42));
        }
    }
}
