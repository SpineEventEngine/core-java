/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.event

import com.google.common.collect.ImmutableSet
import com.google.protobuf.Message
import io.spine.base.EventMessage
import io.spine.core.ContractFor
import io.spine.logging.Logging
import io.spine.server.BoundedContext
import io.spine.server.type.EventClass

/**
 * A policy converts one event into zero to many other events.
 *
 * As a rule of thumb, a policy should read:
 * Whenever <something happens>, then <something else must happen>.
 *
 * For example:
 * Whenever a field option is discovered, a validation rule must be added.
 *
 * To implement the policy, declare a method which reacts to an event with an event:
 * ```kotlin
 * class MyPolicy : Policy<ProjectCreated>() {
 *
 *     @React
 *     override fun whenever(event: ProjectCreated): Just<ReadmeFileAdded> {
 *         // Produce the event.
 *     }
 * }
 * ```
 * @param E the type of the event handled by this policy
 */
public abstract class Policy<E : EventMessage>: AbstractEventReactor(), Logging {

    protected lateinit var context: BoundedContext

    /**
     * Handles an event and produces some number of events in responce.
     */
    @ContractFor(handler = React::class)
    protected abstract fun whenever(event: E): Iterable<Message>
    final override fun registerWith(context: BoundedContext) {
        super.registerWith(context)
        this.context = context
    }

    /**
     * Ensures that there is only one event receptor defined in the derived class.
     *
     * @throws IllegalStateException
     *          if the derived class defines more than one event receptor
     */
    final override fun messageClasses(): ImmutableSet<EventClass> {
        val classes = super.messageClasses()
        checkHandlers(classes)
        return classes
    }

    private fun checkHandlers(events: Iterable<EventClass>) {
        val classes = events.toList()
        if (classes.size > 1) {
            throw IllegalStateException(
                "Policy `${javaClass.name}` handles too many events: [${classes.joinToString()}]."
            )
        }
    }
}
