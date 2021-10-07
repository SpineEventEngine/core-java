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

package io.spine.internal.gradle.report.pom

import java.io.StringWriter
import java.lang.System.lineSeparator

/**
 * Helps to format the `pom.xml` file according to its expected XML structure.
 */
internal object PomFormatting {

    private val NL = lineSeparator()
    private const val XML_METADATA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    private const val PROJECT_SCHEMA_LOCATION = "<project " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 " +
            "http://maven.apache.org/xsd/maven-4.0.0.xsd\" " +
            "xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
    private const val MODEL_VERSION = "<modelVersion>4.0.0</modelVersion>"
    private const val CLOSING_PROJECT_TAG = "</project>"

    /**
     * Writes the starting segment of `pom.xml`.
     */
    internal fun writeStart(dest: StringWriter) {
        dest.write(XML_METADATA)
        dest.write(NL)
        dest.write(PROJECT_SCHEMA_LOCATION)
        dest.write(NL)
        dest.write(MODEL_VERSION)
        dest.write(NL)
        dest.write(NL)
        dest.write(describingComment())
        dest.write(NL)
        dest.write(NL)
    }

    /**
     * Obtains a description comment that describes the nature of the generated `pom.xml` file.
     */
    private fun describingComment(): String {
        val description =
            lineSeparator() +
                    "This file was generated using the Gradle `generatePom` task. " +
                    lineSeparator() +
                    "This file is not suitable for `maven` build tasks. It only describes the " +
                    "first-level dependencies of " +
                    lineSeparator() +
                    "all modules and does not describe the project " +
                    "structure per-subproject." +
                    lineSeparator()
        return String.format(
            "<!-- %s %s %s -->",
            lineSeparator(),
            description,
            lineSeparator()
        )
    }

    /**
     * Writes the closing segment of `pom.xml`.
     */
    internal fun writeEnd(dest: StringWriter) {
        dest.write(CLOSING_PROJECT_TAG)
    }

    /**
     * Writes the specified lines using the specified [destination], dividing them
     * by platform-specific line separator.
     *
     * The written lines are also padded with platform's line separator from both sides
     */
    internal fun writeBlocks(destination: StringWriter, vararg lines: String) {
        lines.iterator().forEach {
            destination.write(it)
            destination.write(NL)
            destination.write(NL)
        }
    }
}
