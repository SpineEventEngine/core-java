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

package io.spine.internal.gradle

import com.google.common.base.Joiner
import io.spine.internal.dependency.Flogger
import java.io.File
import java.io.InputStream
import java.io.StringWriter
import java.util.*

object Runtime {
    @Suppress("unused")
    val flogger = Flogger.Runtime
}

/**
 * Executor of CLI commands.
 *
 * Uses the passed [workingFolder] as the directory in which the commands are executed.
 */
class Cli(private val workingFolder: File) {

    /**
     * Executes the given terminal command and retrieves the command output.
     *
     * <p>{@link Runtime#exec(String[], String[], File) Executes} the given {@code String} array as
     * a CLI command. If the execution is successful, returns the command output. Throws
     * an {@link IllegalStateException} otherwise.
     *
     * @param command the command to execute
     * @return the command line output
     * @throws IllegalStateException upon an execution error
     */
    fun execute(vararg command: String): String {
        val outWriter = StringWriter()
        val errWriter = StringWriter()

        val process = ProcessBuilder(*command)
            .directory(workingFolder)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.inputStream!!.pourTo(outWriter)
        process.errorStream!!.pourTo(errWriter)
        val exitCode = process.waitFor()

        if (exitCode == 0) {
            return outWriter.toString()
        } else {
            val cmdAsString = Joiner.on(" ").join(command.iterator())
            val errorMsg = "Command `$cmdAsString` finished with exit code $exitCode:" +
                    " ${System.lineSeparator()}$errWriter" +
                    " ${System.lineSeparator()}$outWriter."
            throw IllegalStateException(errorMsg)
        }
    }
}

/**
 * Asynchronously reads all lines from this [InputStream] and appends them
 * to the passed [StringWriter].
 */
fun InputStream.pourTo(dest: StringWriter) {
    Thread {
        val sc = Scanner(this)
        while (sc.hasNextLine()) {
            dest.append(sc.nextLine())
        }
    }.start()
}
