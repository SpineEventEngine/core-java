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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.test.TestValues;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;

import static io.spine.core.given.GivenUserId.of;

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
        super(ActorRequestFactory.newBuilder().setActor(actor)
                                 .setZoneOffset(zoneOffset));
    }

    protected TestActorRequestFactory(UserId actor, ZoneOffset zoneOffset, TenantId tenantId) {
        super(ActorRequestFactory.newBuilder().setActor(actor)
                                 .setZoneOffset(zoneOffset)
                                 .setTenantId(tenantId));
    }

    public static TestActorRequestFactory newInstance(String actor, ZoneOffset zoneOffset) {
        return newInstance(of(actor), zoneOffset);
    }

    public static TestActorRequestFactory newInstance(UserId actor, ZoneOffset zoneOffset) {
        return new TestActorRequestFactory(actor, zoneOffset);
    }

    public static TestActorRequestFactory newInstance(Class<?> testClass) {
        return newInstance(testClass.getName(), ZoneOffsets.utc());
    }

    public static TestActorRequestFactory newInstance(UserId actor) {
        return newInstance(actor, ZoneOffsets.utc());
    }

    public static TestActorRequestFactory newInstance(UserId actor, TenantId tenantId) {
        return new TestActorRequestFactory(actor, ZoneOffsets.utc(), tenantId);
    }

    public static TestActorRequestFactory newInstance(Class<?> testClass, TenantId tenantId) {
        return new TestActorRequestFactory(of(testClass.getName()),
                                           ZoneOffsets.utc(), tenantId);
    }

    /** Creates new command with the passed timestamp. */
    public Command createCommand(Message message, Timestamp timestamp) {
        final Command command = command().create(message);
        return withTimestamp(command, timestamp);
    }

    private static Command withTimestamp(Command command, Timestamp timestamp) {
        final CommandContext context = command.getContext();
        final ActorContext.Builder withTime = context.getActorContext()
                                                     .toBuilder()
                                                     .setTimestamp(timestamp);
        final Command.Builder commandBuilder =
                command.toBuilder()
                       .setContext(context.toBuilder()
                                          .setActorContext(withTime));
        return commandBuilder.build();
    }

    public Command createCommand(Message message) {
        final Command command = command().create(message);
        return command;
    }

    public CommandEnvelope createEnvelope(Message message) {
        return CommandEnvelope.of(createCommand(message));
    }

    /**
     * Generates a test instance of a command based on {@link StringValue}
     */
    public Command generateCommand() {
        return generate();
    }

    /**
     * Generates a command and wraps it into envelope.
     */
    public CommandEnvelope generateEnvelope() {
        final Command command = generateCommand();
        final CommandEnvelope result = CommandEnvelope.of(command);
        return result;
    }

    private Command generate() {
        final String randomSuffix = String.valueOf(TestValues.random(10_000));
        final StringValue msg = StringValue.newBuilder()
                                           .setValue("GeneratedTestCommand" + randomSuffix)
                                           .build();
        return createCommand(msg);
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
