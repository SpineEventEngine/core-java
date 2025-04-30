/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.dependency.test

// https://junit.org/junit5/
@Suppress("unused", "ConstPropertyName")
object JUnit {
    const val version = "5.12.2"

    /**
     * The BOM of JUnit.
     *
     * This one should be forced in a project via:
     *
     * ```kotlin
     * dependencies {
     *     testImplementation(enforcedPlatform(JUnit.bom))
     * }
     * ```
     * The version of JUnit is forced automatically by
     * the [BomsPlugin][io.spine.dependency.boms.BomsPlugin]
     * when it is applied to the project.
     */
    const val bom = "org.junit:junit-bom:$version"

    private const val legacyVersion = "4.13.1"

    // https://github.com/apiguardian-team/apiguardian
    private const val apiGuardianVersion = "1.1.2"

    // https://github.com/junit-pioneer/junit-pioneer
    private const val pioneerVersion = "2.3.0"
    const val pioneer = "org.junit-pioneer:junit-pioneer:$pioneerVersion"

    const val legacy = "junit:junit:$legacyVersion"

    @Deprecated("Use JUnit.Jupiter.api instead", ReplaceWith("JUnit.Jupiter.api"))
    val api = listOf(
        "org.apiguardian:apiguardian-api:$apiGuardianVersion",
        "org.junit.jupiter:junit-jupiter-api:$version",
        "org.junit.jupiter:junit-jupiter-params:$version"
    )

    @Deprecated("Use JUnit.Jupiter.engine instead", ReplaceWith("JUnit.Jupiter.engine"))
    const val runner = "org.junit.jupiter:junit-jupiter-engine:$version"

    @Deprecated("Use JUnit.Jupiter.params instead", ReplaceWith("JUnit.Jupiter.params"))
    const val params = "org.junit.jupiter:junit-jupiter-params:$version"

    object Jupiter {
        const val group = "org.junit.jupiter"
        private const val infix = "junit-jupiter"

        // We do not use versions because they are forced via BOM.
        const val api = "$group:$infix-api"
        const val params = "$group:$infix-params"
        const val engine = "$group:$infix-engine"

        const val apiArtifact = "$api:$version"
    }

    object Platform {
        // https://junit.org/junit5/
        internal const val group = "org.junit.platform"
        private const val infix = "junit-platform"
        const val commons = "$group:$infix-commons"
        const val launcher = "$group:$infix-launcher"
        const val engine = "$group:$infix-engine"
        const val suiteApi = "$group:$infix-suite-api"
    }
}
