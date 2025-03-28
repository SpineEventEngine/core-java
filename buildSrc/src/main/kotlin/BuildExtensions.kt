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

@file:Suppress("UnusedReceiverParameter", "unused", "TopLevelPropertyNaming", "ObjectPropertyName")

import io.spine.dependency.build.ErrorProne
import io.spine.dependency.build.GradleDoctor
import io.spine.dependency.build.Ksp
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.McJava
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.ProtoTap
import io.spine.dependency.test.Kotest
import io.spine.dependency.test.Kover
import io.spine.gradle.standardToSpineSdk
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

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
 * But for some plugins, it is impossible to apply them directly to a project.
 * For example, when a plugin is not published to Gradle Portal, it can only be
 * applied with the buildscript's classpath. Thus, it is necessary to leave some freedom
 * upon how to apply them. In such cases, just a shortcut to a dependency object
 * can be declared without applying the plugin in-place.
 */
private const val ABOUT_DEPENDENCY_EXTENSIONS = ""

/**
 * Applies [standard][standardToSpineSdk] repositories to this `buildscript`.
 */
fun ScriptHandlerScope.standardSpineSdkRepositories() {
    repositories.standardToSpineSdk()
}

/**
 * Shortcut to [McJava] dependency object for using under `buildScript`.
 */
val ScriptHandlerScope.mcJava: McJava
    get() = McJava

/**
 * Shortcut to [McJava] dependency object.
 *
 * This plugin is not published to Gradle Portal and cannot be applied directly to a project.
 * Firstly, it should be put to buildscript's classpath and then applied by ID only.
 */
val PluginDependenciesSpec.mcJava: McJava
    get() = McJava

/**
 * Shortcut to [ProtoData] dependency object for using under `buildscript`.
 */
val ScriptHandlerScope.protoData: ProtoData
    get() = ProtoData

/**
 * Shortcut to [ProtoData] dependency object.
 *
 * This plugin is published at Gradle Plugin Portal.
 * But when used in a pair with [mcJava], it cannot be applied directly to a project.
 * It is so, because [mcJava] uses [protoData] as its dependency.
 * And the buildscript's classpath ends up with both of them.
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
 * Declaring of top-level shortcuts eliminates the need to apply plugins
 * using a fully qualified name of dependency objects.
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

val PluginDependenciesSpec.prototap: PluginDependencySpec
    get() = id(ProtoTap.gradlePluginId).version(ProtoTap.version)

val PluginDependenciesSpec.`gradle-doctor`: PluginDependencySpec
    get() = id(GradleDoctor.pluginId).version(GradleDoctor.version)

val PluginDependenciesSpec.kotest: PluginDependencySpec
    get() = Kotest.MultiplatformGradlePlugin.let {
        return id(it.id).version(it.version)
    }

val PluginDependenciesSpec.kover: PluginDependencySpec
    get() = id(Kover.id).version(Kover.version)

val PluginDependenciesSpec.ksp: PluginDependencySpec
    get() = id(Ksp.id).version(Ksp.version)

/**
 * Configures the dependencies between third-party Gradle tasks
 * and those defined via ProtoData and Spine Model Compiler.
 *
 * It is required to avoid warnings in build logs, detecting the undeclared
 * usage of Spine-specific task output by other tasks,
 * e.g., the output of `launchProtoData` is used by `compileKotlin`.
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
        val compileKotlin = "compileKotlin"
        compileKotlin.run {
            dependOn(generateProto)
            dependOn(launchProtoData)
        }
        val compileTestKotlin = "compileTestKotlin"
        compileTestKotlin.dependOn(launchTestProtoData)
        val sourcesJar = "sourcesJar"
        val kspKotlin = "kspKotlin"
        sourcesJar.run {
            dependOn(generateProto)
            dependOn(launchProtoData)
            dependOn(kspKotlin)
            dependOn(createVersionFile)
            dependOn("prepareProtocConfigVersions")
        }
        val dokkaHtml = "dokkaHtml"
        dokkaHtml.run {
            dependOn(generateProto)
            dependOn(launchProtoData)
            dependOn(kspKotlin)
        }
        val dokkaJavadoc = "dokkaJavadoc"
        dokkaJavadoc.run {
            dependOn(launchProtoData)
            dependOn(kspKotlin)
        }
        "publishPluginJar".dependOn(createVersionFile)
        compileKotlin.dependOn(kspKotlin)
        compileTestKotlin.dependOn("kspTestKotlin")
        "compileTestFixturesKotlin".dependOn("kspTestFixturesKotlin")
        "javadocJar".dependOn(dokkaHtml)
        "dokkaKotlinJar".dependOn(dokkaJavadoc)
    }
}

/**
 * Obtains all modules names of which do not have `"-tests"` as the suffix.
 *
 * By convention, such modules are for integration tests and should be treated differently.
 */
val Project.productionModules: Iterable<Project>
    get() = rootProject.subprojects.filter { !it.name.contains("-tests") }

/**
 * Sets the remote debug option for this [JavaExec] task.
 *
 * The port number is [5566][BuildSettings.REMOTE_DEBUG_PORT].
 *
 * @param enabled If `true` the task will be suspended.
 */
fun JavaExec.remoteDebug(enabled: Boolean = true) {
    debugOptions {
        this@debugOptions.enabled.set(enabled)
        port.set(BuildSettings.REMOTE_DEBUG_PORT)
        server.set(true)
        suspend.set(true)
    }
}

/**
 * Sets the remote debug option for the task of [JavaExec] type with the given name.
 *
 * The port number is [5566][BuildSettings.REMOTE_DEBUG_PORT].
 *
 * @param enabled If `true` the task will be suspended.
 * @throws IllegalStateException if the task with the given name is not found, or,
 *  if the taks is not of [JavaExec] type.
 */
fun Project.setRemoteDebug(taskName: String, enabled: Boolean = true) {
    val task = tasks.findByName(taskName)
    check(task != null) {
        "Could not find a task named `$taskName` in the project `$name`."
    }
    check(task is JavaExec) {
        "The task `$taskName` is not of type `JavaExec`."
    }
    task.remoteDebug(enabled)
}

/**
 * Sets remote debug options for the `launchProtoData` task.
 *
 * @param enabled if `true` the task will be suspended.
 *
 * @see remoteDebug
 */
fun Project.protoDataRemoteDebug(enabled: Boolean = true) =
    setRemoteDebug("launchProtoData", enabled)

/**
 * Sets remote debug options for the `launchTestProtoData` task.
 *
 * @param enabled if `true` the task will be suspended.
 *
 * @see remoteDebug
 */
fun Project.testProtoDataRemoteDebug(enabled: Boolean = true) =
    setRemoteDebug("launchTestProtoData", enabled)

/**
 * Sets remote debug options for the `launchTestFixturesProtoData` task.
 *
 * @param enabled if `true` the task will be suspended.
 *
 * @see remoteDebug
 */
fun Project.testFixturesProtoDataRemoteDebug(enabled: Boolean = true) =
    setRemoteDebug("launchTestFixturesProtoData", enabled)
