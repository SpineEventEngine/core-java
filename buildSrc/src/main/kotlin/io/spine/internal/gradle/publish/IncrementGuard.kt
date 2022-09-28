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

@file:Suppress("unused")

package io.spine.internal.gradle.publish

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin which adds a [CheckVersionIncrement] task.
 *
 * The task is called `checkVersionIncrement` inserted before the `check` task.
 */
class IncrementGuard : Plugin<Project> {

    companion object {
        const val taskName = "checkVersionIncrement"
    }

    /**
     * Adds the [CheckVersionIncrement] task to the project.
     *
     * Only adds the check if the project is built on Travis CI and the job is a pull request.
     *
     * The task only runs on non-master branches on GitHub Actions. This is done
     * to prevent unexpected CI fails when re-building `master` multiple times, creating git
     * tags, and in other cases that go outside of the "usual" development cycle.
     */
    override fun apply(target: Project) {
        val tasks = target.tasks
        tasks.register(taskName, CheckVersionIncrement::class.java) {
            repository = CloudRepo.published
            tasks.getByName("check").dependsOn(this)

            shouldRunAfter("test")
            if (!shouldCheckVersion()) {
                logger.info(
                    "The build does not represent a GitHub Actions feature branch job, " +
                            "the `checkVersionIncrement` task is disabled."
                )
                this.enabled = false
            }
        }
    }

    /**
     * Returns `true` if the current build is a GitHub Actions build which represents a push
     * to a feature branch.
     *
     * Returns `false` if the associated reference is not a branch (e.g. a tag) or if it has
     * the name which ends with `master`. So, on branches such as `master` and `2.x-jdk8-master`
     * this method would return `false`.
     *
     * @see <a href="https://docs.github.com/en/free-pro-team@latest/actions/reference/environment-variables">
     *     List of default environment variables provided for GitHub Actions builds</a>
     */
    private fun shouldCheckVersion(): Boolean {
        val eventName = System.getenv("GITHUB_EVENT_NAME")
        if ("push" != eventName) {
            return false
        }
        val reference = System.getenv("GITHUB_REF") ?: return false
        val matches = Regex("refs/heads/(.+)").matchEntire(reference) ?: return false
        val branch = matches.groupValues[1]
        return !branch.endsWith("master")
    }
}
