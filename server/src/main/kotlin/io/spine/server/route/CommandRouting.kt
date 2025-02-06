/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.base.CommandMessage
import io.spine.base.RejectionThrowable
import io.spine.core.CommandContext

/**
 * A routing schema for commands.
 *
 * A command is always delivered to only one entity. This is called [unicast][Unicast] routing.
 * That is why the entries of this schema, having the [CommandRoute] type, return [I].
 *
 * ## The default command route
 *
 * The default route used by `CommandRouting` takes the first field declared in
 * a command message type. This is handled by [DefaultCommandRoute] passed to the constructor
 * of `CommandRouting` when [newInstance] factory function is called.
 *
 * If alternative default routing is needed for your repository, please call [replaceDefault]
 * in the `setupCommandRouting` method of the corresponding repository class.
 *
 * ## Setting a custom route for a command
 *
 * Such a mapping may be required if the first field of the command message is
 * not of the same as the type of the entity ID which handles the command, as required by
 * the [default route][DefaultCommandRoute].
 *
 * Or, it could be the case when the target entity ID could be calculated by
 * both the command message and its context.
 *
 * ## Routing commands with a common interface
 *
 * The routing entry can accept a type of the command class or an interface.
 * If a routing schema needs to contain entries for command classes *and* an interface
 * that these commands implement, routes for interfaces should be defined *after* entries
 * for the classes:
 *
 * ```kotlin
 * routing.route<MyCommandClass> { command, context -> ... }
 *        .route<AnotherCommand> { command ->  ... }
 *        .route<MyInterface> { command, context ->  ... }
 *        .route<MySuperInterface> { command ->  ... }
 * ```
 * Also, adding entries for interfaces should start with more specific ones, followed by
 * those that are super-interfaces for already added entries.
 * If these rules are not followed, calling [route] will result in `IllegalStateException`.
 *
 * @param I The type of the entity IDs used by this command routing.
 * @param defaultRoute The route to use if a custom one is not [set][route].
 *
 * @see io.spine.server.procman.ProcessManagerRepository.setupCommandRouting
 * @see io.spine.server.aggregate.AggregateRepository.setupCommandRouting
 */
public class CommandRouting<I : Any> private constructor(
    defaultRoute: CommandRoute<I, CommandMessage>
) : MessageRouting<
        I,
        CommandMessage,
        CommandContext,
        I,
        CommandRouting<I>
        >(defaultRoute), CommandRoute<I, CommandMessage> {

    override fun self(): CommandRouting<I> = this

    /**
     * Obtains a route for the passed command class.
     *
     * @param C The type of the command message.
     * @return optionally available route for [C].
     */
    public inline fun <reified C : CommandMessage> find(): CommandRoute<I, C>? =
        find(C::class.java)

    /**
     * Obtains a route for the passed command class.
     *
     * @param cls The class of the command messages.
     * @param C The type of the command message.
     * @return optionally available route for [C].
     */
    public override fun <C : CommandMessage> find(cls: Class<C>): CommandRoute<I, C>? =
        super.find(cls) as CommandRoute<I, C>?

    public companion object {

        /**
         * Creates a new command routing.
         *
         * @param I The type of entity identifiers returned by new routing.
         * @param idClass The class of target entity identifiers.
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
         * @param cause The cause of not being able to route a command.
         *
         * @throws IllegalStateException always.
         */
        @JvmStatic
        public fun unableToRoute(command: CommandMessage, cause: RejectionThrowable) {
            throw IllegalStateException("Unable to route the command `$command`.", cause)
        }
    }
}

