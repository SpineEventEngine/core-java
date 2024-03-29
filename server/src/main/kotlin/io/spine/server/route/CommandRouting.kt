/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.server.route

import com.google.errorprone.annotations.CanIgnoreReturnValue
import io.spine.base.CommandMessage
import io.spine.base.RejectionThrowable
import io.spine.core.CommandContext
import java.util.*

/**
 * A routing schema used by a [CommandDispatcher][io.spine.server.commandbus.CommandDispatcher]
 * for delivering a command to its [receptor][io.spine.server.model.Receptor].
 *
 * A routing schema consists of a default route and custom routes per command class.
 * When finding a command target, the `CommandRouting` will see if there is a custom route
 * set for the type of the command. If not found, the [default route][DefaultCommandRoute]
 * will be [applied][CommandRoute.apply].
 *
 * @param I the type of the entity IDs used by this command routing.
 */
public class CommandRouting<I: Any>
private constructor(defaultRoute: CommandRoute<I, CommandMessage>) :
    MessageRouting<CommandMessage, CommandContext, I>(defaultRoute)
{
    public override fun defaultRoute(): CommandRoute<I, CommandMessage> {
        return super.defaultRoute() as CommandRoute<I, CommandMessage>
    }

    /**
     * Sets new default route in the schema.
     *
     * @param newDefault the new route to be used as default.
     * @return `this` to allow chained calls when configuring the routing.
     */
    @CanIgnoreReturnValue
    public fun replaceDefault(newDefault: CommandRoute<I, CommandMessage>): CommandRouting<I> {
        return super.replaceDefault(newDefault) as CommandRouting<I>
    }

    /**
     * Sets a custom route for the given command type [C].
     *
     * Such a mapping may be required if the first field of the command message is
     * not an ID of the entity which handles the command as required by
     * the [default route][DefaultCommandRoute].
     *
     * It could be because the first command field is of different type, or, when
     * we need to re-direct the command to an entity with a different ID.
     *
     * ### Routing commands with a common interface
     * The type of the command can be a class or an interface. If a routing schema needs to
     * contain entries for command classes *and* an interface that these commands implement, routes
     * for interfaces should be defined *after* entries for the classes:
     *
     * ```kotlin
     * customRouting.route<MyCommandClass> { event, context -> ... }
     *              .route<MyCommandInterface> { event, context ->  ... }
     * ```
     * Defining an entry for an interface and then for the class which implements the interface will
     * result in `IllegalStateException`.
     *
     * @param via
     *         the route to be used for this type of commands.
     * @param C the type of the command message.
     * @return `this` to allow chained calls when configuring the routing.
     * @throws IllegalStateException
     *          if the route for this command class is already set either directly or
     *          via a super-interface.
     */
    public inline fun <reified C : CommandMessage> route(
        via: CommandRoute<I, C>
    ): CommandRouting<I> = route(C::class.java, via)

    /**
     * Sets a custom route for the given command type [C].
     *
     * This is the Java version of `public inline fun` [route].
     *
     * @param commandType
     *         the type of the command message.
     * @param via
     *         the route to be used for this type of commands.
     * @param C the type of the command message.
     * @return `this` to allow chained calls when configuring the routing.
     * @throws IllegalStateException
     *          if the route for this command class is already set either directly or
     *          via a super-interface.
     * @see route
     */
    @CanIgnoreReturnValue
    public fun <C : CommandMessage> route(
        commandType: Class<C>,
        via: CommandRoute<I, C>
    ): CommandRouting<I> {
        @Suppress("UNCHECKED_CAST") // The cast is required to adapt the type to internal API.
        val casted = via as RouteFn<CommandMessage, CommandContext, I>
        addRoute(commandType, casted)
        return this
    }

    /**
     * Obtains a route for the passed command class.
     *
     * @param C the type of the command message.
     * @return optionally available route for [C].
     */
    public inline fun <reified C : CommandMessage> find(): CommandRoute<I, C>? =
        find(C::class.java)

    /**
     * Obtains a route for the passed command class.
     *
     * @param commandClass
     *         the class of the command messages.
     * @param C the type of the command message.
     * @return optionally available route for [C].
     */
    public fun <C : CommandMessage> find(commandClass: Class<C>): CommandRoute<I, C>? {
        val match: Match = routeFor(commandClass)
        return if (match.found()) {
            @Suppress("UNCHECKED_CAST") // protected by generic params of this class
            match.route() as CommandRoute<I, C>
        } else {
            null
        }
    }

    /**
     * Obtains a route for the passed command class.
     */
    @Deprecated("Use `find` instead.", ReplaceWith("find(commandClass)"))
    public fun <C : CommandMessage> get(commandClass: Class<C>): Optional<CommandRoute<I, C>> {
        return Optional.ofNullable(find(commandClass))
    }

    /**
     * Removes a route for the passed command class.
     *
     * @param C the type of the command for which to remove the routing.
     * @throws IllegalStateException
     *          if a custom route for this message class was not previously set.
     */
    public inline fun <reified C : CommandMessage> remove(): Unit =
        remove(C::class.java)

    public companion object {

        @Suppress("ConstPropertyName")
        private const val serialVersionUID: Long = 0L

        /**
         * Creates a new command routing.
         *
         * @param I
         *         the type of entity identifiers returned by new routing.
         * @param idClass
         *         the class of target entity identifiers.
         * @return new routing instance.
         */
        @JvmStatic
        public fun <I: Any> newInstance(idClass: Class<I>): CommandRouting<I> {
            val defaultRoute = DefaultCommandRoute.newInstance(idClass)
            return CommandRouting(defaultRoute)
        }

        /**
         * Tells that the [command] cannot be routed by throwing [IllegalStateException].
         *
         * The function is expected to be called by command routing functions
         * when they cannot route a command.
         *
         * @param cause
         *         the cause of not being able to route a command.
         *
         * @throws IllegalStateException always.
         */
        @JvmStatic
        public fun unableToRoute(command: CommandMessage, cause: RejectionThrowable) {
            throw IllegalStateException("Unable to route command `$command`.", cause)
        }
    }
}
