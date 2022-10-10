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

import io.spine.internal.dependency.Dokka
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.JUnit
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.VersionWriter
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.github.pages.updateGitHubPages
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    apply(from = "$rootDir/version.gradle.kts")

    io.spine.internal.gradle.doApplyStandard(repositories)
    io.spine.internal.gradle.doApplyGitHubPackages(repositories, "base", rootProject)

    val kotlinVersion = io.spine.internal.dependency.Kotlin.version
    val spineBaseVersion: String by extra
    val spineTimeVersion: String by extra
    val toolBaseVersion: String by extra
    val mcJavaVersion: String by extra

    dependencies {
        classpath("io.spine.tools:spine-mc-java-plugins:${mcJavaVersion}:all")
    }

    io.spine.internal.gradle.doForceVersions(configurations)
    configurations.all {
        resolutionStrategy {
            force(
                    "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
                    "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion",
                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine:spine-time:$spineTimeVersion",
                    "io.spine.tools:spine-tool-base:$toolBaseVersion"
            )
        }
    }
}

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
plugins {
    `java-library`
    kotlin("jvm")
    idea
    id(io.spine.internal.dependency.Protobuf.GradlePlugin.id)
    id(io.spine.internal.dependency.ErrorProne.GradlePlugin.id)
}

repositories.applyStandard()

apply(from = "$rootDir/version.gradle.kts")
val spineBaseVersion: String by extra
val validationVersion: String by extra
val spineTimeVersion: String by extra
val toolBaseVersion: String by extra
val spineBaseTypesVersion: String by extra

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

    apply {
        from("$rootDir/version.gradle.kts")
    }

    group = "io.spine"
    version = extra["versionToPublish"]!!
}

subprojects {

    repositories.applyStandard()

    apply {
        plugin("java-library")
        plugin("jacoco")
        plugin("com.google.protobuf")
        plugin("net.ltgt.errorprone")
        plugin("io.spine.mc-java")
        plugin("kotlin")
        plugin("pmd")
        plugin("maven-publish")
        plugin("pmd-settings")
        plugin("dokka-for-java")
    }

    java {
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

    kotlin {
        val javaVersion = JavaVersion.VERSION_11.toString()

        applyJvmToolchain(javaVersion)
        explicitApi()

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = javaVersion
            setFreeCompilerArgs()
        }
    }

    dependencies {
        ErrorProne.apply {
            errorprone(core)
        }

        api("io.spine:spine-base:$spineBaseVersion")
        api("io.spine:spine-time:$spineTimeVersion")

        testImplementation(JUnit.runner)
        testImplementation("io.spine.tools:spine-testlib:$spineBaseVersion")
    }

    configurations {
        forceVersions()
        excludeProtobufLite()

        all {
            resolutionStrategy {
                exclude("io.spine:spine-validate:$spineBaseVersion")
                force(
                    "org.jetbrains.dokka:dokka-base:${Dokka.version}",
                    "org.jetbrains.dokka:dokka-analysis:${Dokka.version}",
                    /* Force the version of gRPC used by the `:client` module over the one
                       set by `mc-java` in the `:core` module when specifying compiler artifact
                       for the gRPC plugin.
                       See `io.spine.tools.mc.java.gradle.plugins.JavaProtocConfigurationPlugin
                       .configureProtocPlugins() method which sets the version from resources. */
                    "io.grpc:protoc-gen-grpc-java:${Grpc.version}",

                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine.validation:spine-validation-java-runtime:$validationVersion",
                    "io.spine:spine-time:$spineTimeVersion",
                    "io.spine:spine-base-types:$spineBaseTypesVersion",
                    "io.spine.tools:spine-testlib:$spineBaseVersion",
                    "io.spine.tools:spine-plugin-base:$toolBaseVersion",
                    "io.spine.tools:spine-tool-base:$toolBaseVersion",
                    Grpc.core,
                    Grpc.protobuf,
                    Grpc.stub
                )
            }
        }
    }

    val generatedDir = "$projectDir/generated"
    val generatedJavaDir = "$generatedDir/main/java"
    val generatedTestJavaDir = "$generatedDir/test/java"
    val generatedGrpcDir = "$generatedDir/main/grpc"
    val generatedTestGrpcDir = "$generatedDir/test/grpc"
    val generatedSpineDir = "$generatedDir/main/spine"
    val generatedTestSpineDir = "$generatedDir/test/spine"

    sourceSets {
        main {
            java.srcDirs(
                generatedSpineDir,
                generatedJavaDir,
            )
        }
        test {
            java.srcDirs(
                generatedTestSpineDir,
                generatedTestJavaDir,
            )
        }
    }

    tasks {
        val generateRejections by existing
        compileKotlin {
            dependsOn(generateRejections)
        }

        val generateTestRejections by existing
        compileTestKotlin {
            dependsOn(generateTestRejections)
        }

        registerTestTasks()
        test {
            useJUnitPlatform { includeEngines("junit-jupiter") }
            configureLogging()
        }
    }

    apply<IncrementGuard>()
    apply<VersionWriter>()

    LicenseReporter.generateReportIn(project)
    JavadocConfig.applyTo(project)
    CheckStyleConfig.applyTo(project)

    idea {
        module {
            generatedSourceDirs.addAll(
                files(
                    generatedJavaDir,
                    generatedGrpcDir,
                    generatedSpineDir,
                    generatedTestJavaDir,
                    generatedTestGrpcDir,
                    generatedTestSpineDir
                )
            )
            testSources.from(generatedTestJavaDir)

            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    /**
     * Determines whether this project should expose its Javadoc to `SpineEventEngine.github.io`
     * website.
     *
     * Currently, the `testutil` projects are excluded from publishing, as well as the modules
     * that perform the model compile-time checks.
     *
     * @return `true` is the project Javadoc should be published, `false` otherwise
     */
    fun shouldPublishJavadoc() =
        !project.name.startsWith("testutil") &&
        !project.name.startsWith("model")

    updateGitHubPages(project.version.toString()) {
        allowInternalJavadoc.set(true)
        rootFolder.set(rootDir)
    }

    tasks.named("publish") {
        dependsOn("${project.path}:updateGitHubPages")
    }

    project.afterEvaluate {
        tasks.findByName("launchProtoDataMain")?.apply {
            val launchProtoDataMain = this
            arrayOf("compileKotlin").forEach {
                tasks.findByName(it)?.dependsOn(launchProtoDataMain)
            }
        }

        tasks.findByName("launchProtoDataTest")?.apply {
            val lanunceProtoDataTest = this
            tasks.findByName("compileTestKotlin")?.dependsOn(lanunceProtoDataTest)
        }
    }
}

JacocoConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)
