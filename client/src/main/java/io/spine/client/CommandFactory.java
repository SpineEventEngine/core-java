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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Commands;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.protobuf.AnyPacker;
import io.spine.time.ZoneOffset;
import io.spine.validate.ValidationException;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.validate.Validate.checkValid;

/**
 * A factory of {@link Command} instances.
 *
 * <p>Uses the given {@link ActorRequestFactory} as the source of the command meta information,
 * such as the actor, the tenant, etc.
 *
 * <p>The command messages passed to the factory are
 * {@linkplain io.spine.validate.Validate#checkValid(Message) validated} according to their
 * Proto definitions. If a given message is invalid, a {@link ValidationException} is thrown.
 *
 * @see ActorRequestFactory#command()
 */
public final class CommandFactory {

    private final ActorRequestFactory actorRequestFactory;

    CommandFactory(ActorRequestFactory actorRequestFactory) {
        this.actorRequestFactory = checkNotNull(actorRequestFactory);
    }

    /**
     * Creates a new {@link Command} with the given message.
     *
     * @param message the command message
     * @return new command instance
     * @throws ValidationException if the passed message does not satisfy the constraints
     *                             set for it in its Protobuf definition
     */
    public Command create(Message message) throws ValidationException {
        checkNotNull(message);
        checkValid(message);

        CommandContext context = createContext();
        Command result = createCommand(message, context);
        return result;
    }

    /**
     * Creates a new {@code Command} with the passed message and target entity version.
     *
     * <p>The {@code targetVersion} parameter defines the version of the entity which handles
     * the resulting command. Note that the framework performs no validation of the target version
     * before a command is handled. The validation may be performed by the user themselves instead.
     *
     * @param message       the command message
     * @param targetVersion the version of the entity for which this command is intended
     * @return new command instance
     * @throws ValidationException if the passed message does not satisfy the constraints
     *                             set for it in its Protobuf definition
     */
    public Command create(Message message, int targetVersion) throws ValidationException {
        checkNotNull(message);
        checkValid(message);

        CommandContext context = createContext(targetVersion);
        Command result = createCommand(message, context);
        return result;
    }

    /**
     * Creates a new {@code Command} with the passed {@code message} and {@code context}.
     *
     * @param message the command message
     * @param context the command context
     * @return a new command instance
     * @throws ValidationException if the passed message does not satisfy the constraints
     *                             set for it in its Protobuf definition
     */
    @Internal
    public Command createWithContext(Message message, CommandContext context)
            throws ValidationException {
        checkNotNull(message);
        checkNotNull(context);
        checkValid(message);

        Command result = createCommand(message, context);
        return result;
    }

    /**
     * Creates new {@code Command} with the passed message, using the existing context.
     *
     * <p>The produced command is created with a {@code CommandContext} instance, copied from
     * the given one, but with the current time set as a context timestamp.
     *
     * @param message the command message
     * @param context the command context to use as a base for the new command
     * @return new command instance
     * @throws ValidationException if the passed message does not satisfy the constraints
     *                             set for it in its Protobuf definition
     */
    @Internal
    public Command createBasedOnContext(Message message, CommandContext context)
            throws ValidationException {
        checkNotNull(message);
        checkNotNull(context);
        checkValid(message);

        CommandContext newContext = contextBasedOn(context);

        Command result = createCommand(message, newContext);
        return result;
    }

    /**
     * Creates a command instance with the given {@code message} and {@code context}.
     *
     * <p>If an instance of {@link Any} is passed as the {@code message} parameter, the packed
     * message is used for the command construction.
     *
     * <p>The ID of the new command instance is automatically generated.
     *
     * @param message the command message
     * @param context the context of the command
     * @return a new command
     */
    private static Command createCommand(Message message, CommandContext context) {
        Any packed = AnyPacker.pack(message);
        Command.Builder result = Command
                .newBuilder()
                .setId(Commands.generateId())
                .setMessage(packed)
                .setContext(context);
        return result.build();
    }

    /**
     * Creates command context for a new command.
     */
    @VisibleForTesting
    CommandContext createContext() {
        return createContext(actorRequestFactory.getTenantId(),
                             actorRequestFactory.getActor(),
                             actorRequestFactory.getZoneOffset());
    }

    /**
     * Creates command context for a new command with entity ID.
     */
    private CommandContext createContext(int targetVersion) {
        return createContext(actorRequestFactory.getTenantId(),
                             actorRequestFactory.getActor(),
                             actorRequestFactory.getZoneOffset(), targetVersion);
    }

    /**
     * Creates a new command context with the current time.
     *
     * @param tenantId   the ID of the tenant or {@code null} for single-tenant applications
     * @param userId     the actor ID
     * @param zoneOffset the offset of the timezone in which the user works
     * @return new {@code CommandContext}
     * @see CommandFactory#create(Message)
     */
    private static CommandContext createContext(@Nullable TenantId tenantId,
                                                UserId userId,
                                                ZoneOffset zoneOffset) {
        CommandContext.Builder result = newContextBuilder(tenantId, userId, zoneOffset);
        return result.build();
    }

    /**
     * Creates a new command context with the given parameters and
     * {@link io.spine.base.Time#getCurrentTime() current time} as the {@code timestamp}.
     *
     * @param tenantId      the ID of the tenant or {@code null} for single-tenant applications
     * @param userId        the actor id
     * @param zoneOffset    the offset of the timezone in which the user works
     * @param targetVersion the version of the entity for which this command is intended
     * @return new {@code CommandContext}
     * @see CommandFactory#create(Message)
     */
    @VisibleForTesting
    static CommandContext createContext(@Nullable TenantId tenantId,
                                        UserId userId,
                                        ZoneOffset zoneOffset,
                                        int targetVersion) {
        CommandContext.Builder builder = newContextBuilder(tenantId, userId, zoneOffset);
        CommandContext result = builder.setTargetVersion(targetVersion)
                                       .build();
        return result;
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private static CommandContext.Builder newContextBuilder(@Nullable TenantId tenantId,
                                                            UserId userId,
                                                            ZoneOffset zoneOffset) {
        ActorContext.Builder actorContext = ActorContext
                .newBuilder()
                .setActor(userId)
                .setTimestamp(getCurrentTime())
                .setZoneOffset(zoneOffset);
        if (tenantId != null) {
            actorContext.setTenantId(tenantId);
        }

        CommandContext.Builder result = CommandContext
                .newBuilder()
                .setActorContext(actorContext);
        return result;
    }

    /**
     * Creates a new instance of {@code CommandContext} based on the passed one.
     *
     * <p>The returned instance gets new {@code timestamp} set to
     * the {@link io.spine.base.Time#getCurrentTime() current time}.
     *
     * @param value the instance from which to copy values
     * @return new {@code CommandContext}
     */
    private static CommandContext contextBasedOn(CommandContext value) {
        ActorContext.Builder withCurrentTime =
                value.getActorContext()
                     .toBuilder()
                     .setTimestamp(getCurrentTime());
        CommandContext.Builder result =
                value.toBuilder()
                     .setActorContext(withCurrentTime);
        return result.build();
    }
}
