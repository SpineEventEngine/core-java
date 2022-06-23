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

package io.spine.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import java.io.File

/**
 * A Gradle task which runs another Gradle build.
 *
 * Launches Gradle wrapper under a given [directory] with the `build` task. The `clean` task is also
 * run if current build includes a `clean` task.
 *
 * The build writes verbose log into `$directory/build/debug-out.txt`. The error output is written
 * into `$directory/build/error-out.txt`.
 */
open class RunBuild : DefaultTask() {

    /**
     * Path to the directory which contains a Gradle wrapper script.
     */
    @Internal
    lateinit var directory: String

    @TaskAction
    private fun execute() {
        val runsOnWindows = OperatingSystem.current().isWindows()
        val script = if (runsOnWindows) "gradlew.bat" else "gradlew"
        val command = buildCommand(script)

        // Ensure build error output log.
        // Since we're executing this task in another process, we redirect error output to
        // the file under the `build` directory.
        val buildDir = File(directory, "build")
        if (!buildDir.exists()) {
            buildDir.mkdir()
        }
        val errorOut = File(buildDir, "error-out.txt")
        val debugOut = File(buildDir, "debug-out.txt")

        val process = buildProcess(command, errorOut, debugOut)
        if (process.waitFor() != 0) {
            throw GradleException("Build FAILED. See $errorOut for details.")
        }
    }

    private fun buildCommand(script: String): List<String> {
        val command = mutableListOf<String>()
        command.add("${project.rootDir}/$script")
        val shouldClean = project.gradle
            .taskGraph
            .hasTask(":clean")
        if (shouldClean) {
            command.add("clean")
        }
        command.add("build")
        command.add("--console=plain")
        command.add("--debug")
        command.add("--stacktrace")
        return command
    }

    private fun buildProcess(command: List<String>, errorOut: File, debugOut: File) =
        ProcessBuilder()
            .command(command)
            .directory(project.file(directory))
            .redirectError(errorOut)
            .redirectOutput(debugOut)
            .start()
}
