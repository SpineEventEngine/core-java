/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.protobuf.Messages;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;

import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.CommandContext.Schedule;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Timestamps2.isBetween;
import static io.spine.protobuf.Timestamps2.isLaterThan;
import static io.spine.validate.Validate.isNotDefault;

/**
 * Client-side utilities for working with commands.
 */
public final class Commands {

    private static final Stringifier<CommandId> idStringifier = new CommandIdStringifier();
    private static final String COMMAND_ID_CANNOT_BE_EMPTY = "Command ID cannot be empty.";

    static {
        StringifierRegistry.instance()
                           .register(idStringifier(), CommandId.class);
    }

    /** Prevent instantiation of this utility class. */
    private Commands() {
    }

    /**
     * Creates a new {@link CommandId} based on random UUID.
     *
     * @return new command ID
     */
    public static CommandId generateId() {
        return Identifier.generate(CommandId.class);
    }

    /**
     * Extracts the message from the passed {@code Command} instance.
     *
     * @param command a command to extract a message from
     * @return an unpacked message
     */
    public static CommandMessage getMessage(Command command) {
        checkNotNull(command);
        CommandMessage result = (CommandMessage) unpack(command.getMessage());
        return result;
    }

    /**
     * Extracts a command message if the passed instance is a {@link Command} object or
     * {@link com.google.protobuf.Any Any}, otherwise returns the passed message.
     */
    public static CommandMessage ensureMessage(Message commandOrMessage) {
        checkNotNull(commandOrMessage);
        if (commandOrMessage instanceof Command) {
            return getMessage((Command) commandOrMessage);
        }
        CommandMessage unpacked = (CommandMessage) Messages.ensureMessage(commandOrMessage);
        return unpacked;
    }

    /**
     * Obtains a tenant ID from the command.
     */
    public static TenantId getTenantId(Command command) {
        checkNotNull(command);
        TenantId result = getTenantId(command.getContext());
        return result;
    }

    /**
     * Obtains a {@link TenantId} from the {@link CommandContext}.
     *
     * <p>The {@link CommandContext} is accessible from the {@link Event} if the {@code Event} was 
     * created as a result of some command or its rejection. This makes the {@code CommandContext}
     * a valid {@code TenantId} source inside of the {@code Event}.
     */
    @Internal
    public static TenantId getTenantId(CommandContext context) {
        return context.getActorContext()
                      .getTenantId();
    }

    /**
     * Creates a predicate for filtering commands created after the passed timestamp.
     */
    public static Predicate<Command> wereAfter(Timestamp from) {
        checkNotNull(from);
        return request -> {
            checkNotNull(request);
            Timestamp timestamp = getTimestamp(request);
            return isLaterThan(timestamp, from);
        };
    }

    /**
     * Creates a predicate for filtering commands created withing given time range.
     */
    public static Predicate<Command> wereWithinPeriod(Timestamp from, Timestamp to) {
        checkNotNull(from);
        checkNotNull(to);
        return request -> {
            checkNotNull(request);
            Timestamp timestamp = getTimestamp(request);
            return isBetween(timestamp, from, to);
        };
    }

    private static Timestamp getTimestamp(Command request) {
        checkNotNull(request);
        Timestamp result = request.getContext()
                                  .getActorContext()
                                  .getTimestamp();
        return result;
    }

    /**
     * Sorts the command given command request list by command timestamp value.
     *
     * @param commands the command list to sort
     */
    public static void sort(List<Command> commands) {
        checkNotNull(commands);
        commands.sort((o1, o2) -> {
            Timestamp timestamp1 = getTimestamp(o1);
            Timestamp timestamp2 = getTimestamp(o2);
            return Timestamps.compare(timestamp1, timestamp2);
        });
    }

    /**
     * Checks if the command is scheduled to be delivered later.
     *
     * @param command a command to check
     * @return {@code true} if the command context has a scheduling option set,
     * {@code false} otherwise
     */
    public static boolean isScheduled(Command command) {
        checkNotNull(command);
        Schedule schedule = command.getContext()
                                   .getSchedule();
        Duration delay = schedule.getDelay();
        if (isNotDefault(delay)) {
            checkArgument(delay.getSeconds() > 0,
                          "Command delay seconds must be a positive value.");
            return true;
        }
        return false;
    }

    /**
     * Tests whether both command contexts are from the same actor
     * working under the same tenant.
     */
    @VisibleForTesting
    static boolean sameActorAndTenant(CommandContext c1, CommandContext c2) {
        checkNotNull(c1);
        checkNotNull(c2);
        ActorContext a1 = c1.getActorContext();
        ActorContext a2 = c2.getActorContext();
        return a1.getActor()
                 .equals(a2.getActor()) &&
               a1.getTenantId()
                 .equals(a2.getTenantId());
    }

    /**
     * Obtains stringifier for command IDs.
     */
    public static Stringifier<CommandId> idStringifier() {
        return idStringifier;
    }

    /**
     * Ensures that the passed ID is valid.
     *
     * @param id an ID to check
     * @throws IllegalArgumentException if the ID string value is empty or blank
     */
    public static CommandId checkValid(CommandId id) {
        checkNotNull(id);
        String idStr = Identifier.toString(id);
        checkArgument(!Identifier.isEmpty(idStr), "Command ID must not be an empty string.");
        return id;
    }

    /**
     * Validates the passed command ID.
     */
    @Internal
    public static List<ConstraintViolation> validateId(CommandId id) {
        MessageValidator validator = MessageValidator.newInstance(id);
        List<ConstraintViolation> violations = validator.validate();
        if (id.getUuid().isEmpty()) {
            return ImmutableList.<ConstraintViolation>builder()
                    .addAll(violations)
                    .add(ConstraintViolation
                                 .newBuilder()
                                 .setMsgFormat(COMMAND_ID_CANNOT_BE_EMPTY)
                                 .build())
                    .build();
        }
        return violations;
    }

    /**
     * The stringifier for command IDs.
     */
    static class CommandIdStringifier extends Stringifier<CommandId> {
        @Override
        protected String toString(CommandId commandId) {
            String result = commandId.getUuid();
            return result;
        }

        @Override
        protected CommandId fromString(String str) {
            CommandId result = CommandId
                    .newBuilder()
                    .setUuid(str)
                    .build();
            return result;
        }
    }
}
