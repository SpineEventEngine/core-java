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

// Those accessors should match Gradle's naming conventions.
@file:Suppress("TopLevelPropertyNaming", "unused")

import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.GradleDoctor
import io.spine.internal.dependency.Protobuf
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

/**
 * Provides shortucts for applying plugins from our dependnecy objects.
 *
 * Dependency objects cannot be used under `plugins` section because `io` is a value
 * declared in auto-generatated `org.gradle.kotlin.dsl.PluginAccessors.kt` file.
 * It conflicts with our own declarations.
 *
 * Declaring of top-level shortucts eliminates need in applying plugins
 * using fully-qualified name of dependency objects.
 *
 * It is still possible to apply a plugin with a custom version, if needed.
 * Just delcate a version again on the returned [PluginDependencySpec].
 *
 * For example:
 *
 * ```
 * plugins {
 *     protobuf version("0.8.19-custom")
 * }
 * ```
 */
private const val ABOUT = ""

val PluginDependenciesSpec.errorprone: PluginDependencySpec
    get() = id(ErrorProne.GradlePlugin.id)

val PluginDependenciesSpec.protobuf: PluginDependencySpec
    get() = id(Protobuf.GradlePlugin.id)

val PluginDependenciesSpec.`gradle-doctor`: PluginDependencySpec
    get() = id(GradleDoctor.pluginId).version(GradleDoctor.version)
