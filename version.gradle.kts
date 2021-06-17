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

/**
 * The versions of the libraries used.
 *
 * This file is used in both module `build.gradle.kts` scripts and in the integration tests,
 * as we want to manage the versions in a single source.
 *
 * This version file adheres to the contract of the
 * [publishing application](https://github.com/SpineEventEngine/publishing).
 *
 * When changing the version declarations or adding new ones, make sure to change
 * the publishing application accordingly.
 */

/**
 * Version of this library.
 */
val coreJava = "2.0.0-SNAPSHOT.26"

/**
 * Versions of the Spine libraries that `core-java` depends on.
 */
val base = "2.0.0-SNAPSHOT.34"
val time = "2.0.0-SNAPSHOT.34"

project.extra.apply {
    this["versionToPublish"] = coreJava
    this["spineBaseVersion"] = base
    this["spineTimeVersion"] = time
    this["kotlinVersion"] = io.spine.internal.dependency.Kotlin.version
}
