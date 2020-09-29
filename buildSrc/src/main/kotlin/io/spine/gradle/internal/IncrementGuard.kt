/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.gradle.internal

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
     * The task will never run outside of Travis CI or when building individual branches. This is
     * done to prevent unexpected CI fails when re-building `master` multiple times, creating git
     * tags, and in other cases that go outside of the "usual" development cycle.
     */
    override fun apply(target: Project) {
        val tasks = target.tasks
        tasks.register(taskName, CheckVersionIncrement::class.java) {
            repository = PublishingRepos.cloudRepo
            tasks.getByName("check").dependsOn(this)

            shouldRunAfter("test")
            if (!isTravisPullRequest()) {
                logger.info("The build does not represent a Travis pull request job, the " +
                        "`checkVersionIncrement` task is disabled.")
                this.enabled = false
            }
        }
    }

    /**
     * Returns `true` if the current build is a Travis job which represents a GitHub pull request.
     *
     * Implementation note: the `TRAVIS_PULL_REQUEST` environment variable contains the pull
     * request number rather than `"true"` in positive case, hence the check.
     *
     * @see <a href="https://docs.travis-ci.com/user/environment-variables/#default-environment-variables">
     *     List of default environment variables provided for Travis builds</a>
     */
    private fun isTravisPullRequest(): Boolean {
        val isPullRequest = System.getenv("TRAVIS_PULL_REQUEST")
        return isPullRequest != null && isPullRequest != "false"
    }
}
