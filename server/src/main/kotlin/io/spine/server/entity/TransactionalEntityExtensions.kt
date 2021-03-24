/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.entity

import io.spine.annotation.Experimental
import io.spine.base.EntityState
import io.spine.validate.ValidatingBuilder

/**
 * Extends [TransactionalEntity] with the `update` block for accessing
 * properties of the entity state [builder][TransactionalEntity.builder].
 *
 * For example, a method that applies an event may look like this:
 *
 * ```kotlin
 * @Apply
 * fun event(e: TaskCreated) {
 *     update {
 *         title = e.title
 *         description = e.description
 *     }
 * }
 * ```
 *
 * @param I the type of the entity identifiers
 * @param E the type of the transactional entity
 * @param S the type of the entity state
 * @param B the type of the entity state builder
 *
 * @apiNote This function is not `inline` because [TransactionalEntity.builder] is `protected`
 * while inline functions can use only `public` API.
 */
@Experimental
fun <I,
     E : TransactionalEntity<I, S, B>,
     S : EntityState<I>,
     B : ValidatingBuilder<S>>
        E.update(block: B.() -> Unit): B {
    val builder = builderOf(this)
    block.invoke(builder)
    return builder
}

/**
 * Obtains the builder from the passed entity.
 *
 * @apiNote We employ the fact that we are in the same package with [TransactionalEntity] and
 * because of this can access its `protected` API.
 */
private fun <I,
        S : EntityState<I>,
        B : ValidatingBuilder<S>>
        builderOf(e: TransactionalEntity<I, S, B>): B = e.builder()
