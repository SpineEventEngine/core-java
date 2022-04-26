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

package io.spine.internal.gradle.publish

import io.spine.internal.gradle.sourceSets
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Excludes Google `.proto` sources from all artifacts.
 *
 * Goes through all registered `Jar` tasks and filters out Google's files.
 */
@Suppress("unused")
fun TaskContainer.excludeGoogleProtoFromArtifacts() {
    withType<Jar>().configureEach {
        exclude { it.isGoogleProtoSource() }
    }
}

/**
 * Checks if the given file belongs to the Google `.proto` sources.
 */
private fun FileTreeElement.isGoogleProtoSource(): Boolean {
    val pathSegments = relativePath.segments
    return pathSegments.isNotEmpty() && pathSegments[0].equals("google")
}

/**
 * Locates or creates `sourcesJar` task in this [Project].
 *
 * The output of this task is a `jar` archive. The archive contains sources from `main` source set.
 * The task makes sure that sources from the directories below will be included into
 * a resulted archive:
 *
 *  - Kotlin
 *  - Java
 *  - Proto
 *
 * Java and Kotlin sources are default to `main` source set since it is created by `java` plugin.
 * For Proto sources to be included – [special treatment][protoSources] is needed.
 */
internal fun Project.sourcesJar() = tasks.getOrCreate("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource) // Puts Java and Kotlin sources.
    from(protoSources()) // Puts Proto sources.
}

/**
 * Locates or creates `protoJar` task in this [Project].
 *
 * The output of this task is a `jar` archive. The archive contains only
 * [Proto sources][protoSources] from `main` source set.
 */
internal fun Project.protoJar() = tasks.getOrCreate("protoJar") {
    archiveClassifier.set("proto")
    from(protoSources())
}

/**
 * Locates or creates `testJar` task in this [Project].
 *
 * The output of this task is a `jar` archive. The archive contains compilation output
 * of `test` source set.
 */
internal fun Project.testJar() = tasks.getOrCreate("testJar") {
    archiveClassifier.set("test")
    from(sourceSets["test"].output)
}

/**
 * Locates or creates `javadocJar` task in this [Project].
 *
 * The output of this task is a `jar` archive. The archive contains Javadoc,
 * generated upon Java sources from `main` source set. If javadoc for Kotlin is also needed,
 * apply Dokka plugin. It tunes `javadoc` task to generate docs upon Kotlin sources as well.
 */
internal fun Project.javadocJar() = tasks.getOrCreate("javadocJar") {
    archiveClassifier.set("javadoc")
    from(files("$buildDir/docs/javadoc"))
    dependsOn("javadoc")
}

/**
 * Locates or creates `dokkaJar` task in this [Project].
 *
 * The output of this task is a `jar` archive. The archive contains the Dokka output, generated upon
 * Java sources from `main` source set. Requires Dokka to be configured in the target project by
 * applying `dokka-for-java` plugin.
 */
internal fun Project.dokkaJar() = tasks.getOrCreate("dokkaJar") {
    archiveClassifier.set("dokka")
    from(files("$buildDir/docs/dokka"))
    dependsOn("dokkaHtml")
}

private fun TaskContainer.getOrCreate(name: String, init: Jar.() -> Unit): TaskProvider<Jar> =
    if (names.contains(name)) {
        named<Jar>(name)
    } else {
        register<Jar>(name) {
            init()
        }
    }
