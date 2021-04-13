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

import io.spine.internal.dependency.AnimalSniffer
import io.spine.internal.dependency.AppEngine
import io.spine.internal.dependency.AutoCommon
import io.spine.internal.dependency.AutoService
import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.FindBugs
import io.spine.internal.dependency.Firebase
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.HttpClient
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Roaster

@Suppress("unused")
object Build {
    const val animalSniffer = AnimalSniffer.lib
    const val autoCommon = AutoCommon.lib
    val autoService = AutoService
    const val appEngine = AppEngine.sdk
    val checker = CheckerFramework
    val errorProne = ErrorProne
    const val firebaseAdmin = Firebase.admin
    val flogger = Flogger
    val guava = Guava
    const val googleHttpClient = HttpClient.google
    const val googleHttpClientApache = HttpClient.apache
    val gradlePlugins = GradlePlugins
    const val jacksonDatabind = Jackson.databind
    const val jsr305Annotations = FindBugs.annotations
    val kotlin = Kotlin
    val protobuf = Protobuf
    val roaster = Roaster

    val ci = "true".equals(System.getenv("CI"))

    @Deprecated("Use Flogger over SLF4J.", replaceWith = ReplaceWith("flogger"))
    @Suppress("DEPRECATION")
    val slf4j = io.spine.internal.dependency.Slf4J.lib
}
