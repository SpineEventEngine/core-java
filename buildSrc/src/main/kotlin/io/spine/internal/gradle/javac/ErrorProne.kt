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

package io.spine.internal.gradle.javac

import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider

/**
 * Sets up `Error Prone` by applying [ErrorProneConfig].
 *
 * Which means specifying arguments for the `Error Prone` compiler invocation.
 *
 * Although `Error Prone` is linked as a dedicated `Gradle` plugin, its configuration is actually
 * performed through the [JavaCompile] task.
 *
 * Here's an example of how to use it:
 *
 *     tasks {
 *         withType<JavaCompile> {
 *             configureErrorProne()
 *         }
 *     }
 */
fun JavaCompile.configureErrorProne() {
    options.errorprone
        .errorproneArgumentProviders
        .add(ErrorProneConfig.ARGUMENTS)
}

/**
 * The knowledge that is required to set up `Error Prone`.
 */
private object ErrorProneConfig {

    /**
     * Command line options for the `Error Prone` compiler.
     */
    val ARGUMENTS = CommandLineArgumentProvider {
        listOf(

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
    }
}
