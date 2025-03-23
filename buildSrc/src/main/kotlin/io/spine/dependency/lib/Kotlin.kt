/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.dependency.lib

// https://github.com/JetBrains/kotlin
// https://github.com/Kotlin
@Suppress("unused", "ConstPropertyName")
object Kotlin {

    /**
     * This is the version of Kotlin we use for writing code which does not
     * depend on Gradle and the version of embedded Kotlin.
     */
    @Suppress("MemberVisibilityCanBePrivate") // used directly from the outside.
    const val runtimeVersion = "2.1.20"

    /**
     * This is the version of
     * [Kotlin embedded into Gradle](https://docs.gradle.org/current/userguide/compatibility.html#kotlin).
     */
    const val embeddedVersion = "2.0.21"

    /**
     * The version of the JetBrains annotations library, which is a transitive
     * dependency for us via Kotlin libraries.
     *
     * @see <a href="https://github.com/JetBrains/java-annotations">Java Annotations</a>
     */
    private const val annotationsVersion = "26.0.2"

    private const val group = "org.jetbrains.kotlin"

    const val scriptRuntime = "$group:kotlin-script-runtime:$runtimeVersion"
    const val stdLib       = "$group:kotlin-stdlib:$runtimeVersion"
    const val stdLibCommon = "$group:kotlin-stdlib-common:$runtimeVersion"

    const val toolingCore = "$group:kotlin-tooling-core:$runtimeVersion"

    @Deprecated("Please use `stdLib` instead.")
    const val stdLibJdk7   = "$group:kotlin-stdlib-jdk7:$runtimeVersion"

    @Deprecated("Please use `stdLib` instead.")
    const val stdLibJdk8   = "$group:kotlin-stdlib-jdk8:$runtimeVersion"

    const val reflect    = "$group:kotlin-reflect:$runtimeVersion"
    const val testJUnit5 = "$group:kotlin-test-junit5:$runtimeVersion"

    @Deprecated(message = "Please use `GradlePlugin.api` instead.", ReplaceWith("GradlePlugin.api"))
    const val gradlePluginApi = "$group:kotlin-gradle-plugin-api:$runtimeVersion"

    @Deprecated(message = "Please use `GradlePlugin.lib` instead.", ReplaceWith("GradlePlugin.lib"))
    const val gradlePluginLib = "$group:kotlin-gradle-plugin:$runtimeVersion"

    const val jetbrainsAnnotations = "org.jetbrains:annotations:$annotationsVersion"

    object Compiler {
        const val embeddable = "$group:kotlin-compiler-embeddable:$runtimeVersion"
    }

    object GradlePlugin {
        const val version = runtimeVersion
        const val api = "$group:kotlin-gradle-plugin-api:$version"
        const val lib = "$group:kotlin-gradle-plugin:$version"
        const val model = "$group:kotlin-gradle-model:$version"
    }
}
