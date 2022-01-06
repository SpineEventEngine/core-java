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

package io.spine.internal.gradle.javadoc

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

/**
 * Javadoc processing settings.
 *
 * This type is named with `Config` suffix to avoid its confusion with the standard `Javadoc` type.
 */
@Suppress("unused")
object JavadocConfig {

    @Suppress("MemberVisibilityCanBePrivate") // opened to be visible from docs.
    val tags = listOf(
        JavadocTag("apiNote", "API Note"),
        JavadocTag("implSpec", "Implementation Requirements"),
        JavadocTag("implNote", "Implementation Note")
    )

    val encoding = Encoding("UTF-8")

    fun applyTo(project: Project) {
        val docletOptions = project.tasks.javadocTask().options as StandardJavadocDocletOptions
        docletOptions.encoding = encoding.name
        reduceParamWarnings(docletOptions)
        registerCustomTags(docletOptions)
    }

    /**
     * Configures the [Javadoc] task for the passed [docletOptions] to avoid numerous warnings
     * for missing `@param` tags.
     *
     * As suggested by Stephen Colebourne:
     *  [https://blog.joda.org/2014/02/turning-off-doclint-in-jdk-8-javadoc.html]
     *
     * See also:
     *  [https://github.com/GPars/GPars/blob/master/build.gradle#L268]
     */
    private fun reduceParamWarnings(docletOptions: StandardJavadocDocletOptions) {
        if (JavaVersion.current().isJava8Compatible) {
            docletOptions.addStringOption("Xdoclint:none", "-quiet")
        }
    }

    /**
     * Registers custom [tags] for the passed doclet options which in turn belong
     * to some particular [Javadoc] task.
     */
    fun registerCustomTags(docletOptions: StandardJavadocDocletOptions) {
        docletOptions.tags = tags.map { it.toString() }
    }
}
