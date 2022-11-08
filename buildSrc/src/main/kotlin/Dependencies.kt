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

@file:Suppress("UnusedReceiverParameter", "unused")

import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.GradleDoctor
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Spine.ProtoData
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

/**
 * Provides shortucts to reference our dependnecy objects.
 *
 * Dependency objects cannot be used under `plugins` section because `io` is a value
 * declared in auto-generatated `org.gradle.kotlin.dsl.PluginAccessors.kt` file.
 * It conflicts with our own declarations.
 *
 * In such cases, a shortctut to apply a plugin can be created:
 *
 * ```
 * val PluginDependenciesSpec.`gradle-doctor`: PluginDependencySpec
 *     get() = id(GradleDoctor.pluginId).version(GradleDoctor.version)
 * ```
 *
 * But for some plugins, it's impossible to apply them directly to a project.
 * For example, when a plugin is not published to Gradle Portal, it can only be
 * applied with buildscript's classpath. Thus, it's needed to leave some freedom
 * upon how to apply them. In such cases, just a shortcut to a dependency object
 * can be declared, without applyin of the plugin in-place.
 */
private const val ABOUT = ""

/**
 * Shortcut to [Spine.McJava] dependency object.
 *
 * This plugin is not published to Gradle Portal and cannot be applied directly to a project.
 * Firstly, it should be put to buildscript's classpath and then applied by ID only.
 */
val PluginDependenciesSpec.mcJava: Spine.McJava
    get() = Spine.McJava

/**
 * Shortcut to [Spine.ProtoData] dependency object.
 *
 * This plugin is in Gradle Portal. But when used in pair with [mcJava], it cannot be applied
 * directly to a project. It it so, because [mcJava] uses [protoData] as its dependency.
 * And buildscript's classpath ends up with both of them.
 */
val PluginDependenciesSpec.protoData: ProtoData
    get() = ProtoData
