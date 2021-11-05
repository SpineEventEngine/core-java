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

package io.spine.internal.gradle.test

import io.spine.internal.gradle.test.task.FastTest
import io.spine.internal.gradle.test.task.SlowTest
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.kotlin.dsl.TaskContainerScope
import org.gradle.kotlin.dsl.register

fun TaskContainerScope.registerTestTasks() {
    register<FastTest>("fastTest").let {
        register<SlowTest>("slowTest") {
            shouldRunAfter(it)
        }
    }
}

fun Test.configureOutput() {
    fun TestResult.summary(): String =
        """
        Test custom kotlin summary:
        >> $testCount tests
        >> $successfulTestCount succeeded
        >> $failedTestCount failed
        >> $skippedTestCount skipped
        """

    testLogging {
        showExceptions = true
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
        showCauses = true
        showStandardStreams = true
    }

    afterSuite(

        // `GroovyInteroperability` is employed as `afterSuite()` has no equivalent in Kotlin DSL.
        // See issue: https://github.com/gradle/gradle/issues/5431

        KotlinClosure2<TestDescriptor, TestResult, Unit>({ descriptor, result ->

            // If the descriptor has no parent, then it is the root test suite,
            // i.e. it includes the info about all the run tests.

            if (descriptor.parent == null) {
                logger.lifecycle(result.summary())
            }
        })
    )
}
