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

package io.spine.internal.gradle.testing

import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register

/**
 * Registers [slowTest][SlowTest] and [fastTest][FastTest] tasks in this [TaskContainer].
 *
 * Slow tests are registered to run after all fast tests.
 *
 * Usage example:
 *
 * ```
 * tasks {
 *     registerTestTasks()
 * }
 * ```
 */
@Suppress("unused")
fun TaskContainer.registerTestTasks() {
    register<FastTest>("fastTest").let {
        register<SlowTest>("slowTest") {
            shouldRunAfter(it)
        }
    }
}

/**
 * Name of a tag for annotating a test class or method that is known to be slow and
 * should not normally be run together with the main test suite.
 *
 * @see [SlowTest](https://spine.io/base/reference/testlib/io/spine/testing/SlowTest.html)
 * @see [Tag](https://junit.org/junit5/docs/5.0.2/api/org/junit/jupiter/api/Tag.html)
 */
private const val SLOW_TAG = "slow"

/**
 * Executes JUnit tests filtering out the ones tagged as `slow`.
 */
private open class FastTest : Test() {
    init {
        description = "Executes all JUnit tests but the ones tagged as `slow`."
        group = "Verification"

        this.useJUnitPlatform {
            excludeTags(SLOW_TAG)
        }
    }
}

/**
 * Executes JUnit tests tagged as `slow`.
 */
private open class SlowTest : Test() {
    init {
        description = "Executes JUnit tests tagged as `slow`."
        group = "Verification"

        this.useJUnitPlatform {
            includeTags(SLOW_TAG)
        }
    }
}
