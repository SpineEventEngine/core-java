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

package io.spine.server.route;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.core.CommandContext;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A routing schema used by a {@link io.spine.server.commandbus.CommandDispatcher CommandDispatcher}
 * for delivering a command to its handler.
 *
 * <p>A routing schema consists of a default route and custom routes per command class.
 * When finding a command target the {@code CommandRouting} would see if there is a custom route
 * set for the type of the command. If not found, the {@linkplain DefaultCommandRoute default route}
 * will be {@linkplain CommandRoute#apply(Message, Message) applied}.
 *
 * @param <I> the type of the entity IDs of this repository
 */
public final class CommandRouting<I> extends MessageRouting<CommandMessage, CommandContext, I> {

    private static final long serialVersionUID = 0L;

    private CommandRouting(CommandRoute<I, CommandMessage> defaultRoute) {
        super(defaultRoute);
    }

    /**
     * Creates a new command routing.
     *
     * @param <I> the type of entity identifiers returned by new routing
     * @return new routing instance
     */
    public static <I> CommandRouting<I> newInstance() {
        CommandRoute<I, CommandMessage> defaultRoute = DefaultCommandRoute.newInstance();
        return new CommandRouting<>(defaultRoute);
    }

    @Override
    public final CommandRoute<I, CommandMessage> getDefault() {
        return (CommandRoute<I, CommandMessage>) super.getDefault();
    }

    /**
     * Sets new default route in the schema.
     *
     * @param newDefault the new route to be used as default
     * @return {@code this} to allow chained calls when configuring the routing
     */
    @CanIgnoreReturnValue
    public CommandRouting<I> replaceDefault(CommandRoute<I, CommandMessage> newDefault) {
        checkNotNull(newDefault);
        return (CommandRouting<I>) super.replaceDefault(newDefault);
    }

    /**
     * Sets a custom route for the passed command class.
     *
     * <p>Such a mapping may be required when...
     * <ul>
     * <li>The first field of the command message is not an ID of the entity which handles the
     * command (as required by the {@linkplain DefaultCommandRoute default route}.
     * <li>The command need to be dispatched to an entity which ID differs from the value set in the
     * first command attribute.
     * </ul>
     *
     * @param commandClass the class of the command message
     * @param via          the route to be used for this class of commands
     * @param <M>          the type of the command message
     * @return {@code this} to allow chained calls when configuring the routing
     * @throws IllegalStateException if the route for this command class is already set
     */
    @CanIgnoreReturnValue
    public <M extends CommandMessage>
    CommandRouting<I> route(Class<M> commandClass, CommandRoute<I, M> via)
            throws IllegalStateException {
        @SuppressWarnings("unchecked") // The cast is required to adapt the type to internal API.
        Route<CommandMessage, CommandContext, I> casted =
                (Route<CommandMessage, CommandContext, I>) via;
        doRoute(commandClass, casted);
        return this;
    }

    /**
     * Obtains a route for the passed command class.
     *
     * @param commandClass the class of the command messages
     * @param <M>          the type of the command message
     * @return optionally available route
     */
    public <M extends CommandMessage> Optional<CommandRoute<I, M>> get(Class<M> commandClass) {
        Optional<? extends Route<CommandMessage, CommandContext, I>> optional = doGet(commandClass);
        if (optional.isPresent()) {
            Route<CommandMessage, CommandContext, I> route = optional.get();
            @SuppressWarnings({"unchecked", "RedundantSuppression"})
                // The cast is safe as we deal only with CommandRoute's.
            CommandRoute<I, M> commandRoute = (CommandRoute<I, M>) route;
            return Optional.of(commandRoute);
        }
        return Optional.empty();
    }
}
