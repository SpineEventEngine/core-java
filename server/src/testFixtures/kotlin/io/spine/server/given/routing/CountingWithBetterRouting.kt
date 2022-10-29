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

package io.spine.server.given.routing

import io.spine.core.Subscribe
import io.spine.server.BoundedContext
import io.spine.server.BoundedContext.singleTenant
import io.spine.server.command.Assign
import io.spine.server.command.SingleCommandAssignee
import io.spine.server.entity.alter
import io.spine.server.given.counting.NumberStats
import io.spine.server.given.counting.Range
import io.spine.server.given.counting.RangeStats
import io.spine.server.given.counting.command.GenerateNumbers
import io.spine.server.given.counting.event.NumberGenerated
import io.spine.server.given.counting.event.numberGenerated
import io.spine.server.projection.Projection
import io.spine.server.route.Route
import io.spine.testing.TestValues.random

/**
 * This context is similar to
 * [CountingContext][io.spine.server.given.counting.createCountingContext]
 * but is based on improved routing.
 *
 * Once the API for declaration of routing schemas per class is finalized,
 * we need to replace this context with another mini-domain to avoid
 * potential confusion.
 */
@Suppress("unused") // is declared for documentation purposes.
private const val ABOUT = ""

/**
 * Generates random numbers taking the parameter from the [GenerateNumbers] command.
 */
private class RandomNumberGenerator: SingleCommandAssignee<GenerateNumbers>() {

    @Assign
    override fun handle(command: GenerateNumbers): Iterable<NumberGenerated> {
        val events = generateSequence {
            with(command.range) {
                random(minValue, maxValue)
            }
        }.map { generated ->
            numberGenerated {
                number = generated
                range = command.range
            }
        }.take(command.count).toList()
        return events
    }
}

/**
 * Accumulates number stats per range in response to [NumberGenerated] event.
 */
private class RangeStatsView: Projection<Range, RangeStats, RangeStats.Builder>() {

    @Subscribe
    fun whenever(event: NumberGenerated) = alter {
        range = event.range
        val number = event.number
        val currentCount = countMap[number]
        val newValue = currentCount?.inc() ?: 1
        putCount(number, newValue)
    }

    companion object {
        @Route @JvmStatic
        fun byRange(e: NumberGenerated): Range = e.range
    }
}

/**
 * Counts a number of times a number was generated.
 */
private class NumberStatsView: Projection<Int, NumberStats, NumberStats.Builder>() {

    @Subscribe
    fun whenever(event: NumberGenerated) = alter {
        number = event.number
        count = count.inc()
    }

    companion object {
        @Route @JvmStatic
        fun byNumber(e: NumberGenerated): Int = e.number
    }
}

/**
 * Creates Counting bounded context.
 */
@Suppress("unused")
fun createCountingContext(): BoundedContext = singleTenant("Counting").apply {
    addAssignee(RandomNumberGenerator())
    add(RangeStatsView::class.java)
    add(NumberStatsView::class.java)
}.build()
