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

package io.spine.internal.dependency

// https://github.com/JetBrains/kotlin
// https://github.com/Kotlin
@Suppress("unused", "ConstPropertyName")
object Kotlin {

    /**
     * When changing the version, also change the version used in the `buildSrc/build.gradle.kts`.
     */
    @Suppress("MemberVisibilityCanBePrivate") // used directly from outside
    const val version = "1.9.20"

    /**
     * The version of the JetBrains annotations library, which is a transitive
     * dependency for us via Kotlin libraries.
     *
     * @see <a href="https://github.com/JetBrains/java-annotations">Java Annotations</a>
     */
    private const val annotationsVersion = "24.0.1"

    private const val group = "org.jetbrains.kotlin"

    const val stdLib       = "$group:kotlin-stdlib:$version"
    const val stdLibCommon = "$group:kotlin-stdlib-common:$version"

    @Deprecated("Please use `stdLib` instead.")
    const val stdLibJdk7   = "$group:kotlin-stdlib-jdk7:$version"

    @Deprecated("Please use `stdLib` instead.")
    const val stdLibJdk8   = "$group:kotlin-stdlib-jdk8:$version"

    const val reflect    = "$group:kotlin-reflect:$version"
    const val testJUnit5 = "$group:kotlin-test-junit5:$version"

    const val gradlePluginApi = "$group:kotlin-gradle-plugin-api:$version"
    const val gradlePluginLib = "$group:kotlin-gradle-plugin:$version"

    const val jetbrainsAnnotations = "org.jetbrains:annotations:$annotationsVersion"
}
