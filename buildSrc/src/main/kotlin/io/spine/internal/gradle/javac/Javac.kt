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

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider

/**
 * Configures the `javac` tool through this `JavaCompile` task.
 *
 * There are several steps performed:
 *
 *  1. Ensures JDK 8 is used for compilation;
 *  2. Passes a couple of arguments to the compiler. See [JavacConfig] for more details;
 *  3. Sets the UTF-8 encoding to be used when reading Java source files.
 *
 * Please note that Spine Event Engine can be built with JDK 8 only.
 * Supporting JDK 11 and above at build-time is planned in 2.0 release.
 *
 * Here's an example of how to use it:
 *
 *```
 * tasks {
 *     withType<JavaCompile> {
 *         configureJavac()
 *     }
 * }
 *```
 */
fun JavaCompile.configureJavac() {

    if (JavaVersion.current() != JavacConfig.EXPECTED_JAVA_VERSION) {
        throw GradleException("Spine Event Engine can be built with JDK 8 only." +
                " Supporting JDK 11 and above at build-time is planned in 2.0 release." +
                " Please use the pre-built binaries available in the Spine Maven repository." +
                " See https://github.com/SpineEventEngine/base/issues/457."
        )
    }

    with(options) {
        encoding = JavacConfig.SOURCE_FILES_ENCODING
        compilerArgumentProviders.add(JavacConfig.ARGUMENTS)
    }
}

/**
 * The knowledge that is required to set up `javac`.
 */
private object JavacConfig {
    const val SOURCE_FILES_ENCODING = "UTF-8"
    val EXPECTED_JAVA_VERSION = JavaVersion.VERSION_1_8
    val ARGUMENTS = CommandLineArgumentProvider {
        listOf(

            // Protobuf Compiler generates the code, which uses the deprecated `PARSER` field.
            // See issue: https://github.com/SpineEventEngine/config/issues/173
            // "-Werror",

            "-Xlint:unchecked",
            "-Xlint:deprecation",
        )
    }
}
