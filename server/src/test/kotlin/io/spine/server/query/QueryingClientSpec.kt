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

package io.spine.server.query

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth8.assertThat
import io.spine.grpc.StreamObservers
import io.spine.server.BoundedContext
import io.spine.server.given.counting.NumberStats
import io.spine.server.given.counting.Range
import io.spine.server.given.counting.RangeStats
import io.spine.server.given.counting.command.generateNumbers
import io.spine.server.given.counting.createCountingContext
import io.spine.server.given.counting.range
import io.spine.testing.client.TestActorRequestFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`QueryingClient` should")
internal class QueryingClientSpec {

    /**
     * Prepares the environment for all the tests of this suite.
     */
    companion object {

        private val actor = QueryingClientSpec::class.simpleName!!
        private lateinit var context: BoundedContext
        private lateinit var range: Range

        @JvmStatic
        @BeforeAll
        fun initContext() {
            context = createCountingContext()
            range = range {
                minValue = -100
                maxValue = 100
            }

            val factory = TestActorRequestFactory(this::class.java)
            val cmd = factory.createCommand(generateNumbers {
                count = 100
                range = this@Companion.range
            })

            context.commandBus().post(cmd, StreamObservers.noOpObserver())
        }

        @JvmStatic
        @AfterAll
        fun shutDown() {
            context.close()
        }
    }

    @Test
    fun `fetch multiple results`() {
        val client = QueryingClient(context, NumberStats::class.java, actor)
        val stats = client.all()
        assertThat(stats).isNotEmpty()
    }

    @Test
    fun `fetch the only one`() {
        val client = QueryingClient(context, RangeStats::class.java, actor)
        val rangeStats = client.find(range)
        assertThat(rangeStats).isPresent()
        assertThat(rangeStats.get().range).isEqualTo(range)
    }

    @Test
    fun `fetch none`() {
        val client = QueryingClient(context, RangeStats::class.java, actor)
        val nonExisting = range {
            minValue = -200
            maxValue = 200
        }
        val rangeStats = client.find(nonExisting)
        assertThat(rangeStats).isEmpty()
    }
}
