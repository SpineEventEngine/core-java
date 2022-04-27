/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.internal.dependency.Dokka
import io.spine.internal.gradle.dokka.onlyJavaSources
import io.spine.internal.gradle.dokka.onlyNonGeneratedSources

import java.time.LocalDate
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.dokka")
}

dependencies {
    /**
     * To generate the documentation as seen from Java perspective, the kotlin-as-java plugin was
     * added to the Dokka's classpath.
     *
     * @see <a href="https://github.com/Kotlin/dokka#output-formats">
     *     Dokka output formats</a>
     */
    dokkaPlugin(Dokka.KotlinAsJavaPlugin.lib)

    /**
     * To exclude pieces of code annotated with `@Internal` from the documentation a custom plugin
     * is added to the Dokka's classpath.
     *
     * @see <a href="https://github.com/SpineEventEngine/dokka-tools/tree/master/dokka-extensions">
     *     Custom Dokka Plugins</a>
     */
    dokkaPlugin(Dokka.SpineExtensions.lib)
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        sourceRoots.setFrom(
            onlyJavaSources()
        )

        sourceRoots.setFrom(
            onlyNonGeneratedSources()
        )

        skipEmptyPackages.set(true)
    }

    outputDirectory.set(buildDir.resolve("docs/dokka"))

    val dokkaConfDir = rootDir.resolve("buildSrc/src/main/resources/dokka")

    /**
     * Dokka Base plugin allows to set a few properties to customize the output:
     *
     * - `customStyleSheets` property to which we can pass our css files overriding styles generated
     * by Dokka;
     * - `customAssets` property to provide resources. We need to provide an image with the name
     * "logo-icon.svg" to overwrite the default one used by Dokka;
     * - `separateInheritedMembers` when set to `true`, creates a separate tab in type-documentation
     * for inherited members.
     *
     * @see <a href="https://kotlin.github.io/dokka/1.6.10/user_guide/base-specific/frontend/#prerequisites">
     * Dokka modifying frontend assets</a>
     */
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        customStyleSheets = listOf(file("${dokkaConfDir.resolve("styles/custom-styles.css")}"))
        customAssets = listOf(file("${dokkaConfDir.resolve("assets/logo-icon.svg")}"))
        separateInheritedMembers = true
        footerMessage = "Copyright ${LocalDate.now().year}, TeamDev"
    }
}
