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

// https://github.com/FasterXML/jackson/wiki/Jackson-Releases
@Suppress("unused", "ConstPropertyName")
object Jackson {
    const val version = "2.15.3"
    private const val databindVersion = "2.15.3"

    private const val coreGroup = "com.fasterxml.jackson.core"
    private const val dataformatGroup = "com.fasterxml.jackson.dataformat"
    private const val moduleGroup = "com.fasterxml.jackson.module"

    // https://github.com/FasterXML/jackson-core
    const val core = "$coreGroup:jackson-core:${version}"
    // https://github.com/FasterXML/jackson-databind
    const val databind = "$coreGroup:jackson-databind:${databindVersion}"
    // https://github.com/FasterXML/jackson-annotations
    const val annotations = "$coreGroup:jackson-annotations:${version}"

    // https://github.com/FasterXML/jackson-dataformat-xml/releases
    const val dataformatXml = "$dataformatGroup:jackson-dataformat-xml:${version}"
    // https://github.com/FasterXML/jackson-dataformats-text/releases
    const val dataformatYaml = "$dataformatGroup:jackson-dataformat-yaml:${version}"

    // https://github.com/FasterXML/jackson-module-kotlin/releases
    const val moduleKotlin = "$moduleGroup:jackson-module-kotlin:${version}"

    // https://github.com/FasterXML/jackson-bom
    const val bom = "com.fasterxml.jackson:jackson-bom:${version}"
}
