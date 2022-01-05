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
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType

/**
 * Configures the `spinePublishing` extension.
 *
 * As `Publish` is a class-plugin in `buildSrc`, we don't get strongly typed generated helper
 * methods for the `spinePublishing` configuration. Thus, we provide this helper function for use
 * in Kotlin build scripts.
 */
@Suppress("unused")
fun Project.spinePublishing(action: PublishExtension.() -> Unit) {
    apply<Publish>()

    val extension = extensions.getByType(PublishExtension::class)
    extension.action()
}

internal fun Project.applyMavenPublish(
    extension: PublishExtension,
    rootPublish: Task?,
    checkCredentials: Task
) {
    logger.debug("Applying `maven-publish` plugin to ${name}.")

    apply(plugin = "maven-publish")

    setUpDefaultArtifacts()

    val applyAction = {
        val publishingExtension = extensions.getByType(PublishingExtension::class)
        with(publishingExtension) {
            val project = this@applyMavenPublish
            createMavenPublication(project, extension)
            setUpRepositories(project, extension)
        }

        if (rootPublish != null) {
            prepareTasks(rootPublish, checkCredentials)
        } else {
            tasks.getByPath(Publish.taskName).dependsOn(checkCredentials)
        }
    }
    if (state.executed) {
        applyAction()
    } else {
        afterEvaluate { applyAction() }
    }
}

internal fun Project.createPublishTask(): Task =
    rootProject.tasks.create(Publish.taskName)

internal fun Project.createCheckTask(extension: PublishExtension): Task {
    val checkCredentials = tasks.create("checkCredentials")
    checkCredentials.doLast {
        extension.targetRepositories
            .get()
            .forEach {
                it.credentials(this@createCheckTask)
                    ?: throw InvalidUserDataException(
                        "No valid credentials for repository `${it}`. Please make sure " +
                                "to pass username/password or a valid `.properties` file."
                    )
            }
    }
    return checkCredentials
}

private fun Project.prepareTasks(publish: Task, checkCredentials: Task) {
    val publishTasks = getTasksByName(Publish.taskName, false)
    publish.dependsOn(publishTasks)
    publishTasks.forEach { it.dependsOn(checkCredentials) }
}

private fun Project.setUpDefaultArtifacts() {
    val sourceJar = tasks.createIfAbsent(
        artifactTask = ArtifactTaskName.sourceJar,
        from = sourceSets["main"].allSource,
        classifier = "sources"
    )
    val testOutputJar = tasks.createIfAbsent(
        artifactTask = ArtifactTaskName.testOutputJar,
        from = sourceSets["test"].output,
        classifier = "test"
    )
    val javadocJar = tasks.createIfAbsent(
        artifactTask = ArtifactTaskName.javadocJar,
        from = files("$buildDir/docs/javadoc"),
        classifier = "javadoc",
        dependencies = setOf("javadoc")
    )

    artifacts {
        val archives = ConfigurationName.archives
        add(archives, sourceJar)
        add(archives, testOutputJar)
        add(archives, javadocJar)
    }
}

private fun TaskContainer.createIfAbsent(
    artifactTask: ArtifactTaskName,
    from: FileCollection,
    classifier: String,
    dependencies: Set<Any> = setOf()
): Task {
    val existing = findByName(artifactTask.name)
    if (existing != null) {
        return existing
    }
    return create(artifactTask.name, Jar::class) {
        this.from(from)
        archiveClassifier.set(classifier)
        dependencies.forEach { dependsOn(it) }
    }
}
