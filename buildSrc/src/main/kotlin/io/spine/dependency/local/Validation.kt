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

package io.spine.dependency.local

/**
 * Dependencies on Spine Validation SDK.
 *
 * See [`SpineEventEngine/validation`](https://github.com/SpineEventEngine/validation/).
 */
@Suppress("ConstPropertyName", "unused")
object Validation {
    /**
     * The version of the Validation library artifacts.
     */
    const val version = "2.0.0-SNAPSHOT.183"

    const val group = "io.spine.validation"
    private const val prefix = "spine-validation"

    const val runtimeModule = "$group:$prefix-java-runtime"
    const val runtime = "$runtimeModule:$version"
    const val java = "$group:$prefix-java:$version"

    const val javaBundleModule = "$group:$prefix-java-bundle"

    /** Obtains the artifact for the `java-bundle` artifact of the given version. */
    fun javaBundle(version: String) = "$javaBundleModule:$version"

    val javaBundle = javaBundle(version)

    const val model = "$group:$prefix-model:$version"
    const val config = "$group:$prefix-configuration:$version"
}
