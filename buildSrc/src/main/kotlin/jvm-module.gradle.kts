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

import BuildSettings.javaVersion
import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.Dokka
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.JavaX
import io.spine.internal.dependency.Kotest
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.github.pages.updateGitHubPages
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.idea
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.`java-library`
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    idea
    id("net.ltgt.errorprone")
    id("pmd-settings")
    id("project-report")
    id("dokka-for-java")
    kotlin("jvm")
    id("io.kotest")
    id("org.jetbrains.kotlinx.kover")
    id("detekt-code-analysis")
    id("dokka-for-kotlin")
}

LicenseReporter.generateReportIn(project)
JavadocConfig.applyTo(project)
CheckStyleConfig.applyTo(project)

project.run {
    configureJava(javaVersion)
    configureKotlin(javaVersion)
    addDependencies()
    forceConfigurations()

    val generatedDir = "$projectDir/generated"
    setTaskDependencies(generatedDir)
    setupTests()

    configureGitHubPages()
}

typealias Module = Project

fun Module.configureJava(javaVersion: JavaLanguageVersion) {
    java {
        toolchain.languageVersion.set(javaVersion)
    }

    tasks {
        withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
        }
    }
}

fun Module.configureKotlin(javaVersion: JavaLanguageVersion) {
    kotlin {
        applyJvmToolchain(javaVersion.asInt())
        explicitApi()
    }

    tasks {
        withType<KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = javaVersion.toString()
            setFreeCompilerArgs()
        }
    }

    kover {
        useJacoco()
    }

    koverReport {
        defaults {
            xml {
                onCheck = true
            }
        }
    }
}

/**
 * These dependencies are applied to all subprojects and do not have to
 * be included explicitly.
 *
 * We expose production code dependencies as API because they are used
 * by the framework parts that depend on `base`.
 */
fun Module.addDependencies() = dependencies {
    errorprone(ErrorProne.core)

    Protobuf.libs.forEach { api(it) }
    api(Guava.lib)

    compileOnlyApi(CheckerFramework.annotations)
    compileOnlyApi(JavaX.annotations)
    ErrorProne.annotations.forEach { compileOnlyApi(it) }

    implementation(Spine.Logging.lib)

    testImplementation(Guava.testLib)
    testImplementation(JUnit.runner)
    testImplementation(JUnit.pioneer)
    JUnit.api.forEach { testImplementation(it) }

    testImplementation(Spine.testlib)
    testImplementation(Kotest.frameworkEngine)
    testImplementation(Kotest.datatest)
    testImplementation(Kotest.runnerJUnit5Jvm)
    testImplementation(JUnit.runner)
}

fun Module.forceConfigurations() {
    with(configurations) {
        forceVersions()
        excludeProtobufLite()
        all {
            resolutionStrategy {
                force(
                    JUnit.bom,
                    JUnit.runner,
                    Dokka.BasePlugin.lib
                )
            }
        }
    }
}

fun Module.setupTests() {
    tasks {
        registerTestTasks()
        test.configure {
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            configureLogging()
        }
    }
}

fun Module.setTaskDependencies(generatedDir: String) {
    tasks {
        val cleanGenerated by registering(Delete::class) {
            delete(generatedDir)
        }
        clean.configure {
            dependsOn(cleanGenerated)
        }

        project.afterEvaluate {
            val publish = tasks.findByName("publish")
            publish?.dependsOn("${project.path}:updateGitHubPages")
        }
    }
    configureTaskDependencies()
}

fun Module.configureGitHubPages() {
    val docletVersion = project.version.toString()
    updateGitHubPages(docletVersion) {
        allowInternalJavadoc.set(true)
        rootFolder.set(rootDir)
    }
}
