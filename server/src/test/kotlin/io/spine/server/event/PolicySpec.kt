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

package io.spine.server.event

import io.kotest.matchers.shouldBe
import io.spine.core.External
import io.spine.server.tuple.EitherOf2
import io.spine.test.shared.event.SomethingHappened
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`Policy` should")
internal class PolicySpec {

    @Test
    fun `do not allow adding more react methods`() {
        assertThrows<IllegalStateException> {
            GreedyPolicy()
        }
    }

    @Test
    fun `allow using 'Just' in return value`() {
        val policy = object : Policy<SomethingHappened>() {
            @React
            public override fun whenever(event: SomethingHappened): Just<NoReaction> {
                return Just.noReaction
            }
        }
        policy.whenever(somethingHappened) shouldBe Just.noReaction
    }

    @Test
    fun `allow using 'Either' in return value`() {
        object : Policy<SomethingHappened>() {
            @React
            public override fun whenever(
                event: SomethingHappened
            ): EitherOf2<NoReaction, NoReaction> = noReaction().asA()
        }.let {
            it.whenever(somethingHappened) shouldBe EitherOf2.withA(noReaction)
        }
    }

    companion object {
        val somethingHappened: SomethingHappened = SomethingHappened.getDefaultInstance()
        val noReaction: NoReaction = NoReaction.getDefaultInstance()
    }
}

/**
 * The policy which attempts to define a `@React` receptor to handle more than one
 * event type, as required by the `Policy` contract.
 */
private class GreedyPolicy : Policy<NoReaction>() {

    @React
    override fun whenever(@External event: NoReaction): Just<NoReaction> =
        Just.noReaction

    @React
    fun on(@Suppress("UNUSED_PARAMETER") e: SomethingHappened): Just<NoReaction> =
        Just.noReaction
}
