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
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.Internal;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps;
import org.spine3.time.ZoneOffset;
import org.spine3.users.TenantId;
import org.spine3.users.UserId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.CommandContext.Schedule;
import static org.spine3.base.CommandContext.newBuilder;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.validate.Validate.checkPositive;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * Client-side utilities for working with commands.
 *
 * @author Alexander Yevsyukov
 */
public class Commands {

    /** A suffix which the {@code .proto} file containing commands must have in its name. */
    public static final String FILE_NAME_SUFFIX = "commands";

    private static final char FILE_PATH_SEPARATOR = '/';
    private static final char FILE_EXTENSION_SEPARATOR = '.';

    private Commands() {
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
     * Creates a new command context with the current time.
     *
     * <p>This method is not supposed to be called from outside the framework.
     * Commands in client applications should be created by
     * {@link org.spine3.client.CommandFactory#createCommand(Message) CommandFactory.create(Message)},
     * which creates {@code CommandContext} automatically.
     *
     * @param tenantId   the ID of the tenant or {@code null} for single-tenant applications
     * @param userId     the actor ID
     * @param zoneOffset the offset of the timezone in which the user works
     * @return new {@code CommandContext}
     * @see org.spine3.client.CommandFactory#createCommand(Message)
     */
    @Internal
    public static CommandContext createContext(@Nullable TenantId tenantId,
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
     * <p>This method is not supposed to be called from outside the framework.
     * Commands in client applications should be created by {@link org.spine3.client.CommandFactory#createCommand(Message)},
     * which creates {@code CommandContext} automatically.
     *
     * @param tenantId      the ID of the tenant or {@code null} for single-tenant applications
     * @param userId        the actor id
     * @param zoneOffset    the offset of the timezone in which the user works
     * @param targetVersion the the ID of the entity for applying commands
     * @return new {@code CommandContext}
     * @see org.spine3.client.CommandFactory#createCommand(Message)
     */
    @Internal
    public static CommandContext createContext(@Nullable TenantId tenantId,
                                               UserId userId,
                                               ZoneOffset zoneOffset,
                                               int targetVersion) {
        checkNotNull(userId);
        checkNotNull(zoneOffset);
        checkNotNull(targetVersion);

        final CommandContext.Builder result = newContextBuilder(tenantId, userId, zoneOffset);
        result.setTargetVersion(targetVersion);

        return result.build();
    }

    private static CommandContext.Builder newContextBuilder(@Nullable TenantId tenantId,
                                                            UserId userId,
                                                            ZoneOffset zoneOffset) {
        final CommandId commandId = generateId();
        final CommandContext.Builder result = newBuilder()
                .setActor(userId)
                .setTimestamp(getCurrentTime())
                .setCommandId(commandId)
                .setZoneOffset(zoneOffset);
        if (tenantId != null) {
            result.setTenantId(tenantId);
        }
        return result;
    }

    /**
     * Creates a new instance of {@code CommandContext} based on the passed one.
     *
     * <p>The returned instance gets new generated {@code CommandId} and {@code timestamp}
     * set to the time of the call.
     *
     * @param commandContext the instance from which to copy values
     * @return new {@code CommandContext}
     */
    public static CommandContext newContextBasedOn(CommandContext commandContext) {
        checkNotNull(commandContext);
        final CommandContext.Builder result = commandContext.toBuilder()
                .setCommandId(generateId())
                .setTimestamp(getCurrentTime());
        return result.build();
    }

    /**
     * Creates a command instance with the given {@code message} and the {@code context}.
     *
     * <p>If {@code Any} instance is passed as the first parameter it will be used as is.
     * Otherwise, the command message will be packed into {@code Any}.
     *
     * @param message the command message
     * @param context the context of the command
     * @return a new command
     */
    public static Command createCommand(Message message, CommandContext context) {
        checkNotNull(message);
        checkNotNull(context);

        final Any packed = AnyPacker.pack(message);
        final Command.Builder result = Command.newBuilder()
                                               .setMessage(packed)
                                               .setContext(context);
        return result.build();
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
     * Extracts a command ID from the passed {@code Command} instance.
     */
    public static CommandId getId(Command command) {
        checkNotNull(command);
        final CommandId id = command.getContext()
                                    .getCommandId();
        return id;
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
                return Timestamps.isLaterThan(timestamp, from);
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
                return Timestamps.isBetween(timestamp, from, to);
            }
        };
    }

    private static Timestamp getTimestamp(Command request) {
        checkNotNull(request);
        final Timestamp result = request.getContext()
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
                return Timestamps.compare(timestamp1, timestamp2);
            }
        });
    }

    /**
     * Checks if the file is for commands.
     *
     * @param file a descriptor of a {@code .proto} file to check
     * @return {@code true} if the file name ends with the {@link #FILE_NAME_SUFFIX}, {@code false} otherwise
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
     * @return {@code true} if the command context has a scheduling option set, {@code false} otherwise
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
     * Sets a new scheduling time to {@link Schedule}.
     *
     * @param command        a command to update
     * @param schedulingTime the time when the command was scheduled by the {@code CommandScheduler}
     * @return an updated command
     */
    @Internal
    public static Command setSchedulingTime(Command command, Timestamp schedulingTime) {
        checkNotNull(command);
        checkNotNull(schedulingTime);

        final Duration delay = command.getContext()
                                      .getSchedule()
                                      .getDelay();
        final Command result = setSchedule(command, delay, schedulingTime);
        return result;
    }

    /**
     * Updates {@link Schedule}.
     *
     * @param command        a command to update
     * @param delay          a delay to set (see {@link Schedule#getDelay()} for details)
     * @param schedulingTime the time when the command was scheduled by the {@code CommandScheduler}
     * @return an updated command
     */
    @Internal
    public static Command setSchedule(Command command, Duration delay, Timestamp schedulingTime) {
        checkNotNull(command);
        checkNotNull(delay);
        checkNotNull(schedulingTime);
        checkPositive(schedulingTime, "command scheduling time");

        final CommandContext context = command.getContext();
        final Schedule scheduleUpdated = context.getSchedule()
                                                .toBuilder()
                                                .setDelay(delay)
                                                .setSchedulingTime(schedulingTime)
                                                .build();
        final CommandContext contextUpdated = context.toBuilder()
                                                     .setSchedule(scheduleUpdated)
                                                     .build();
        final Command result = command.toBuilder()
                                      .setContext(contextUpdated)
                                      .build();
        return result;
    }

    /**
     * Tests whether both command contexts are from the same actor
     * working under the same tenant.
     */
    @VisibleForTesting
    public static boolean sameActorAndTenant(CommandContext c1, CommandContext c2) {
        checkNotNull(c1);
        checkNotNull(c2);
        return  c1.getActor().equals(c2.getActor()) &&
                c1.getTenantId().equals(c2.getTenantId());
    }
}
