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

@file:Suppress("UnusedReceiverParameter", "unused", "TopLevelPropertyNaming", "ObjectPropertyName")

import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.GradleDoctor
import io.spine.internal.dependency.Kotest
import io.spine.internal.dependency.Kover
import io.spine.internal.dependency.ProtoData
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.standardToSpineSdk
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

/**
 * Applies [standard][standardToSpineSdk] repositories to this `buildscript`.
 */
fun ScriptHandlerScope.standardSpineSdkRepositories() {
    repositories.standardToSpineSdk()
}

/**
 * Provides shortcuts to reference our dependency objects.
 *
 * Dependency objects cannot be used under `plugins` section because `io` is a value
 * declared in auto-generated `org.gradle.kotlin.dsl.PluginAccessors.kt` file.
 * It conflicts with our own declarations.
 *
 * In such cases, a shortcut to apply a plugin can be created:
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
 * can be declared, without applying of the plugin in-place.
 */
private const val ABOUT_DEPENDENCY_EXTENSIONS = ""

/**
 * Shortcut to [Spine.McJava] dependency object.
 *
 * This plugin is not published to Gradle Portal and cannot be applied directly to a project.
 * Firstly, it should be put to buildscript's classpath and then applied by ID only.
 */
val PluginDependenciesSpec.mcJava: Spine.McJava
    get() = Spine.McJava

/**
 * Shortcut to [ProtoData] dependency object.
 *
 * This plugin is in Gradle Portal. But when used in pair with [mcJava], it cannot be applied
 * directly to a project. It is so, because [mcJava] uses [protoData] as its dependency.
 * And buildscript's classpath ends up with both of them.
 */
val PluginDependenciesSpec.protoData: ProtoData
    get() = ProtoData

/**
 * Provides shortcuts for applying plugins from our dependency objects.
 *
 * Dependency objects cannot be used under `plugins` section because `io` is a value
 * declared in auto-generated `org.gradle.kotlin.dsl.PluginAccessors.kt` file.
 * It conflicts with our own declarations.
 *
 * Declaring of top-level shortcuts eliminates need in applying plugins
 * using fully-qualified name of dependency objects.
 *
 * It is still possible to apply a plugin with a custom version, if needed.
 * Just declare a version again on the returned [PluginDependencySpec].
 *
 * For example:
 *
 * ```
 * plugins {
 *     protobuf version("0.8.19-custom")
 * }
 * ```
 */
private const val ABOUT_PLUGIN_ACCESSORS = ""

val PluginDependenciesSpec.errorprone: PluginDependencySpec
    get() = id(ErrorProne.GradlePlugin.id)

val PluginDependenciesSpec.protobuf: PluginDependencySpec
    get() = id(Protobuf.GradlePlugin.id)

val PluginDependenciesSpec.`gradle-doctor`: PluginDependencySpec
    get() = id(GradleDoctor.pluginId).version(GradleDoctor.version)

val PluginDependenciesSpec.kotest: PluginDependencySpec
    get() = Kotest.MultiplatformGradlePlugin.let {
        return id(it.id).version(it.version)
    }

val PluginDependenciesSpec.kover: PluginDependencySpec
    get() = id(Kover.id).version(Kover.version)

/**
 * Configures the dependencies between third-party Gradle tasks
 * and those defined via ProtoData and Spine Model Compiler.
 *
 * It is required in order to avoid warnings in build logs, detecting the undeclared
 * usage of Spine-specific task output by other tasks,
 * e.g. the output of `launchProtoData` is used by `compileKotlin`.
 */
@Suppress("unused")
fun Project.configureTaskDependencies() {

    /**
     * Creates a dependency between the Gradle task of *this* name
     * onto the task with `taskName`.
     *
     * If either of tasks does not exist in the enclosing `Project`,
     * this method does nothing.
     *
     * This extension is kept local to `configureTaskDependencies` extension
     * to prevent its direct usage from outside.
     */
    fun String.dependOn(taskName: String) {
        val whoDepends = this
        val dependOntoTask: Task? = tasks.findByName(taskName)
        dependOntoTask?.let {
            tasks.findByName(whoDepends)?.dependsOn(it)
        }
    }

    afterEvaluate {
        val launchProtoData = "launchProtoData"
        val launchTestProtoData = "launchTestProtoData"
        val generateProto = "generateProto"
        val createVersionFile = "createVersionFile"
        "compileKotlin".dependOn(launchProtoData)
        "compileTestKotlin".dependOn(launchTestProtoData)
        val sourcesJar = "sourcesJar"
        sourcesJar.dependOn(generateProto)
        sourcesJar.dependOn(launchProtoData)
        sourcesJar.dependOn(createVersionFile)
        sourcesJar.dependOn("prepareProtocConfigVersions")
        val dokkaHtml = "dokkaHtml"
        dokkaHtml.dependOn(generateProto)
        dokkaHtml.dependOn(launchProtoData)
        "dokkaJavadoc".dependOn(launchProtoData)
        "publishPluginJar".dependOn(createVersionFile)
    }
}
