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
import org.gradle.api.tasks.TaskContainer

/**
 * Sets dependencies for `publish` task in this [Project].
 *
 * This method performs the following:
 *
 *  1. Makes `publish` task in a root project depend on local `publish`.
 *  2. Makes local `publish` task verify that credentials are present for each
 *     of destination repositories.
 */
internal fun Project.configurePublishTask(destinations: Set<Repository>) {
    val rootPublish = rootProject.tasks.getOrCreatePublishTask()
    val localPublish = tasks.getOrCreatePublishTask()
    val checkCredentials = tasks.registerCheckCredentialsTask(destinations)
    rootPublish.configure { dependsOn(localPublish) }
    localPublish.configure { dependsOn(checkCredentials) }
}

private const val PUBLISH = "publish"

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
