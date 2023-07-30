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

package io.spine.internal.gradle.publish

import io.spine.internal.gradle.Repository
import io.spine.internal.gradle.isSnapshot
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create

/**
 * The name of the Maven Publishing Gradle plugin.
 */
private const val MAVEN_PUBLISH = "maven-publish"

/**
 * Abstract base for handlers of publications in a project
 * with [spinePublishing] settings declared.
 */
internal sealed class PublicationHandler(
    protected val project: Project,
    private val destinations: Set<Repository>
) {

    fun apply() = with(project) {
        if (!hasCustomPublishing) {
            apply(plugin = MAVEN_PUBLISH)
        }

        pluginManager.withPlugin(MAVEN_PUBLISH) {
            handlePublications()
            registerDestinations()
            configurePublishTask(destinations)
        }
    }

    /**
     * Either handles publications already declared in the given project,
     * or creates new ones.
     */
    abstract fun handlePublications()

    /**
     * Goes through the [destinations] and registers each as a repository for publishing
     * in the given Gradle project.
     */
    private fun registerDestinations() {
        val repositories = project.publishingExtension.repositories
        destinations.forEach { destination ->
            repositories.register(project, destination)
        }
    }

    /**
     * Takes a group name and a version from the given [project] and assigns
     * them to this publication.
     */
    protected fun MavenPublication.assignMavenCoordinates() {
        groupId = project.group.toString()
        artifactId = project.spinePublishing.artifactPrefix + artifactId
        version = project.version.toString()
    }
}

/**
 * Adds a Maven repository to the project specifying credentials, if they are
 * [available][Repository.credentials] from the root project.
 */
private fun RepositoryHandler.register(project: Project, repository: Repository) {
    val isSnapshot = project.version.toString().isSnapshot()
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
 * [constructor parameter][jarFlags]. Please, take a look on [specifyArtifacts] for additional info.
 *
 * @param jarFlags
 *         flags for additional JARs published along with the compilation output.
 * @param destinations
 *         Maven repositories to which the produced artifacts will be sent.
 * @see <a href="https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:publications">
 *       Maven Publish Plugin | Publications</a>
 */
internal class StandardJavaPublicationHandler(
    project: Project,
    private val jarFlags: JarFlags,
    destinations: Set<Repository>,
) : PublicationHandler(project, destinations) {

    /**
     * Creates a new "mavenJava" [MavenPublication] in the given project.
     */
    override fun handlePublications() {
        val jars = project.artifacts(jarFlags)
        val publications = project.publications
        publications.create<MavenPublication>("mavenJava") {
            assignMavenCoordinates()
            specifyArtifacts(jars)
        }
    }

    /**
     * Specifies which artifacts this [MavenPublication] will contain.
     *
     * A typical Maven publication contains:
     *
     *  1. Jar archives. For example: compilation output, sources, javadoc, etc.
     *  2. Maven metadata file that has ".pom" extension.
     *  3. Gradle's metadata file that has ".module" extension.
     *
     *  Metadata files contain information about a publication itself, its artifacts and their
     *  dependencies. Presence of ".pom" file is mandatory for publication to be consumed by
     *  `mvn` build tool itself or other build tools that understand Maven notation (Gradle, Ivy).
     *  Presence of ".module" is optional, but useful when a publication is consumed by Gradle.
     *
     * @see <a href="https://maven.apache.org/pom.html">Maven – POM Reference</a>
     * @see <a href="https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html">
     *      Understanding Gradle Module Metadata</a>
     */
    private fun MavenPublication.specifyArtifacts(jars: Set<TaskProvider<Jar>>) {

        /* "java" component provides a jar with compilation output of "main" source set.
           It is NOT defined as another `Jar` task intentionally. Doing that will leave the
           publication without correct ".pom" and ".module" metadata files generated.
        */
        val javaComponent = project.components.findByName("java")
        javaComponent?.let {
            from(it)
        }

        /* Other artifacts are represented by `Jar` tasks. Those artifacts don't bring any other
           metadata in comparison with `Component` (such as dependencies notation).
         */
        jars.forEach {
            artifact(it)
        }
    }
}

/**
 * A handler for custom publications, which are declared under the [publications]
 * section of a module.
 *
 * Such publications should be treated differently than [StandardJavaPublicationHandler],
 * which is <em>created</em> for a module. Instead, since the publications are already declared,
 * this class only [assigns maven coordinates][assignMavenCoordinates].
 *
 * A module which declares custom publications must be specified in
 * the [SpinePublishing.modulesWithCustomPublishing] property.
 *
 * If a module with [publications] declared locally is not specified as one with custom publishing,
 * it may cause a name clash between an artifact produced by the [standard][MavenPublication]
 * publication, and custom ones. In order to have both standard and custom publications,
 * please specify custom artifact IDs or classifiers for each custom publication.
 */
internal class CustomPublicationHandler(project: Project, destinations: Set<Repository>) :
    PublicationHandler(project, destinations) {

    override fun handlePublications() {
        project.publications.forEach {
            (it as MavenPublication).assignMavenCoordinates()
        }
    }
}
