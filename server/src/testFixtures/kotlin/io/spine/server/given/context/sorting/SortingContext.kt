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

package io.spine.server.given.context.sorting

import com.google.protobuf.Empty
import io.spine.base.EventMessage
import io.spine.core.Subscribe
import io.spine.server.BoundedContext
import io.spine.server.BoundedContext.singleTenant
import io.spine.server.command.Assign
import io.spine.server.command.SingleCommandAssignee
import io.spine.server.entity.alter
import io.spine.server.given.context.sorting.SorterKt.bucket
import io.spine.server.given.context.sorting.command.GenerateFigures
import io.spine.server.given.context.sorting.event.FigureGenerated
import io.spine.server.given.context.sorting.event.figureGenerated
import io.spine.server.projection.Projection
import io.spine.server.route.Route
import io.spine.testing.TestValues.random

/**
 * This context generates geometric figures in response to the [GenerateFigures] command.
 *
 * Figures are accumulated by buckets by the [SorterView] projection.
 * Statistics for figures are handled by [FigureStatsView].
 *
 * ### Implementation note
 *
 * The implementation of this context is deliberately naïve in terms of event generation
 * and propagation. It generates an event for each generated figure.
 *
 * If a number of events is big, it leads to increase of a load to a data storage because we need to
 * load and store corresponding entity states. It is not noticeable for this test fixture
 * arrangement because in-memory storage is used.
 *
 * A production implementation of similar cases should prefer bigger event containing
 * all information (provided [size limit](https://stackoverflow.com/a/34186672) is met),
 * or series of events containing chunks of information.
 */
@Suppress("unused") // is declared for documentation purposes.
private const val ABOUT = ""

/**
 * Generates geometric figures taking the parameters from the [GenerateFigures] command.
 */
private class FigureGenerator: SingleCommandAssignee<GenerateFigures>() {

    @Assign
    override fun handle(command: GenerateFigures): Iterable<FigureGenerated> {
        val events = generateSequence {
            val figures = command.figureList
            val index = random(figures.size - 1)
            figures[index]
        }.map { selected ->
            figureGenerated {
                figure = selected
            }
        }.take(command.count).toList()
        return events
    }
}

/**
 * A singleton accumulating figures in buckets corresponding to their kinds
 * in response to [FigureGenerated] event.
 */
private class SorterView: Projection<String, Sorter, Sorter.Builder>() {

    @Subscribe
    fun whenever(event: FigureGenerated) = alter {
        val figure = event.figure
        fun newBucket() = bucket { this@bucket.figure.add(figure) }.toBuilder()

        // Find a bucket with the same kind of figures.
        val bucket = bucketBuilderList.find { bucket ->
            bucket.figureList.first().kindCase == figure.kindCase
        }
        if (bucket == null) {
            bucketBuilderList.add(newBucket())
            return@alter
        } else {
            bucket.addFigure(figure)
        }
    }

    companion object {
        @Route
        @JvmStatic
        fun toSingleton(@Suppress("UNUSED_PARAMETER") e: EventMessage): String = "SINGLETON"
    }
}

/**
 * Counts a number of times a figure was generated.
 */
private class FigureStatsView: Projection<Figure, FigureStats, FigureStats.Builder>() {

    @Subscribe
    fun whenever(event: FigureGenerated) = alter {
        figure = event.figure
        count = count.inc()
    }

    companion object {
        @Route
        @JvmStatic
        fun byFigure(e: FigureGenerated): Figure = e.figure
    }
}

/**
 * Creates Sorting bounded context.
 */
@Suppress("unused")
fun createSortingContext(): BoundedContext = singleTenant("Sorting").apply {
    addAssignee(FigureGenerator())
    add(SorterView::class.java)
    add(FigureStatsView::class.java)
}.build()
