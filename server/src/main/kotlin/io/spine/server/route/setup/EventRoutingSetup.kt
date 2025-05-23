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

package io.spine.server.route.setup

import io.spine.base.EventMessage
import io.spine.core.EventContext
import io.spine.server.entity.Entity
import io.spine.server.route.EventRouting
import io.spine.server.route.EventRoutingMap

/**
 * The base interface for generated classes that customize [EventRouting] for
 * a class of entities that handle events.
 *
 * @param I The type of the entity identifiers.
 */
public interface EventRoutingSetup<I : Any> :
    RoutingSetup<I, EventMessage, EventContext, Set<I>, EventRouting<I>> {

    public companion object {

        /**
         * Configures the [EventRouting] for a repository, if
         * a corresponding routing setup class exists.
         *
         * The setup class, if available, is determined based
         * on the entity class managed by the repository.
         * Once found, its [setup] function is invoked to customize the routing.
         *
         * If no setup class is found, this function has no effect.
         *
         * @param cls The class of entities managed by the repository.
         * @param routing The [EventRouting] instance to be customized.
         */
        @JvmStatic
        public fun <I : Any> apply(
            cls: Class<out Entity<I, *>>,
            routing: EventRouting<I>
        ) {
            val found = RoutingSetupRegistry.find(cls, EventRoutingSetup::class.java)
            found?.let {
                @Suppress("UNCHECKED_CAST")
                (it as EventRoutingSetup<I>).setup(routing)
            } ?: run {
                // Use reflection-based schema, if any.
                val classRouting = EventRoutingMap(cls)
                classRouting.addTo(routing)
            }
        }
    }
}
