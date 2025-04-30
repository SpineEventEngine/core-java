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

package io.spine.dependency.lib

// https://github.com/FasterXML/jackson/wiki/Jackson-Releases
@Suppress("unused", "ConstPropertyName")
object Jackson {
    const val version = "2.18.3"

    private const val groupPrefix = "com.fasterxml.jackson"
    private const val coreGroup = "$groupPrefix.core"
    private const val moduleGroup = "$groupPrefix.module"

    // https://github.com/FasterXML/jackson-bom
    const val bom = "com.fasterxml.jackson:jackson-bom:$version"

    // Constants coming below without `$version` are covered by the BOM.

    // https://github.com/FasterXML/jackson-core
    const val core = "$coreGroup:jackson-core"

    // https://github.com/FasterXML/jackson-databind
    const val databind = "$coreGroup:jackson-databind"

    // https://github.com/FasterXML/jackson-annotations
    const val annotations = "$coreGroup:jackson-annotations"

    // https://github.com/FasterXML/jackson-module-kotlin/releases
    const val moduleKotlin = "$moduleGroup:jackson-module-kotlin"

    object DataFormat {
        private const val group = "$groupPrefix.dataformat"
        private const val infix = "jackson-dataformat"

        // https://github.com/FasterXML/jackson-dataformat-xml/releases
        const val xml = "$group:$infix-xml"

        // https://github.com/FasterXML/jackson-dataformats-text/releases
        const val yaml = "$group:$infix-yaml"

        const val xmlArtifact = "$xml:$version"
        const val yamlArtifact = "$yaml:$version"
    }

    object DataType {
        private const val group = "$groupPrefix.datatype"
        private const val infix = "jackson-datatype"

        // https://github.com/FasterXML/jackson-modules-java8
        const val jdk8 = "$group:$infix-jdk8"

        // https://github.com/FasterXML/jackson-modules-java8/tree/2.19/datetime
        const val dateTime = "$group:$infix-jsr310"

        // https://github.com/FasterXML/jackson-datatypes-collections/blob/2.19/guava
        const val guava = "$group:$infix-guava"

        // https://github.com/FasterXML/jackson-dataformats-binary/tree/2.19/protobuf
        const val protobuf = "$group:$infix-protobuf"

        // https://github.com/FasterXML/jackson-datatypes-misc/tree/2.19/javax-money
        const val javaXMoney = "$group:$infix-javax-money"

        // https://github.com/FasterXML/jackson-datatypes-misc/tree/2.19/moneta
        const val moneta = "$group:jackson-datatype-moneta"
    }

    // https://github.com/FasterXML/jackson-jr
    object Junior {
        const val version = Jackson.version
        const val group = "com.fasterxml.jackson.jr"
        const val objects = "$group:jackson-jr-objects"
    }
}
