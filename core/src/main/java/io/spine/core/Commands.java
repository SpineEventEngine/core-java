/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Predicate;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.Identifier;
import io.spine.annotation.Internal;
import io.spine.base.ThrowableMessage;
import io.spine.protobuf.AnyPacker;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;
import io.spine.time.Timestamps2;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.Identifier.EMPTY_ID;
import static io.spine.core.CommandContext.Schedule;
import static io.spine.validate.Validate.isNotDefault;

/**
 * Client-side utilities for working with commands.
 *
 * @author Alexander Yevsyukov
 */
public final class Commands {

    private static final Stringifier<CommandId> idStringifier = new CommandIdStringifier();

    static {
        StringifierRegistry.getInstance()
                           .register(idStringifier(), CommandId.class);
    }

    private Commands() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a new {@link CommandId} based on random UUID.
     *
     * @return new command ID
     */
    public static CommandId generateId() {
        final String value = UUID.randomUUID()
                                 .toString();
        return CommandId.newBuilder()
                        .setUuid(value)
                        .build();
    }

    /**
     * Extracts the message from the passed {@code Command} instance.
     *
     * @param command a command to extract a message from
     * @param <M>     a type of the command message
     * @return an unpacked message
     */
    public static <M extends Message> M getMessage(Command command) {
        checkNotNull(command);
        final M result = AnyPacker.unpack(command.getMessage());
        return result;
    }

    /**
     * Extracts a command message if the passed instance is a {@link Command} object or
     * {@link com.google.protobuf.Any Any}, otherwise returns the passed message.
     */
    public static Message ensureMessage(Message commandOrMessage) {
        checkNotNull(commandOrMessage);
        if (commandOrMessage instanceof Command) {
            return getMessage((Command) commandOrMessage);
        }
        return io.spine.protobuf.Messages.ensureMessage(commandOrMessage);
    }

    /**
     * Obtains a tenant ID from the command.
     */
    public static TenantId getTenantId(Command command) {
        checkNotNull(command);
        final TenantId result = command.getContext()
                                       .getActorContext()
                                       .getTenantId();
        return result;
    }

    /**
     * Creates a predicate for filtering commands created after the passed timestamp.
     */
    public static Predicate<Command> wereAfter(final Timestamp from) {
        checkNotNull(from);
        return new Predicate<Command>() {
            @Override
            public boolean apply(@Nullable Command request) {
                checkNotNull(request);
                final Timestamp timestamp = getTimestamp(request);
                return Timestamps2.isLaterThan(timestamp, from);
            }
        };
    }

    /**
     * Creates a predicate for filtering commands created withing given timerange.
     */
    public static Predicate<Command> wereWithinPeriod(final Timestamp from, final Timestamp to) {
        checkNotNull(from);
        checkNotNull(to);
        return new Predicate<Command>() {
            @Override
            public boolean apply(@Nullable Command request) {
                checkNotNull(request);
                final Timestamp timestamp = getTimestamp(request);
                return Timestamps2.isBetween(timestamp, from, to);
            }
        };
    }

    private static Timestamp getTimestamp(Command request) {
        checkNotNull(request);
        final Timestamp result = request.getContext()
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
        Collections.sort(commands, new Comparator<Command>() {
            @Override
            public int compare(Command o1, Command o2) {
                final Timestamp timestamp1 = getTimestamp(o1);
                final Timestamp timestamp2 = getTimestamp(o2);
                return Timestamps2.compare(timestamp1, timestamp2);
            }
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
        final Schedule schedule = command.getContext()
                                         .getSchedule();
        final Duration delay = schedule.getDelay();
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
    public static boolean sameActorAndTenant(CommandContext c1, CommandContext c2) {
        checkNotNull(c1);
        checkNotNull(c2);
        final ActorContext a1 = c1.getActorContext();
        final ActorContext a2 = c2.getActorContext();
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
        final String idStr = Identifier.toString(id);
        checkArgument(!idStr.equals(EMPTY_ID), "Command ID must not be an empty string.");
        return id;
    }

    /**
     * Produces a {@link Rejection} for the given {@link Command} based on the given
     * {@linkplain ThrowableMessage cause}.
     *
     * <p>The given {@link Throwable} should be
     * {@linkplain Rejections#causedByRejection caused by a Rejection} or
     * an {@link IllegalArgumentException} is thrown.
     *
     * @param command the command to reject
     * @param cause the rejection cause (may be wrapped into other kinds of {@code Throwable})
     * @return a {@link Rejection} for the given command
     * @throws IllegalArgumentException upon an invalid rejection cause
     */
    @Internal
    public static Rejection rejectWithCause(Command command, Throwable cause)
            throws IllegalArgumentException {
        checkNotNull(command);
        checkNotNull(cause);

        final ThrowableMessage rejectionThrowable = Rejections.getCause(cause);
        final Rejection rejection = Rejections.toRejection(rejectionThrowable, command);
        return rejection;
    }

    /**
     * The stringifier for command IDs.
     */
    static class CommandIdStringifier extends Stringifier<CommandId> {
        @Override
        protected String toString(CommandId commandId) {
            final String result = commandId.getUuid();
            return result;
        }

        @Override
        protected CommandId fromString(String str) {
            final CommandId result = CommandId.newBuilder()
                                              .setUuid(str)
                                              .build();
            return result;
        }
    }
}
