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

package io.spine.testing.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Timestamp;
import io.spine.base.CommandMessage;
import io.spine.client.ActorRequestFactory;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.testing.TestValues;
import io.spine.testing.client.command.TestCommandMessage;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;

import static io.spine.testing.core.given.GivenUserId.of;

/**
 * An {@code ActorRequestFactory} for running tests.
 */
@VisibleForTesting
public class TestActorRequestFactory extends ActorRequestFactory {

    protected TestActorRequestFactory(UserId actor, ZoneOffset zoneOffset, ZoneId zoneId) {
        super(ActorRequestFactory
                      .newBuilder()
                      .setActor(actor)
                      .setZoneOffset(zoneOffset)
                      .setZoneId(zoneId)
        );
    }

    protected TestActorRequestFactory(TenantId tenantId,
                                      UserId actor,
                                      ZoneOffset zoneOffset,
                                      ZoneId zoneId) {
        super(ActorRequestFactory
                      .newBuilder()
                      .setTenantId(tenantId)
                      .setActor(actor)
                      .setZoneOffset(zoneOffset)
                      .setZoneId(zoneId)
        );
    }

    public static
    TestActorRequestFactory newInstance(String actor, ZoneOffset zoneOffset, ZoneId zoneId) {
        return newInstance(of(actor), zoneOffset, zoneId);
    }

    public static
    TestActorRequestFactory newInstance(UserId actor, ZoneOffset zoneOffset, ZoneId zoneId) {
        return new TestActorRequestFactory(actor, zoneOffset, zoneId);
    }

    public static TestActorRequestFactory newInstance(Class<?> testClass) {
        return newInstance(testClass.getName(), ZoneOffsets.getDefault(), ZoneIds.systemDefault());
    }

    public static TestActorRequestFactory newInstance(UserId actor) {
        return newInstance(actor, ZoneOffsets.getDefault(), ZoneIds.systemDefault());
    }

    public static TestActorRequestFactory newInstance(UserId actor, TenantId tenantId) {
        return new TestActorRequestFactory(tenantId, actor,
                                           ZoneOffsets.getDefault(),
                                           ZoneIds.systemDefault());
    }

    public static TestActorRequestFactory newInstance(Class<?> testClass, TenantId tenantId) {
        return new TestActorRequestFactory(tenantId,
                                           of(testClass.getName()),
                                           ZoneOffsets.getDefault(),
                                           ZoneIds.systemDefault());
    }

    /** Creates new command with the passed timestamp. */
    public Command createCommand(CommandMessage message, Timestamp timestamp) {
        Command command = command().create(message);
        return withTimestamp(command, timestamp);
    }

    private static Command withTimestamp(Command command, Timestamp timestamp) {
        CommandContext context = command.getContext();
        ActorContext.Builder withTime = context.getActorContext()
                                               .toBuilder()
                                               .setTimestamp(timestamp);
        Command.Builder commandBuilder =
                command.toBuilder()
                       .setContext(context.toBuilder()
                                          .setActorContext(withTime));
        return commandBuilder.build();
    }

    public Command createCommand(CommandMessage message) {
        Command command = command().create(message);
        return command;
    }

    public CommandEnvelope createEnvelope(CommandMessage message) {
        return CommandEnvelope.of(createCommand(message));
    }

    /**
     * Generates a test instance of a command with the message
     * {@link io.spine.testing.client.command.TestCommandMessage TestCommandMessage}.
     */
    public Command generateCommand() {
        @SuppressWarnings("MagicNumber")
        String randomSuffix = String.format("%04d", TestValues.random(10_000));
        TestCommandMessage msg = TestCommandMessage
                .newBuilder()
                .setId("random-number-" + randomSuffix)
                .build();
        return createCommand(msg);
    }

    /**
     * Generates a command and wraps it into envelope.
     */
    public CommandEnvelope generateEnvelope() {
        Command command = generateCommand();
        CommandEnvelope result = CommandEnvelope.of(command);
        return result;
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
}
