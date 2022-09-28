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

package io.spine.internal.dependency

// https://errorprone.info/
@Suppress("unused")
object ErrorProne {
    // https://github.com/google/error-prone
    private const val version = "2.13.1"
    // https://github.com/tbroyer/gradle-errorprone-plugin/blob/v0.8/build.gradle.kts
    private const val javacPluginVersion = "9+181-r4173-1"

    val annotations = listOf(
        "com.google.errorprone:error_prone_annotations:${version}",
        "com.google.errorprone:error_prone_type_annotations:${version}"
    )
    const val core = "com.google.errorprone:error_prone_core:${version}"
    const val checkApi = "com.google.errorprone:error_prone_check_api:${version}"
    const val testHelpers = "com.google.errorprone:error_prone_test_helpers:${version}"
    const val javacPlugin  = "com.google.errorprone:javac:${javacPluginVersion}"

    // https://github.com/tbroyer/gradle-errorprone-plugin/releases
    object GradlePlugin {
        const val id = "net.ltgt.errorprone"
        /**
         * The version of this plugin is already specified in `buildSrc/build.gradle.kts` file.
         * Thus, when applying the plugin in projects build files, only the [id] should be used.
         *
         * When the plugin is used as a library (e.g. in tools), its version and the library
         * artifacts are of importance.
         */
        const val version = "2.0.2"
        const val lib = "net.ltgt.gradle:gradle-errorprone-plugin:${version}"
    }
}
