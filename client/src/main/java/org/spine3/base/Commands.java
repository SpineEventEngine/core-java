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

package org.spine3.base;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.AnyPacker;
import org.spine3.string.Stringifier;
import org.spine3.time.Timestamps2;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.CommandContext.Schedule;
import static org.spine3.base.Identifiers.EMPTY_ID;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * Client-side utilities for working with commands.
 *
 * @author Alexander Yevsyukov
 */
public final class Commands {

    /** A suffix which the {@code .proto} file containing commands must have in its name. */
    public static final String FILE_NAME_SUFFIX = "commands";

    private static final char FILE_PATH_SEPARATOR = '/';
    private static final char FILE_EXTENSION_SEPARATOR = '.';

    private static final Stringifier<CommandId> idStringifier = new CommandIdStringifier();

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
     * Checks if the file is for commands.
     *
     * @param file a descriptor of a {@code .proto} file to check
     * @return {@code true} if the file name ends with the {@link #FILE_NAME_SUFFIX},
     * {@code false} otherwise
     */
    public static boolean isCommandsFile(FileDescriptor file) {
        checkNotNull(file);

        final String fqn = file.getName();
        final int startIndexOfFileName = fqn.lastIndexOf(FILE_PATH_SEPARATOR) + 1;
        final int endIndexOfFileName = fqn.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        final String fileName = fqn.substring(startIndexOfFileName, endIndexOfFileName);
        final boolean isCommandsFile = fileName.endsWith(FILE_NAME_SUFFIX);
        return isCommandsFile;
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
        final String idStr = idToString(id);
        checkArgument(!idStr.equals(EMPTY_ID), "Command ID must not be an empty string.");
        return id;
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
