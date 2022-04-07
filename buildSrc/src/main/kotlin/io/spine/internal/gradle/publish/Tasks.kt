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
import java.util.*
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

private const val PUBLISH = "publish"

/**
 * Locates `publish` task in this [TaskContainer].
 *
 * This task publishes all defined publications to all defined repositories. To achieve that,
 * the task depends on all `publish`*PubName*`PublicationTo`*RepoName*`Repository` tasks.
 *
 * Please note, task execution would not copy publications to the local Maven cache.
 *
 * @see <a href="https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks">
 *     Tasks | Maven Publish Plugin</a>
 */
internal val TaskContainer.publish: TaskProvider<Task>
    get() = named(PUBLISH)

/**
 * Sets dependencies for `publish` task in this [Project].
 *
 * This method performs the following:
 *
 *  1. When this [Project] is not a root, makes `publish` task in a root project
 *     depend on a local `publish`.
 *  2. Makes local `publish` task verify that credentials are present for each
 *     of destination repositories.
 */
internal fun Project.configurePublishTask(destinations: Set<Repository>) {
    attachCredentialsVerification(destinations)
    bindToRootPublish()
}

private fun Project.attachCredentialsVerification(destinations: Set<Repository>) {
    val checkCredentials = tasks.registerCheckCredentialsTask(destinations)
    val localPublish = tasks.publish
    localPublish.configure { dependsOn(checkCredentials) }
}

private fun Project.bindToRootPublish() {
    if (project == rootProject) {
        return
    }

    val localPublish = tasks.publish
    val rootPublish = rootProject.tasks.getOrCreatePublishTask()
    rootPublish.configure { dependsOn(localPublish) }
}

/**
 * Use this task accessor when it is not guaranteed that the task is present
 * in this [TaskContainer].
 */
private fun TaskContainer.getOrCreatePublishTask() =
    if (names.contains(PUBLISH)) {
        named(PUBLISH)
    } else {
        register(PUBLISH)
    }

private fun TaskContainer.registerCheckCredentialsTask(destinations: Set<Repository>) =
    register("checkCredentials") {
        doLast {
            destinations.forEach { it.ensureCredentials(project) }
        }
    }

private fun Repository.ensureCredentials(project: Project) {
    val credentials = credentials(project)
    if (Objects.isNull(credentials)) {
        throw InvalidUserDataException(
            "No valid credentials for repository `${this}`. Please make sure " +
                    "to pass username/password or a valid `.properties` file."
        )
    }
}
