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

import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.protobuf.Messages;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.CommandContext.Schedule;
import static io.spine.validate.Validate.isNotDefault;

/**
 * Client-side utilities for working with commands.
 */
public final class Commands {

    private static final Stringifier<CommandId> idStringifier = new CommandIdStringifier();

    static {
        StringifierRegistry.instance()
                           .register(idStringifier(), CommandId.class);
    }

    /** Prevent instantiation of this utility class. */
    private Commands() {
    }

    /**
     * Extracts the message from the passed {@code Command} instance.
     *
     * @param command a command to extract a message from
     * @return an unpacked message
     * @deprecated use {@link Command#enclosedMessage()}
     */
    @Deprecated
    public static CommandMessage getMessage(Command command) {
        checkNotNull(command);
        CommandMessage result = command.enclosedMessage();
        return result;
    }

    /**
     * Extracts a command message if the passed instance is a {@link Command} object or
     * {@link com.google.protobuf.Any Any}, otherwise returns the passed message.
     */
    public static CommandMessage ensureMessage(Message commandOrMessage) {
        checkNotNull(commandOrMessage);
        if (commandOrMessage instanceof Command) {
            return ((Command) commandOrMessage).enclosedMessage();
        }
        CommandMessage unpacked = (CommandMessage) Messages.ensureMessage(commandOrMessage);
        return unpacked;
    }

    /**
     * Obtains a tenant ID from the command.
     *
     * @deprecated Please use {@link Command#tenant()}.
     */
    @Deprecated
    public static TenantId tenantOf(Command command) {
        checkNotNull(command);
        TenantId result = command.tenant();
        return result;
    }

    /**
     * Obtains a {@link TenantId} from the {@link CommandContext}.
     *
     * <p>The {@link CommandContext} is accessible from the {@link Event} if the {@code Event} was 
     * created as a result of some command or its rejection. This makes the {@code CommandContext}
     * a valid {@code TenantId} source inside of the {@code Event}.
     *
     * @deprecated Please use {@link Command#tenant()}
     */
    @Deprecated
    @Internal
    public static TenantId tenantOf(CommandContext context) {
        return context.getActorContext()
                      .getTenantId();
    }

    /**
     * Sorts the command given command request list by command timestamp value.
     *
     * @param commands the command list to sort
     */
    public static void sort(List<Command> commands) {
        checkNotNull(commands);
        commands.sort((c1, c2) -> Timestamps.compare(c1.time(), c2.time()));
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
        Schedule schedule = command.context()
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
     * Obtains stringifier for command IDs.
     */
    public static Stringifier<CommandId> idStringifier() {
        return idStringifier;
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
            CommandId result = CommandId.of(str);
            return result;
        }
    }
}
