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

/**
 * This script uses two declarations of the constant [licenseReportVersion] because
 * currently there is no way to define a constant _before_ a build script of `buildSrc`.
 * We cannot use imports or do something else before the `buildscript` or `plugin` clauses.
 */

plugins {
    java
    groovy
    `kotlin-dsl`
    val licenseReportVersion = "2.1"
    id("com.github.jk1.dependency-license-report").version(licenseReportVersion)
}

repositories {
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()
}

/**
 * The version of Jackson used by `buildSrc`.
 *
 * Please keep this value in sync. with `io.spine.internal.dependency.Jackson.version`.
 * It's not a requirement, but would be good in terms of consistency.
 */
val jacksonVersion = "2.13.0"

val googleAuthToolVersion = "2.1.2"
val licenseReportVersion = "2.1"
val grGitVersion = "3.1.1"

/**
 * The version of the Kotlin Gradle plugin.
 *
 * Please check that this value matches one defined in
 *  [io.spine.internal.dependency.Kotlin.version].
 */
val kotlinVersion = "1.6.21"

/**
 * The version of Guava used in `buildSrc`.
 *
 * Always use the same version as the one specified in [io.spine.internal.dependency.Guava].
 * Otherwise, when testing Gradle plugins, clashes may occur.
 */
val guavaVersion = "31.1-jre"

/**
 * The version of ErrorProne Gradle plugin.
 *
 * Please keep in sync. with [io.spine.internal.dependency.ErrorProne.GradlePlugin.version].
 *
 * @see <a href="https://github.com/tbroyer/gradle-errorprone-plugin/releases">
 *     Error Prone Gradle Plugin Releases</a>
 */
val errorProneVersion = "2.0.2"

/**
 * The version of Protobuf Gradle Plugin.
 *
 * Please keep in sync. with [io.spine.internal.dependency.Protobuf.GradlePlugin.version].
 *
 * @see <a href="https://github.com/google/protobuf-gradle-plugin/releases">
 *     Protobuf Gradle Plugins Releases</a>
 */
val protobufPluginVersion = "0.8.18"

/**
 * The version of Dokka Gradle Plugins.
 *
 * Please keep in sync with [io.spine.internal.dependency.Dokka.version].
 *
 * @see <a href="https://github.com/Kotlin/dokka/releases">
 *     Dokka Releases</a>
 */
val dokkaVersion = "1.6.21"

configurations.all {
    resolutionStrategy {
        force(
            // Force Kotlin lib versions avoiding using those bundled with Gradle.
            "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
        )
    }
}

val jvmVersion = JavaLanguageVersion.of(11)

java {
    toolchain.languageVersion.set(jvmVersion)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = jvmVersion.toString()
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.google.cloud.artifactregistry:artifactregistry-auth-common:$googleAuthToolVersion") {
        exclude(group = "com.google.guava")
    }
    implementation("com.google.guava:guava:$guavaVersion")
    api("com.github.jk1:gradle-license-report:$licenseReportVersion")
    implementation("org.ajoberstar.grgit:grgit-core:${grGitVersion}")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:${errorProneVersion}")

    // Add explicit dependency to avoid warning on different Kotlin runtime versions.
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    implementation("gradle.plugin.com.google.protobuf:protobuf-gradle-plugin:$protobufPluginVersion")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:${dokkaVersion}")
    implementation("org.jetbrains.dokka:dokka-base:${dokkaVersion}")

    // https://github.com/srikanth-lingala/zip4j
    implementation("net.lingala.zip4j:zip4j:2.10.0")
}
