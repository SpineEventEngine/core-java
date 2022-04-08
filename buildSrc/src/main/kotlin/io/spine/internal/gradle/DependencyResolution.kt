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

package io.spine.internal.gradle

import io.spine.internal.dependency.AnimalSniffer
import io.spine.internal.dependency.AutoCommon
import io.spine.internal.dependency.AutoService
import io.spine.internal.dependency.AutoValue
import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.CommonsCli
import io.spine.internal.dependency.CommonsLogging
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.FindBugs
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.Gson
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.J2ObjC
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.Okio
import io.spine.internal.dependency.Plexus
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Truth
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * The function to be used in `buildscript` when a fully-qualified call must be made.
 */
@Suppress("unused")
fun doForceVersions(configurations: ConfigurationContainer) {
    configurations.forceVersions()
}

/**
 * Forces dependencies used in the project.
 */
fun NamedDomainObjectContainer<Configuration>.forceVersions() {
    all {
        resolutionStrategy {
            failOnVersionConflict()
            cacheChangingModulesFor(0, "seconds")
            forceProductionDependencies()
            forceTestDependencies()
            forceTransitiveDependencies()
        }
    }
}

private fun ResolutionStrategy.forceProductionDependencies() {
    @Suppress("DEPRECATION") // Force SLF4J version.
    force(
        AnimalSniffer.lib,
        AutoCommon.lib,
        AutoService.annotations,
        CheckerFramework.annotations,
        ErrorProne.annotations,
        ErrorProne.core,
        Guava.lib,
        FindBugs.annotations,
        Flogger.lib,
        Flogger.Runtime.systemBackend,
        Kotlin.reflect,
        Kotlin.stdLib,
        Kotlin.stdLibCommon,
        Kotlin.stdLibJdk8,
        Protobuf.libs,
        Protobuf.GradlePlugin.lib,
        io.spine.internal.dependency.Slf4J.lib
    )
}

private fun ResolutionStrategy.forceTestDependencies() {
    force(
        Guava.testLib,
        JUnit.api,
        JUnit.platformCommons,
        JUnit.platformLauncher,
        JUnit.legacy,
        Truth.libs
    )
}

/**
 * Forces transitive dependencies of 3rd party components that we don't use directly.
 */
private fun ResolutionStrategy.forceTransitiveDependencies() {
    force(
        AutoValue.annotations,
        Gson.lib,
        J2ObjC.annotations,
        Plexus.utils,
        Okio.lib,
        CommonsCli.lib,
        CheckerFramework.compatQual,
        CommonsLogging.lib
    )
}

fun NamedDomainObjectContainer<Configuration>.excludeProtobufLite() {

    fun excludeProtoLite(configurationName: String) {
        named(configurationName).get().exclude(
            mapOf(
                "group" to "com.google.protobuf",
                "module" to "protobuf-lite"
            )
        )
    }

    excludeProtoLite("runtimeOnly")
    excludeProtoLite("testRuntimeOnly")
}

@Suppress("unused")
object DependencyResolution {
    @Deprecated(
        "Please use `configurations.forceVersions()`.",
        ReplaceWith("configurations.forceVersions()")
    )
    fun forceConfiguration(configurations: ConfigurationContainer) {
        configurations.forceVersions()
    }

    @Deprecated(
        "Please use `configurations.excludeProtobufLite()`.",
        ReplaceWith("configurations.excludeProtobufLite()")
    )
    fun excludeProtobufLite(configurations: ConfigurationContainer) {
        configurations.excludeProtobufLite()
    }

    @Deprecated(
        "Please use `applyStandard(repositories)` instead.",
        replaceWith = ReplaceWith("applyStandard(repositories)")
    )
    fun defaultRepositories(repositories: RepositoryHandler) {
        repositories.applyStandard()
    }
}
