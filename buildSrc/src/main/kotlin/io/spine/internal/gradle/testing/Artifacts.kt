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

package io.spine.internal.gradle.testing

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Exposes the test classes of this project as a new `testArtifacts` configuration.
 *
 * This allows other Gradle projects to depend on the test classes of some project. It is helpful
 * in case the dependant projects re-use abstract test suites of some "parent" project.
 *
 * Please note that this utility requires Gradle `java` plugin to be applied.
 * Hence, it is recommended to call this extension method from `java` scope:
 *
 * ```
 * java {
 *     exposeTestArtifacts()
 * }
 * ```
 *
 * Here is a snippet which consumes the exposed test classes:
 *
 * ```
 * dependencies {
 *     testImplementation(project(path = ":projectName", configuration = "testArtifacts"))
 * }
 * ```
 */
fun Project.exposeTestArtifacts() {

    val testArtifacts = configurations.create("testArtifacts") {
        extendsFrom(configurations.getAt("testRuntimeClasspath"))
    }

    val sourceSets = extensions.getByType<JavaPluginExtension>().sourceSets
    val testJar = tasks.register<Jar>("testJar") {
        archiveClassifier.set("test")
        from(sourceSets.getAt("test").output)
    }

    artifacts.add(testArtifacts.name, testJar)
}
