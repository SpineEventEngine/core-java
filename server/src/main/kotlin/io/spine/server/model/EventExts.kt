/*
 * Copyright 2024, TeamDev. All rights reserved.
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

@file:Suppress("RemoveRedundantQualifierName") /*
    We have the event message type called `Nothing` which belongs to the same package as this file.
    This event message type was created and used before Kotlin and its type `kotlin.Nothing` became
    widely known.
    We use fully qualified name for our `Nothing` type in this file, which causes the warning we
    suppress above, because our `Nothing` is in the same package with this file.
    We want the fully qualified name to avoid potential confusion with the standard Kotlin type
    if readers don't notice pay the package name of this file during navigation to the type
    aliases or extensions below.
 */

package io.spine.server.model

import io.spine.base.EventMessage
import io.spine.server.EventProducer
import io.spine.server.tuple.EitherOf2

/**
 * The alias for cases of naming collisions between
 * [io.spine.server.model.Nothing] and [kotlin.Nothing].
 */
public typealias NothingHappened = io.spine.server.model.Nothing

/**
 * The alias for cases of naming collisions between
 * [io.spine.server.model.Nothing] and [kotlin.Nothing].
 */
public typealias NoReaction = io.spine.server.model.Nothing

/**
 * The alias to simplify return types of reacting methods when an event of
 * the type specified by the generic parameter [A] is conditionally returned in
 * some cases and [NoReaction] in others.
 */
public typealias OptionalReaction<A> = EitherOf2<A, NoReaction>

/**
 * Produces an instance of [OptionalReaction] holding [NoReaction].
 */
public fun <A: EventMessage> EventProducer.noReaction(): OptionalReaction<A> =
    EitherOf2.withB(nothing())
