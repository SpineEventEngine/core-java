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

@file:Suppress("RemoveRedundantQualifierName")

import Build_gradle.Subproject
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Validation
import io.spine.internal.gradle.VersionWriter
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.github.pages.updateGitHubPages
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.standardToSpineSdk
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    standardSpineSdkRepositories()
    doForceVersions(configurations)
    configurations {
        all {
            resolutionStrategy {
                val spine = io.spine.internal.dependency.Spine
                force(
                    spine.base,
                    spine.toolBase,
                    spine.server,
                    io.spine.internal.dependency.Spine.Logging.lib,
                    io.spine.internal.dependency.Spine.Logging.backend,
                    io.spine.internal.dependency.Spine.Logging.floggerApi,
                    io.spine.internal.dependency.Validation.runtime,
                )
            }
        }
    }

    dependencies {
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib)
    }
}

plugins {
    `java-library`
    kotlin("jvm")
    idea
    protobuf
    errorprone
    `gradle-doctor`
}

object BuildSettings {
    const val JAVA_VERSION = 11
}

repositories.standardToSpineSdk()

spinePublishing {
    modules = setOf(
        "core",
        "client",
        "server",
        "testutil-core",
        "testutil-client",
        "testutil-server",
    )

    destinations = with(PublishingRepos) {
        setOf(
            cloudRepo,
            gitHub("core-java"),
            cloudArtifactRegistry
        )
    }

    testJar {
        inclusions = setOf("server")
    }

    dokkaJar {
        kotlin = true
        java = true
    }
}

allprojects {
    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")
    }

    apply(from = "$rootDir/version.gradle.kts")
    group = "io.spine"
    version = extra["versionToPublish"]!!
}

subprojects {
    repositories.standardToSpineSdk()
    applyPlugins()

    val javaVersion = JavaLanguageVersion.of(BuildSettings.JAVA_VERSION)
    setupJava(javaVersion)
    setupKotlin(javaVersion)

    defineDependencies()
    forceConfigurations()

    val generated = "$projectDir/generated"
    applyGeneratedDirectories(generated)
    setupTestTasks()
    setupPublishing()
    configureTaskDependencies()
}

JacocoConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)

/**
 * The alias for typed extensions functions related to subprojects.
 */
typealias Subproject = Project

/**
 * Applies plugins common to all modules to this subproject.
 */
fun Subproject.applyPlugins() {
    apply {
        plugin("java-library")
        plugin("jacoco")
        plugin("com.google.protobuf")
        plugin("net.ltgt.errorprone")
        plugin("kotlin")
        plugin("pmd")
        plugin("maven-publish")
        plugin("pmd-settings")
        plugin("dokka-for-java")
        plugin("io.spine.mc-java")
    }

    apply<IncrementGuard>()
    apply<VersionWriter>()

    LicenseReporter.generateReportIn(project)
    JavadocConfig.applyTo(project)
    CheckStyleConfig.applyTo(project)
}

/**
 * Configures Java tasks in this project.
 */
fun Subproject.setupJava(javaVersion: JavaLanguageVersion) {
    java {
        toolchain.languageVersion.set(javaVersion)
    }
    tasks {
        withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
        }
        withType<Jar>().configureEach {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

/**
 * Configures Kotlin tasks in this project.
 */
fun Subproject.setupKotlin(javaVersion: JavaLanguageVersion) {
    kotlin {
        applyJvmToolchain(javaVersion.asInt())
        explicitApi()

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = javaVersion.toString()
            setFreeCompilerArgs()
        }
    }
}

/**
 * Configures test tasks in this project.
 */
fun Subproject.setupTestTasks() {
    tasks {
        registerTestTasks()
        test {
            useJUnitPlatform { includeEngines("junit-jupiter") }
            configureLogging()
        }
    }
}

/**
 * Defines dependencies of this subproject.
 */
fun Subproject.defineDependencies() {
    dependencies {
        ErrorProne.apply {
            errorprone(core)
        }
        // Strangely, Gradle does not see `protoData` via DSL here, so we add using the string.
        add("protoData", Validation.java)
        implementation(Validation.runtime)

        testImplementation(JUnit.runner)
        testImplementation(Spine.testlib)
    }
}

/**
 * Adds directories with the generated source code to source sets of the project and
 * to IntelliJ IDEA module settings.
 *
 * @param generatedDir
 *          the name of the root directory with the generated code
 */
fun Subproject.applyGeneratedDirectories(generatedDir: String) {
    val generatedMain = "$generatedDir/main"
    val generatedJava = "$generatedMain/java"
    val generatedKotlin = "$generatedMain/kotlin"
    val generatedGrpc = "$generatedMain/grpc"
    val generatedSpine = "$generatedMain/spine"

    val generatedTest = "$generatedDir/test"
    val generatedTestJava = "$generatedTest/java"
    val generatedTestKotlin = "$generatedTest/kotlin"
    val generatedTestGrpc = "$generatedTest/grpc"
    val generatedTestSpine = "$generatedTest/spine"

    sourceSets {
        main {
            java.srcDirs(
                generatedJava,
                generatedGrpc,
                generatedSpine,
            )
            kotlin.srcDirs(
                generatedKotlin,
            )
        }
        test {
            java.srcDirs(
                generatedTestJava,
                generatedTestGrpc,
                generatedTestSpine,
            )
            kotlin.srcDirs(
                generatedTestKotlin,
            )
        }
    }

    idea {
        module {
            generatedSourceDirs.addAll(files(
                    generatedJava,
                    generatedKotlin,
                    generatedGrpc,
                    generatedSpine,
            ))
            testSources.from(
                generatedTestJava,
                generatedTestKotlin,
                generatedTestGrpc,
                generatedTestSpine,
            )
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
}

/**
 * Forces dependencies of this project.
 */
fun Subproject.forceConfigurations() {
    configurations {
        forceVersions()
        excludeProtobufLite()

        all {
            resolutionStrategy {
                exclude("io.spine", "spine-validate")
                force(
                    /* Force the version of gRPC used by the `:client` module over the one
                       set by `mc-java` in the `:core` module when specifying compiler artifact
                       for the gRPC plugin.
                       See `io.spine.tools.mc.java.gradle.plugins.JavaProtocConfigurationPlugin
                       .configureProtocPlugins()` method which sets the version from resources. */
                    Grpc.ProtocPlugin.artifact,
                    Grpc.api,
                    JUnit.runner,

                    Spine.base,
                    Validation.runtime,
                    Spine.time,
                    Spine.Logging.lib,
                    Spine.Logging.backend,
                    Spine.Logging.floggerApi,
                    Spine.baseTypes,
                    Spine.change,
                    Spine.testlib,
                    Spine.toolBase,
                    Spine.pluginBase,

                    Grpc.core,
                    Grpc.protobuf,
                    Grpc.stub
                )
            }
        }
    }
}

/**
 * Configures publishing for this subproject.
 */
fun Subproject.setupPublishing() {
    updateGitHubPages(project.version.toString()) {
        allowInternalJavadoc.set(true)
        rootFolder.set(rootDir)
    }

    tasks.named("publish") {
        dependsOn("${project.path}:updateGitHubPages")
    }
}
