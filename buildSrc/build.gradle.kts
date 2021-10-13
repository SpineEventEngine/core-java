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

/**
 * This script uses two declarations of the constant [licenseReportVersion] because
 * currently there is no way to define a constant _before_ a build script of `buildSrc`.
 * We cannot use imports or do something else before the `buildscript` or `plugin` clauses.
 *
 * Therefore, when a version of [io.spine.internal.dependency.LicenseReport] changes, it should be
 * changed in the Kotlin object _and_ in this file below twice.
 */

plugins {
    java
    groovy
    `kotlin-dsl`
    pmd
    val licenseReportVersion = "1.16"
    id("com.github.jk1.dependency-license-report").version(licenseReportVersion)
}

repositories {
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()
}

val jacksonVersion = "2.11.0"
val googleAuthToolVersion = "2.1.2"
val licenseReportVersion = "1.16"
val grGitVersion = "3.1.1"

/**
 * The version of Guava used in `buildSrc`.
 *
 * Always use the same version as the one specified in `io.spine.internal.dependency.Guava`.
 * Otherwise, when testing Gradle plugins, clashes may occur.
 */
val guavaVersion = "30.1.1-jre"

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.google.cloud.artifactregistry:artifactregistry-auth-common:$googleAuthToolVersion") {
        exclude(group = "com.google.guava")
    }
    implementation("com.google.guava:guava:$guavaVersion")
    api("com.github.jk1:gradle-license-report:$licenseReportVersion")
    implementation("org.ajoberstar.grgit:grgit-core:${grGitVersion}")
}
