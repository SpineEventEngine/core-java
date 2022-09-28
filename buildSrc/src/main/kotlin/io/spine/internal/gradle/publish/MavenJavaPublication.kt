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

import io.spine.internal.gradle.Repository
import io.spine.internal.gradle.isSnapshot
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType

/**
 * A publication for a typical Java project.
 *
 * In Gradle, in order to publish something somewhere one should create a publication.
 * A publication has a name and consists of one or more artifacts plus information about
 * those artifacts – the metadata.
 *
 * An instance of this class represents [MavenPublication] named "mavenJava". It is generally
 * accepted that a publication with this name contains a Java project published to one or
 * more Maven repositories.
 *
 * By default, only a jar with the compilation output of `main` source set and its
 * metadata files are published. Other artifacts are specified through the
 * [constructor parameter][jars]. Please, take a look on [specifyArtifacts] for additional info.
 *
 * See: [Maven Publish Plugin | Publications](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:publications)
 *
 *  @param artifactId a name that a project is known by.
 *  @param jars list of artifacts to be published along with the compilation output.
 *  @param destinations Maven repositories to which the produced artifacts will be sent.
 */
internal class MavenJavaPublication(
    private val artifactId: String,
    private val jars: Set<TaskProvider<Jar>>,
    private val destinations: Set<Repository>,
) {

    /**
     * Registers this publication in the given project.
     *
     * The only prerequisite for the project is to have `maven-publish` plugin applied.
     */
    fun registerIn(project: Project) {
        createPublication(project)
        registerDestinations(project)
    }

    /**
     * Creates a new "mavenJava" [MavenPublication] in the given project.
     */
    private fun createPublication(project: Project) {
        val gradlePublishing = project.extensions.getByType<PublishingExtension>()
        val gradlePublications = gradlePublishing.publications
        gradlePublications.create<MavenPublication>("mavenJava") {
            specifyMavenCoordinates(project)
            specifyArtifacts(project)
        }
    }

    private fun MavenPublication.specifyMavenCoordinates(project: Project) {
        groupId = project.group.toString()
        artifactId = this@MavenJavaPublication.artifactId
        version = project.version.toString()
    }

    /**
     * Specifies which artifacts this [MavenPublication] will contain.
     *
     * A typical Maven publication contains:
     *
     *  1. Jar archives. For example: compilation output, sources, javadoc, etc.
     *  2. Maven metadata file that has ".pom" extension.
     *  3. Gradle metadata file that has ".module" extension.
     *
     *  Metadata files contain information about a publication itself, its artifacts and their
     *  dependencies. Presence of ".pom" file is mandatory for publication to be consumed by
     *  `mvn` build tool itself or other build tools that understand Maven notation (Gradle, Ivy).
     *  Presence of ".module" is optional, but useful when a publication is consumed by Gradle.
     *
     *  See: [Maven – POM Reference](https://maven.apache.org/pom.html)
     *       [Understanding Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html)
     */
    private fun MavenPublication.specifyArtifacts(project: Project) {

        // "java" component provides a jar with compilation output of "main" source set.
        // It is NOT defined as another `Jar` task intentionally. Doing that will leave the
        // publication without correct ".pom" and ".module" metadata files generated.
        from(project.components["java"])

        // Other artifacts are represented by `Jar` tasks. Those artifacts don't bring any other
        // metadata in comparison with `Component` (such as dependencies notation).
        jars.forEach {
            artifact(it)
        }
    }

    /**
     * Goes through the [destinations] and registers each as a repository for publishing
     * in the given Gradle project.
     */
    private fun registerDestinations(project: Project) {
        val gradlePublishing = project.extensions.getByType<PublishingExtension>()
        val isSnapshot = project.version.toString().isSnapshot()
        val gradleRepositories = gradlePublishing.repositories
        destinations.forEach { destination ->
            gradleRepositories.register(destination, isSnapshot, project)
        }
    }

    private fun RepositoryHandler.register(
        repository: Repository,
        isSnapshot: Boolean,
        project: Project
    ) {
        val target = if (isSnapshot) repository.snapshots else repository.releases
        val credentials = repository.credentials(project.rootProject)
        maven {
            url = project.uri(target)
            credentials {
                username = credentials?.username
                password = credentials?.password
            }
        }
    }
}
