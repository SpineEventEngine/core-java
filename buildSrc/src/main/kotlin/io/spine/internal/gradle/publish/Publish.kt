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

package io.spine.internal.gradle.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.publish.PublishingExtension

/**
 * This plugin allows publishing artifacts to remote Maven repositories.
 *
 * The plugin can be used with single- and multi-module projects.
 *
 * When applied to a single-module project, the reference to the project is passed to the plugin:
 * ```
 * import io.spine.gradle.internal.PublishingRepos
 * import io.spine.gradle.internal.spinePublishing
 *
 * spinePublishing {
 *     publish(project)
 *     targetRepositories.addAll(
 *         PublishingRepos.cloudRepo,
 *         PublishingRepos.gitHub("LibraryName")
 *     )
 * }
 * ```
 * When applied to a multi-module project, the plugin should be applied to the root project.
 * The sub-projects to be published are specified by their names:
 * ```
 * import io.spine.gradle.internal.PublishingRepos
 * import io.spine.gradle.internal.spinePublishing
 *
 * spinePublishing {
 *     projectsToPublish.addAll(
 *         "submodule1",
 *         "submodule2",
 *         "nested:submodule3"
 *     )
 *     targetRepositories.addAll(
 *         PublishingRepos.cloudRepo,
 *         PublishingRepos.gitHub("LibraryName")
 *     )
 * }
 * ```
 *
 * By default, we publish artifacts produced by tasks `sourceJar`, `testOutputJar`,
 * and `javadocJar`, along with the default project compilation output. If any of these tasks is not
 * declared, it's created with sensible default settings by the plugin.
 *
 * To publish more artifacts for a certain project, add them to the archives configuration:
 * ```
 * artifacts {
 *     archives(myCustomJarTask)
 * }
 * ```
 *
 * If any plugins applied to the published project declare any other artifacts, those artifacts
 * are published as well.
 */
class Publish : Plugin<Project> {

    companion object {
        const val taskName = "publish"
        const val extensionName = "spinePublishing"
    }

    override fun apply(project: Project) {
        val extension = PublishExtension.create(project)
        project.extensions.add(PublishExtension::class.java, extensionName, extension)

        project.afterEvaluate {
            val soloMode = extension.singleProject()
            val rootPublish: Task? =
                if (soloMode) null
                else project.createPublishTask()
            val checkCredentials: Task = project.createCheckTask(extension)

            if (soloMode) {
                project.applyMavenPublish(extension, null, checkCredentials)
            } else {
                extension.projectsToPublish
                    .get()
                    .map { project.project(it) }
                    .forEach { it.applyMavenPublish(extension, rootPublish, checkCredentials) }
            }
        }
    }
}

