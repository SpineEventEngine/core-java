/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import io.spine.internal.dependency.Kotest
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.JUnit
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-module")
    kotlin("jvm")
    id("io.kotest")
    id("org.jetbrains.kotlinx.kover")
    id("detekt-code-analysis")
    id("dokka-for-kotlin")
}

kotlin {
    applyJvmToolchain(BuildSettings.javaVersion.asInt())
    explicitApi()
}

dependencies {
    testImplementation(Spine.testlib)
    testImplementation(Kotest.frameworkEngine)
    testImplementation(Kotest.datatest)
    testImplementation(Kotest.runnerJUnit5Jvm)
    testImplementation(JUnit.runner)
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = BuildSettings.javaVersion.toString()
        setFreeCompilerArgs()
    }
}

kover {
    useJacocoTool()
}

koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}
