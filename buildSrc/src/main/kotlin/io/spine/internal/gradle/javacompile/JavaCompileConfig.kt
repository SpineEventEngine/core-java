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

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

/**
 * [JavaCompile] task configuration.
 */
object JavaCompileConfig {

    private const val srcFilesEncoding = "UTF-8"
    private val expectedJavaVersion = JavaVersion.VERSION_1_8
    private val commandLineFlags = listOf(
        "-Xlint:unchecked",
        "-Xlint:deprecation",

        // Protobuf Compiler generates the code, which uses the deprecated PARSER field.
        // See issue: https://github.com/SpineEventEngine/config/issues/173
        // "-Werror"
    )

    fun applyTo(project: Project) {
        ensureJavaVersion()
        project.tasks.withType<JavaCompile> {
            options.encoding = srcFilesEncoding
            options.compilerArgs.addAll(commandLineFlags)
        }
    }

    private fun ensureJavaVersion() {
        if (JavaVersion.current() != expectedJavaVersion) {
            throw GradleException("Spine Event Engine can be built with JDK 8 only." +
                    " Supporting JDK 11 and above at build-time is planned in 2.0 release." +
                    " Please use the pre-built binaries available in the Spine Maven repository." +
                    " See https://github.com/SpineEventEngine/base/issues/457.")
        }
    }
}
