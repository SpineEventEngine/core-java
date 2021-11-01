/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.JUnit
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.Scripts
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.github.pages.updateGitHubPages
import io.spine.internal.gradle.javacompile.configureErrorProne
import io.spine.internal.gradle.javacompile.configureJavaCompile
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.pom.PomGenerator
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
buildscript {
    apply(from = "$rootDir/version.gradle.kts")

    io.spine.internal.gradle.doApplyStandard(repositories)
    io.spine.internal.gradle.doApplyGitHubPackages(repositories, "base", rootProject)

    val kotlinVersion: String by extra
    val spineBaseVersion: String by extra
    val spineTimeVersion: String by extra

    dependencies {
        classpath("io.spine.tools:spine-mc-java:$spineBaseVersion")
    }

    io.spine.internal.gradle.doForceVersions(configurations)
    configurations.all {
        resolutionStrategy {
            force(
                    "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
                    "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion",
                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine:spine-time:$spineTimeVersion"
            )
        }
    }
}

repositories.applyStandard()

apply(from = "$rootDir/version.gradle.kts")

@Suppress("RemoveRedundantQualifierName") // Cannot use imports here.
plugins {
    `java-library`
    kotlin("jvm") version io.spine.internal.dependency.Kotlin.version
    idea
    io.spine.internal.dependency.Protobuf.GradlePlugin.apply {
        id(id) version version
    }
    io.spine.internal.dependency.ErrorProne.GradlePlugin.apply {
        id(id)
    }
}

/** The name of the GitHub repository to which this project belongs. */
val repositoryName: String = "core-java"

val kotlinVersion: String by extra
val spineBaseVersion: String by extra
val spineTimeVersion: String by extra

spinePublishing {
    with(PublishingRepos) {
        targetRepositories.addAll(setOf(
            cloudRepo,
            gitHub(repositoryName),
            cloudArtifactRegistry
        ))
    }

    projectsToPublish.addAll(
        "core",
        "client",
        "server",
        "testutil-core",
        "testutil-client",
        "testutil-server",
        "model-assembler",
        "model-verifier"
    )
}

allprojects {
    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")
    }

    // Apply “legacy” dependency definitions which are not yet migrated to Kotlin.
    // The `ext.deps` project property is used by `.gradle` scripts under `config/gradle`.
    apply {
        from("$rootDir/version.gradle.kts")
    }

    group = "io.spine"
    version = extra["versionToPublish"]!!
}

subprojects {

    with(repositories) {
        applyGitHubPackages("base", rootProject)
        applyGitHubPackages("time", rootProject)
        applyStandard()
    }

    apply {
        plugin("java-library")
        plugin("com.google.protobuf")
        plugin("net.ltgt.errorprone")
        plugin("io.spine.mc-java")
        plugin("kotlin")
        plugin("pmd")
        plugin("maven-publish")
        plugin("pmd-settings")

        with(Scripts) {
            from(projectLicenseReport(project))
        }

        CheckStyleConfig.applyTo(project)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        tasks {
            configureErrorProne()
            configureJavaCompile()
        }
    }

    kotlin {
        explicitApi()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            freeCompilerArgs = listOf("-Xskip-prerelease-check")
        }
    }

    dependencies {
        ErrorProne.apply {
            errorprone(core)
            errorproneJavac(javacPlugin)
        }

        api("io.spine:spine-base:$spineBaseVersion")
        api("io.spine:spine-time:$spineTimeVersion")

        testImplementation(JUnit.runner)
        testImplementation("io.spine.tools:spine-testlib:$spineBaseVersion")
    }

    configurations.forceVersions()
    configurations {
        all {
            resolutionStrategy {
                force(
                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine:spine-time:$spineTimeVersion",
                    "io.spine.tools:spine-testlib:$spineBaseVersion"
                )
            }
        }
    }
    configurations.excludeProtobufLite()

    val srcDir = "$projectDir/src"
    val generatedDir = "$projectDir/generated"
    val generatedJavaDir = "$generatedDir/main/java"
    val generatedTestJavaDir = "$generatedDir/test/java"
    val generatedGrpcDir = "$generatedDir/main/grpc"
    val generatedTestGrpcDir = "$generatedDir/test/grpc"
    val generatedSpineDir = "$generatedDir/main/spine"
    val generatedTestSpineDir = "$generatedDir/test/spine"

    sourceSets {
        main {
            java.srcDirs(generatedJavaDir, "$srcDir/main/java", generatedSpineDir)
            resources.srcDirs("$srcDir/main/resources", "$generatedDir/main/resources")
            proto.srcDirs("$srcDir/main/proto")
        }
        test {
            java.srcDirs(generatedTestJavaDir, "$srcDir/test/java", generatedTestSpineDir)
            resources.srcDirs("$srcDir/test/resources", "$generatedDir/test/resources")
            proto.srcDirs("$srcDir/test/proto")
        }
    }

    val generateRejections by tasks.getting
    tasks.compileKotlin {
        dependsOn(generateRejections)
    }

    val generateTestRejections by tasks.getting
    tasks.compileTestKotlin {
        dependsOn(generateTestRejections)
    }

    tasks.test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
    }

    val isTravis = System.getenv("TRAVIS") == "true"
    if (isTravis) {
        tasks.javadoc {
            val opt = options
            if (opt is CoreJavadocOptions) {
                opt.addStringOption("Xmaxwarns", "1")
            }
        }
    }

    apply {
        with(Scripts) {
            from(slowTests(project))
            from(testOutput(project))
        }
    }

    JavadocConfig.applyTo(project)

    tasks.register("sourceJar", Jar::class) {
        from(sourceSets.main.get().allJava)
        archiveClassifier.set("sources")
    }

    tasks.register("testOutputJar", Jar::class) {
        from(sourceSets.test.get().output)
        archiveClassifier.set("test")
    }

    tasks.register("javadocJar", Jar::class) {
        from("$projectDir/build/docs/javadoc")
        archiveClassifier.set("javadoc")

        dependsOn(tasks.javadoc)
    }

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

            testSourceDirs.add(file(generatedTestJavaDir))

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
    project.tasks["publish"].dependsOn("${project.path}:updateGitHubPages")
}

apply {
    with(Scripts) {
        // Aggregated coverage report across all subprojects.
        from(jacoco(project))

        // Generate a repository-wide report of 3rd-party dependencies and their licenses.
        from(repoLicenseReport(project))


    }
}

// Generate a `pom.xml` file containing first-level dependency of all projects
// in the repository.
PomGenerator.applyTo(project)
