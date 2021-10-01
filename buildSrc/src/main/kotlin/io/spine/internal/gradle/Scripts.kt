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

import org.gradle.api.Project

@Suppress("unused")
object Scripts {
    @Suppress("MemberVisibilityCanBePrivate") // is used from Groovy-based scripts.
    const val commonPath = "/buildSrc/src/main/groovy/"

    fun testArtifacts(p: Project)          = p.script("test-artifacts.gradle")
    fun testOutput(p: Project)             = p.script("test-output.gradle")
    fun slowTests(p: Project)              = p.script("slow-tests.gradle")
    fun jacoco(p: Project)                 = p.script("jacoco.gradle")
    fun publish(p: Project)                = p.script("publish.gradle")
    fun publishProto(p: Project)           = p.script("publish-proto.gradle")
    fun javacArgs(p: Project)              = p.script("javac-args.gradle")
    fun jsBuildTasks(p: Project)           = p.script("js/build-tasks.gradle")
    fun jsConfigureProto(p: Project)       = p.script("js/configure-proto.gradle")
    fun npmPublishTasks(p: Project)        = p.script("js/npm-publish-tasks.gradle")
    fun npmCli(p: Project)                 = p.script("js/npm-cli.gradle")
    fun updatePackageVersion(p: Project)   = p.script("js/update-package-version.gradle")
    fun dartBuildTasks(p: Project)         = p.script("dart/build-tasks.gradle")
    fun pubPublishTasks(p: Project)        = p.script("dart/pub-publish-tasks.gradle")

    @Deprecated("Use `pmd-settings` script plugin instead")
    fun pmd(p: Project)                    = p.script("pmd.gradle")

    fun checkstyle(p: Project)             = p.script("checkstyle.gradle")
    fun runBuild(p: Project)               = p.script("run-build.gradle")
    fun modelCompiler(p: Project)          = p.script("model-compiler.gradle")
    fun licenseReportCommon(p: Project)    = p.script("license-report-common.gradle")
    fun projectLicenseReport(p: Project)   = p.script("license-report-project.gradle")
    fun repoLicenseReport(p: Project)      = p.script("license-report-repo.gradle")
    fun generatePom(p: Project)            = p.script("generate-pom.gradle")

    private fun Project.script(name: String) = "${rootDir}$commonPath${name}"
}
