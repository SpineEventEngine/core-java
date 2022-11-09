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

@file:Suppress("RemoveRedundantQualifierName")

import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.VersionWriter
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.applyStandardWithGitHub
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.github.pages.updateGitHubPages
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.protobuf.suppressDeprecationsInKotlin
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.tools.mc.gradle.ModelCompilerOptions
import io.spine.tools.mc.java.gradle.McJavaOptions
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    io.spine.internal.gradle.doForceVersions(configurations)
    io.spine.internal.gradle.applyWithStandard(this, rootProject,
        "base",
        "time",
        "tool-base"
    )

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
    /**
     * Temporarily use this version, since 3.21.x is known to provide
     * a broken `protoc-gen-js` artifact and Kotlin code without access modifiers.
     *
     * @see <a href="https://github.com/protocolbuffers/protobuf-javascript/issues/127">
     *      protobuf-javascript#127</a>
     * @see <a href="https://github.com/protocolbuffers/protobuf/issues/10593">
     *      protobuf#10593</a>
     */
    const val protocArtifact = "com.google.protobuf:protoc:3.19.6"

    const val JAVA_VERSION = 11
}

repositories.applyStandard()

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
        enabled = true
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
    applyRepositories()
    applyPlugins()

    val javaVersion = JavaLanguageVersion.of(BuildSettings.JAVA_VERSION)
    setupJava(javaVersion)
    setupKotlin(javaVersion)

    val spine = Spine(this)
    defineDependencies(spine)
    forceConfigurations(spine)

    val generated = "$projectDir/generated"
    applyGeneratedDirectories(generated)
    setupTestTasks()
    setupCodeGeneration(generated)
    setupPublishing()
    addTaskDependencies()
}

JacocoConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)

/**
 * The alias for typed extensions functions related to subprojects.
 */
typealias Subproject = Project

/**
 * Defines repositories to be used in subprojects when resolving artifacts.
 */
fun Subproject.applyRepositories() {
    repositories.applyStandardWithGitHub(
        project,
        "base", "time", "base-types", "change", "validation", "ProtoData", "mc-java",
    )
}

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
        plugin("io.spine.protodata")
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
fun Subproject.defineDependencies(spine: Spine) {
    dependencies {
        ErrorProne.apply {
            errorprone(core)
        }
        // Strangely, Gradle does not see `protoData` via DSL here, so we add using the string.
        add("protoData", spine.validation.java)
        implementation(spine.validation.runtime)

        testImplementation(JUnit.runner)
        testImplementation(spine.testlib)
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
 * Configures code generation in this project.
 */
fun Subproject.setupCodeGeneration(generatedDir: String) {
    /**
     * The below arrangement is "unusual" `because:
     *  1. `modelCompiler` could not be found after applying plugin via `apply { }` block.
     *  2. java { }` cannot be used because it conflicts with `java` of type `JavaPluginExtension`
     *     already added to the `Project`.
     */
    val modelCompiler = extensions.getByType(ModelCompilerOptions::class.java)
    modelCompiler.apply {
        // Get nested `this` instead of `Project` instance.
        val mcOptions = (this@apply as ExtensionAware)
        val java = mcOptions.extensions.getByName("java") as McJavaOptions
        java.codegen {
            validation { skipValidation() }
        }
    }

    val protoData = extensions.getByName("protoData") as CodegenSettings
    protoData.apply {
        renderers(
            "io.spine.validation.java.PrintValidationInsertionPoints",
            "io.spine.validation.java.JavaValidationRenderer",

            // Suppress warnings in the generated code.
            "io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration",
            "io.spine.protodata.codegen.java.suppress.SuppressRenderer"
        )
        plugins(
            "io.spine.validation.ValidationPlugin",
        )
    }

    /**
     * Temporarily use this version, since 3.21.x is known to provide
     * a broken `protoc-gen-js` artifact.
     *
     * See https://github.com/protocolbuffers/protobuf-javascript/issues/127.
     * Once it is addressed, this artifact should be `Protobuf.compiler`.
     *
     * Also, this fixes the explicit API more for the generated Kotlin code.
     */
    protobuf {
        // Do not remove this setting until ProtoData can copy all the directories from
        // `build/generated-proto`. Otherwise, the GRPC code won't be picked up.
        // See: https://github.com/SpineEventEngine/ProtoData/issues/94
        generatedFilesBaseDir = generatedDir
        protoc { artifact = BuildSettings.protocArtifact }
    }

    /**
     * Manually suppress deprecations in the generated Kotlin code until ProtoData does it.
     */
    tasks.withType<LaunchProtoData>().forEach { task ->
        task.doLast {
            sourceSets.forEach { sourceSet ->
                suppressDeprecationsInKotlin(generatedDir, sourceSet.name)
            }
        }
    }
}

/**
 * Forces dependencies of this project.
 */
fun Subproject.forceConfigurations(spine: Spine) {
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
                    Grpc.protobufPlugin,

                    spine.base,
                    spine.validation.runtime,
                    spine.time,
                    spine.baseTypes,
                    spine.change,
                    spine.testlib,
                    spine.toolBase,
                    spine.pluginBase,

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

/**
 * Adds explicit dependencies for the tasks of this subproject.
 */
fun Subproject.addTaskDependencies() {
    tasks {
        val generateRejections by existing
        compileKotlin {
            dependsOn(generateRejections)
        }

        val generateTestRejections by existing
        compileTestKotlin {
            dependsOn(generateTestRejections)
        }
    }
    configureTaskDependencies()
}
