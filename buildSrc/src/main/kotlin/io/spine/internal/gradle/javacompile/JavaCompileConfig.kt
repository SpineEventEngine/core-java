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

    private const val SOURCE_FILES_ENCODING = "UTF-8"
    private val EXPECTED_JAVA_VERSION = JavaVersion.VERSION_1_8
    private val COMPILATION_FLAGS = listOf(

        // Protobuf Compiler generates the code, which uses the deprecated `PARSER` field.
        // See issue: https://github.com/SpineEventEngine/config/issues/173
        // "-Werror",

        "-Xlint:unchecked",
        "-Xlint:deprecation",
    )

    fun applyTo(project: Project) {
        checkJavaVersion()
        project.tasks.withType<JavaCompile> {
            with(options) {
                encoding = SOURCE_FILES_ENCODING
                compilerArgs.addAll(COMPILATION_FLAGS)
            }
        }
    }

    private fun checkJavaVersion() {
        if (JavaVersion.current() != EXPECTED_JAVA_VERSION) {
            throw GradleException("Spine Event Engine can be built with JDK 8 only." +
                    " Supporting JDK 11 and above at build-time is planned in 2.0 release." +
                    " Please use the pre-built binaries available in the Spine Maven repository." +
                    " See https://github.com/SpineEventEngine/base/issues/457."
            )
        }
    }
}
