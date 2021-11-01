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

package io.spine.internal.gradle.javacompile

import io.spine.internal.gradle.javacompile.ErrorProneConfig.configureErrorProne
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

/**
 * Configures `ErrorProne` plugin.
 *
 * ```
 * tasks {
 *     withType<JavaCompile> {
 *         configureErrorProne()
 *     }
 * }
 * ```
 */
object ErrorProneConfig {

    private val CODE_ANALYZING_FLAGS = listOf(

        // Exclude generated sources from being analyzed by ErrorProne.
        "-XepExcludedPaths:.*/generated/.*",

        // Turn the check off until ErrorProne can handle `@Nested` JUnit classes.
        // See issue: https://github.com/google/error-prone/issues/956
        "-Xep:ClassCanBeStatic:OFF",

        // Turn off checks that report unused methods and method parameters.
        // See issue: https://github.com/SpineEventEngine/config/issues/61
        "-Xep:UnusedMethod:OFF",
        "-Xep:UnusedVariable:OFF",

        "-Xep:CheckReturnValue:OFF",
        "-Xep:FloggerSplitLogStatement:OFF",
    )

    /**
     * Applies [ErrorProneConfig] to this [JavaCompile]. Although `ErrorProne` is a plugin,
     * it is actually configured through the [JavaCompile] task.
     */
    fun JavaCompile.configureErrorProne() {
        options.errorprone
            .errorproneArgs
            .addAll(CODE_ANALYZING_FLAGS)
    }
}

// Pick up the API that fits best to your code base.

/**
 * Applies [ErrorProneConfig] to all [JavaCompile] tasks in this container.
 */
fun TaskContainer.configureErrorProne() {
    withType<JavaCompile> {
        configureErrorProne()
    }
}

/**
 * Applies [ErrorProneConfig] to all [JavaCompile] tasks in the container.
 */
fun ErrorProneConfig.applyTo(tasks: TaskContainer) {
    tasks.withType<JavaCompile> {
        configureErrorProne()
    }
}

/**
 * Applies [ErrorProneConfig] to all [JavaCompile] tasks in this project.
 */
fun Project.configureErrorProne() {
    tasks.withType<JavaCompile> {
        configureErrorProne()
    }
}

/**
 * Applies [ErrorProneConfig] to all [JavaCompile] tasks in the project.
 */
fun ErrorProneConfig.applyTo(project: Project) {
    project.tasks.withType<JavaCompile> {
        configureErrorProne()
    }
}
