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

package io.spine.internal.gradle.github.pages

import io.spine.internal.gradle.Cli
import io.spine.internal.gradle.InternalJavadocFilter
import io.spine.internal.gradle.JavadocTask
import io.spine.internal.gradle.fs.LazyTempPath
import io.spine.internal.gradle.javadocTask
import java.io.File
import java.lang.System.lineSeparator
import java.nio.file.Path
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.add

/**
 * Registers the `updateGitHubPages` task which performs the update of the GitHub Pages
 * with the Javadoc generated for a particular Gradle project. The generated documentation
 * is appended to the `spine.io` site via GitHub pages by pushing commits to the `gh-pages` branch.
 *
 * Please note that the update is only performed for the projects which are NOT snapshots.
 *
 * Users may supply [allowInternalJavadoc][UpdateGitHubPagesExtension.allowInternalJavadoc] option,
 * which if `true`, includes the documentation for types marked `@Internal`.
 * By default, this option is `false`.
 *
 * Usage:
 * ```
 *      updateGitHubPages {
 *
 *          // Include `@Internal`-annotated types.
 *          allowInternalJavadoc.set(true)
 *
 *          // Propagate the full path to the local folder of the repository root.
 *          rootFolder.set(rootDir.absolutePath)
 *      }
 * ```
 *
 * In order to work, the script needs a `deploy_key_rsa` private RSA key file in the repository
 * root. It is recommended to decrypt it in the repository and then decrypt it on CI upon
 * publication. Also, the script uses the `FORMAL_GIT_HUB_PAGES_AUTHOR` environment variable to
 * set the author email for the commits. The `gh-pages` branch itself should exist before the plugin
 * is run.
 *
 * NOTE: when changing the value of "FORMAL_GIT_HUB_PAGES_AUTHOR", one also must change
 * the SSH private (encrypted `deploy_key_rsa`) and the public ("GitHub Pages publisher (Travis CI)"
 * on GitHub) keys.
 *
 * Another requirement is an environment variable `REPO_SLUG`, which is set by the CI environment,
 * such as `Publish` GitHub Actions workflow. It points to the repository for which the update
 * is executed. E.g.:
 *
 * ```
 *      REPO_SLUG: SpineEventEngine/base
 * ```
 *
 * @see UpdateGitHubPagesExtension for the extension which is used to configure this plugin
 */
class UpdateGitHubPages : Plugin<Project> {

    /**
     * Root folder of the repository, to which this `Project` belongs.
     */
    private lateinit var rootFolder: File

    /**
     * The external inputs to include into the publishing.
     *
     * The inputs are evaluated according to [Copy.from] specification.
     */
    private lateinit var includedInputs: Set<Any>

    /**
     * Path to the temp folder used to gather the Javadoc output
     * before submitting it to the GitHub Pages update.
     */
    private val javadocOutputPath = LazyTempPath("javadoc")

    /**
     * Path to the temp folder used checkout the original GitHub Pages branch.
     */
    private val checkoutTempFolder = LazyTempPath("repoTemp")

    companion object {
        /**
         * The name of the task which updates the GitHub Pages.
         */
        const val taskName = "updateGitHubPages"

        /**
         * The name of the helper task to gather the generated Javadoc before updating GitHub Pages.
         */
        const val copyJavadoc = "copyJavadoc"

        /**
         * The name of the environment variable that contains the email to use for authoring
         * the commits to the GitHub Pages branch.
         */
        const val formalGitHubPagesAuthorVar = "FORMAL_GIT_HUB_PAGES_AUTHOR"

        /**
         * The name of the environment variable containing the repository slug, for which
         * the Gradle build is performed.
         */
        const val repoSlugVar = "REPO_SLUG"

        /**
         * The branch to use when pushing the updates to the documentation.
         */
        const val gitHubPagesBranch = "gh-pages"
    }

    /**
     * Applies the plugin to the specified [project].
     *
     * If the project version says it is a snapshot, the plugin is not applied.
     */
    override fun apply(project: Project) {
        val extension = UpdateGitHubPagesExtension.create(project)
        project.extensions.add(UpdateGitHubPagesExtension::class, "updateGitHubPages", extension)
        project.afterEvaluate {
            val projectVersion = project.version.toString()
            val isSnapshot = isSnapshot(projectVersion)
            if (isSnapshot) {
                registerNoOpTask()
            } else {
                registerTasks(extension, project)
            }
        }
    }

    /**
     * Registers `updateGitHubPages` task which performs no actual update, but prints the message
     * telling the update is skipped, since the project is in its `SNAPSHOT` version.
     */
    private fun Project.registerNoOpTask() {
        tasks.register(taskName) {
            doLast {
                val project = this@registerNoOpTask
                println(
                    "GitHub Pages update will be skipped since this project is a snapshot: " +
                            "`${project.name}-${project.version}`."
                )
            }
        }
    }

    private fun registerTasks(extension: UpdateGitHubPagesExtension, project: Project) {
        val includeInternal = extension.allowInternalJavadoc()
        rootFolder = extension.rootFolder()
        includedInputs = extension.includedInputs()
        val tasks = project.tasks
        if (!includeInternal) {
            InternalJavadocFilter.registerTask(project)
        }
        registerCopyJavadoc(includeInternal, copyJavadoc, tasks)
        val updatePagesTask = registerUpdateTask(project)
        updatePagesTask.configure {
            dependsOn(copyJavadoc)
        }
    }

    private fun registerUpdateTask(project: Project): TaskProvider<Task> {
        return project.tasks.register(taskName) {
            doLast {
                try {
                    updateGhPages(checkoutTempFolder, project, javadocOutputPath)
                } finally {
                    cleanup()
                }
            }
        }
    }

    private fun cleanup() {
        val folders = listOf(checkoutTempFolder, javadocOutputPath)
        folders.forEach {
            it.toFile().deleteRecursively()
        }
    }

    private fun Task.updateGhPages(
        checkoutTempFolder: Path,
        project: Project,
        javadocOutputPath: Path
    ) {
        val gitHubAccessKey = gitHubKey(rootFolder)
        val repoSlug = repoSlug()
        val gitHost = gitHost(repoSlug)
        val ghRepoFolder = File("$checkoutTempFolder/$gitHubPagesBranch")
        val docDirPostfix = "reference/$project.name"
        val mostRecentDocDir = File("$ghRepoFolder/$docDirPostfix")
        val versionedDocDir = File("$mostRecentDocDir/v/$project.version")
        val generatedDocs = project.files(javadocOutputPath)

        // Create SSH config file to allow pushing commits to the repository.
        registerSshKey(gitHubAccessKey)
        checkoutDocs(gitHost, ghRepoFolder)

        logger.debug("Replacing the most recent docs in `$mostRecentDocDir`.")
        copyDocs(project, generatedDocs, mostRecentDocDir)

        logger.debug("Storing the new version of docs in the directory `$versionedDocDir`.")
        copyDocs(project, generatedDocs, versionedDocDir)

        Cli(ghRepoFolder).execute("git", "add", docDirPostfix)
        configureCommitter(ghRepoFolder)
        commitAndPush(ghRepoFolder, project)
        logger.debug("The GitHub Pages contents were successfully updated.")
    }

    private fun registerCopyJavadoc(
        allowInternalJavadoc: Boolean,
        taskName: String,
        tasks: TaskContainer
    ) {
        val inputs = composeInputs(tasks, allowInternalJavadoc)
        tasks.register(taskName, Copy::class.java) {
            doLast {
                from(*inputs.toTypedArray())
                into(javadocOutputPath)
            }
        }
    }

    private fun composeInputs(
        tasks: TaskContainer,
        allowInternalJavadoc: Boolean
    ): MutableList<Any> {
        val inputs = mutableListOf<Any>()
        if (allowInternalJavadoc) {
            inputs.add(tasks.javadocTask(InternalJavadocFilter.taskName))
        } else {
            inputs.add(tasks.javadocTask(JavadocTask.name))
        }
        inputs.addAll(includedInputs)
        return inputs
    }

    private fun commitAndPush(repoBaseDir: File, project: Project) {
        val cli = Cli(repoBaseDir)
        cli.execute(
            "git",
            "commit",
            "--allow-empty",
            "--message=\"Update Javadoc for module ${project.name} as for version ${project.version}\""
        )
        cli.execute("git", "push")
    }

    private fun copyDocs(project: Project, source: FileCollection, destination: File) {
        destination.mkdir()
        project.copy {
            from(source)
            into(destination)
        }
    }

    /**
     * Configures Git to publish the changes under "UpdateGitHubPages Plugin" Git user name
     * and email stored in "FORMAL_GIT_HUB_PAGES_AUTHOR" env variable.
     */
    private fun configureCommitter(repoBaseDir: File) {
        val cli = Cli(repoBaseDir)
        cli.execute("git", "config", "user.name", "\"UpdateGitHubPages Plugin\"")
        val authorEmail = System.getenv(formalGitHubPagesAuthorVar)
        cli.execute("git", "config", "user.email", authorEmail!!)
    }

    private fun checkoutDocs(gitHost: String, repoBaseDir: File) {
        Cli(rootFolder).execute("git", "clone", gitHost, repoBaseDir.absolutePath)
        Cli(repoBaseDir).execute("git", "checkout", gitHubPagesBranch)
    }

    /**
     * Creates an SSH key with the credentials from [gitHubAccessKey]
     * and registers it by invoking the `register-ssh-key.sh` script.
     */
    private fun registerSshKey(gitHubAccessKey: File) {
        val sshConfigFile = File("${System.getProperty("user.home")}/.ssh/config")
        if (!sshConfigFile.exists()) {
            val parentDir = sshConfigFile.canonicalFile.parentFile
            parentDir.mkdirs()
            sshConfigFile.createNewFile()
        }
        sshConfigFile.appendText(
            lineSeparator() +
                    "Host github.com-publish" + lineSeparator() +
                    "User git" + lineSeparator() +
                    "IdentityFile ${gitHubAccessKey.absolutePath}" + lineSeparator()
        )

        Cli(rootFolder).execute(
            "${rootFolder.absolutePath}/config/scripts/register-ssh-key.sh",
            gitHubAccessKey.absolutePath
        )
    }

    /**
     * Reads `REPO_SLUG` environment variable and returns its value.
     *
     * In case it is not set, a [GradleException] is thrown.
     */
    private fun repoSlug(): String {
        val repoSlug = System.getenv(repoSlugVar)
        if (repoSlug == null || repoSlug.isEmpty()) {
            throw GradleException("`REPO_SLUG` environment variable is not set.")
        }
        return repoSlug
    }

    /**
     * Returns the GitHub URL to the project repository.
     *
     * <p>A CI instance comes with an RSA key. However, of course, the default key has no
     * privileges in Spine repositories. Thus, we add our own RSA key — `deploy_rsa_key`.
     * It must have write rights in the associated repository. Also, we don't want that key
     * to be used for anything else but GitHub Pages publishing.
     * Thus, we configure the SSH agent to use the `deploy_rsa_key`
     * only for specific references, namely in `github.com-publish`.
     */
    private fun gitHost(repoSlug: String): String {
        return "git@github.com-publish:${repoSlug}.git"
    }

    /**
     * Locates `deploy_key_rsa` in the passed [rootFolder] and returns it as a [File].
     *
     * If it is not found, a [GradleException] is thrown.
     */
    private fun gitHubKey(rootFolder: File): File {
        val gitHubAccessKey = File("${rootFolder.absolutePath}/deploy_key_rsa")

        if (!gitHubAccessKey.exists()) {
            throw GradleException(
                "File $gitHubAccessKey does not exist. It should be encrypted" +
                        " in the repository and decrypted on CI."
            )
        }
        return gitHubAccessKey
    }

    private fun isSnapshot(version: String): Boolean {
        return version.contains("snapshot", true)
    }
}
