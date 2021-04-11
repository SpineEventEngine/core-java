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

package io.spine.gradle.internal.license

import com.github.jk1.license.*
import com.github.jk1.license.render.ReportRenderer
import org.gradle.api.Plugin
import org.gradle.api.Project


// https://github.com/jk1/Gradle-License-Report
object LicenseReportDependency {
    private const val version = "1.16"
    const val gradlePlugin = "com.github.jk1:gradle-license-report:${version}"
}

object LicenseReportConfig {
    // The output filename
    const val outputFilename = "license-report.md"

    // The path to a directory, to which a per-project report is generated.
    const val relativePath = "/reports/dependency-license/dependency"
}

class RepositoryLicenseReport: Plugin<Project> {
    override fun apply(target: Project) {
        TODO("Not yet implemented")
    }
}

class MarkdownRenderer: com.github.jk1.license.render.ReportRenderer {

}
