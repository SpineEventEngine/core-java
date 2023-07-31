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

package io.spine.internal.dependency

// https://junit.org/junit5/
@Suppress("unused", "ConstPropertyName")
object JUnit {
    const val version = "5.10.0"
    private const val legacyVersion = "4.13.1"

    // https://github.com/apiguardian-team/apiguardian
    private const val apiGuardianVersion = "1.1.2"

    // https://github.com/junit-pioneer/junit-pioneer
    private const val pioneerVersion = "2.0.1"

    const val legacy = "junit:junit:${legacyVersion}"

    val api = listOf(
        "org.apiguardian:apiguardian-api:${apiGuardianVersion}",
        "org.junit.jupiter:junit-jupiter-api:${version}",
        "org.junit.jupiter:junit-jupiter-params:${version}"
    )
    const val bom = "org.junit:junit-bom:${version}"

    const val runner = "org.junit.jupiter:junit-jupiter-engine:${version}"
    const val params = "org.junit.jupiter:junit-jupiter-params:${version}"

    const val pioneer = "org.junit-pioneer:junit-pioneer:${pioneerVersion}"

    object Platform {
        // https://junit.org/junit5/
        const val version = "1.10.0"
        internal const val group = "org.junit.platform"
        const val commons = "$group:junit-platform-commons:$version"
        const val launcher = "$group:junit-platform-launcher:$version"
        const val engine = "$group:junit-platform-engine:$version"
        const val suiteApi = "$group:junit-platform-suite-api:$version"
    }
}
