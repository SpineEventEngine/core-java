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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.ActorContext;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.Commands;
import io.spine.protobuf.AnyPacker;
import io.spine.time.ZoneOffset;
import io.spine.users.TenantId;
import io.spine.users.UserId;
import io.spine.validate.ConstraintViolationThrowable;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.time.Time.getCurrentTime;
import static io.spine.validate.Validate.checkValid;

/**
 * Public API for creating {@link Command} instances, using the {@code ActorRequestFactory}
 * configuration.
 *
 * <p>During the creation of {@code Command} instances the source {@code Message} instances, passed
 * into creation methods, are validated. The validation is performed according to the constraints
 * set in Protobuf definition of each {@code Message}. In case the message isn't valid,
 * an {@linkplain ConstraintViolationThrowable exception} is thrown.
 *
 * <p>Therefore it is recommended to use a corresponding
 * {@linkplain io.spine.validate.ValidatingBuilder ValidatingBuilder} implementation to create
 * a command message.
 *
 * @see ActorRequestFactory#command()
 */
public final class CommandFactory {

    private final ActorRequestFactory actorRequestFactory;

    CommandFactory(ActorRequestFactory actorRequestFactory) {
        this.actorRequestFactory = checkNotNull(actorRequestFactory);
    }

    /**
     * Creates new {@code Command} with the passed message.
     *
     * <p>The command contains a {@code CommandContext} instance with the current time.
     *
     * @param message the command message
     * @return new command instance
     * @throws ConstraintViolationThrowable if the passed message does not satisfy the constraints
     *                                      set for it in its Protobuf definition
     */
    public Command create(Message message) throws ConstraintViolationThrowable {
        checkNotNull(message);
        checkValid(message);

        final CommandContext context = createContext();
        final Command result = createCommand(message, context);
        return result;
    }

    /**
     * Creates new {@code Command} with the passed message and target entity version.
     *
     * <p>The command contains a {@code CommandContext} instance with the current time.
     *
     * <p>The message passed is validated according to the constraints set in its Protobuf
     * definition. In case the message isn't valid, an {@linkplain ConstraintViolationThrowable
     * exception} is thrown.
     *
     * @param message       the command message
     * @param targetVersion the ID of the entity for applying commands if {@code null}
     *                      the commands can be applied to any entity
     * @return new command instance
     * @throws ConstraintViolationThrowable if the passed message does not satisfy the constraints
     *                                      set for it in its Protobuf definition
     */
    public Command create(Message message, int targetVersion) throws ConstraintViolationThrowable {
        checkNotNull(message);
        checkNotNull(targetVersion);
        checkValid(message);

        final CommandContext context = createContext(targetVersion);
        final Command result = createCommand(message, context);
        return result;
    }

    /**
     * Creates new {@code Command} with the passed {@code message} and {@code context}.
     *
     * <p>The timestamp of the resulting command is the <i>same</i> as in
     * the passed {@code CommandContext}.
     *
     * @param message the command message
     * @param context the command context
     * @return a new command instance
     * @throws ConstraintViolationThrowable if the passed message does not satisfy the constraints
     *                                      set for it in its Protobuf definition
     */
    @Internal
    public Command createWithContext(Message message, CommandContext context)
            throws ConstraintViolationThrowable {
        checkNotNull(message);
        checkNotNull(context);
        checkValid(message);

        final Command result = createCommand(message, context);
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
     * @throws ConstraintViolationThrowable if the passed message does not satisfy the constraints
     *                                      set for it in its Protobuf definition
     */
    @Internal
    public Command createBasedOnContext(Message message, CommandContext context)
            throws ConstraintViolationThrowable {
        checkNotNull(message);
        checkNotNull(context);
        checkValid(message);

        final CommandContext newContext = contextBasedOn(context);

        final Command result = createCommand(message, newContext);
        return result;
    }

    /**
     * Creates a command instance with the given {@code message} and the {@code context}.
     *
     * <p>If {@code Any} instance is passed as the first parameter it will be used as is.
     * Otherwise, the command message will be packed into {@code Any}.
     *
     * <p>The ID of the new command instance is automatically generated.
     *
     * @param message the command message
     * @param context the context of the command
     * @return a new command
     */
    private static Command createCommand(Message message, CommandContext context) {
        checkNotNull(message);
        checkNotNull(context);

        final Any packed = AnyPacker.pack(message);
        final Command.Builder result = Command.newBuilder()
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
        checkNotNull(userId);
        checkNotNull(zoneOffset);

        final CommandContext.Builder result = newContextBuilder(tenantId, userId, zoneOffset);
        return result.build();
    }

    /**
     * Creates a new command context with the current time.
     *
     * @param tenantId      the ID of the tenant or {@code null} for single-tenant applications
     * @param userId        the actor id
     * @param zoneOffset    the offset of the timezone in which the user works
     * @param targetVersion the the ID of the entity for applying commands
     * @return new {@code CommandContext}
     * @see CommandFactory#create(Message)
     */
    @VisibleForTesting
    static CommandContext createContext(@Nullable TenantId tenantId,
                                        UserId userId,
                                        ZoneOffset zoneOffset,
                                        int targetVersion) {
        checkNotNull(userId);
        checkNotNull(zoneOffset);
        checkNotNull(targetVersion);

        final CommandContext.Builder builder = newContextBuilder(tenantId, userId, zoneOffset);
        final CommandContext result = builder.setTargetVersion(targetVersion)
                                             .build();
        return result;
    }

    private static CommandContext.Builder newContextBuilder(@Nullable TenantId tenantId,
                                                            UserId userId,
                                                            ZoneOffset zoneOffset) {
        final ActorContext.Builder actorContext = ActorContext.newBuilder()
                                                              .setActor(userId)
                                                              .setTimestamp(getCurrentTime())
                                                              .setZoneOffset(zoneOffset);
        if (tenantId != null) {
            actorContext.setTenantId(tenantId);
        }

        final CommandContext.Builder result = CommandContext.newBuilder()
                                                            .setActorContext(actorContext);
        return result;
    }

    /**
     * Creates a new instance of {@code CommandContext} based on the passed one.
     *
     * <p>The returned instance gets new {@code timestamp} set to the time of the call.
     *
     * @param value the instance from which to copy values
     * @return new {@code CommandContext}
     */
    private static CommandContext contextBasedOn(CommandContext value) {
        checkNotNull(value);
        final ActorContext.Builder withCurrentTime = value.getActorContext()
                                                          .toBuilder()
                                                          .setTimestamp(getCurrentTime());
        final CommandContext.Builder result = value.toBuilder()
                                                   .setActorContext(withCurrentTime);
        return result.build();
    }
}
