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

import io.spine.internal.gradle.JavadocConfig.tags
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

/**
 * Javadoc processing settings.
 *
 * This type is named with `Config` suffix to avoid its confusion with the standard `Javadoc` type.
 */
@Suppress("unused")
object JavadocConfig {
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

/**
 * The Javadoc tag.
 */
class JavadocTag(val name: String, val title: String) {

    override fun toString(): String {
        return "${name}:a:${title}:"
    }
}

/**
 * The encoding to use in Javadoc processing.
 */
data class Encoding(val name: String)

object JavadocTask {
    const val name = "javadoc"
}

/**
 * A helper routine which configures the GitHub Pages updater to exclude `@Internal` types.
 */
object InternalJavadocFilter {

    /**
     * The name of the helper task which configures the Javadoc processing
     * to exclude `@Internal` types.
     */
    const val taskName = "noInternalJavadoc"

    /**
     * The name of the custom configuration in scope of which the exclusion of `@Internal` types
     * is performed.
     */
    private const val excludeDocletConfig = "excludeInternalDoclet"

    /**
     * Creates a custom Javadoc task for the [project] which excludes the the types
     * annotated as `@Internal`.
     *
     * The task is registered under [taskName].
     */
    fun registerTask(project: Project) {
        val excludeInternalDoclet = registerConfiguration(project)
        appendCustomJavadocTask(taskName, project, excludeInternalDoclet)
    }

    private fun registerConfiguration(project: Project): Configuration {
        val configurations = project.configurations
        val excludeInternalDoclet = configurations.create(excludeDocletConfig)
        val projectVersion = project.version.toString()
        project.dependencies.add(
            excludeInternalDoclet.name,
            "io.spine.tools:spine-javadoc-filter:$projectVersion"
        )
        return excludeInternalDoclet
    }

    private fun appendCustomJavadocTask(
        taskName: String,
        project: Project,
        excludeInternalDoclet: Configuration
    ) {
        val tasks = project.tasks
        val javadocTask = tasks.javadocTask(JavadocTask.name)
        tasks.register(taskName, Javadoc::class.java) {

            source = project.sourceSets.getByName("main").allJava.filter {
                !it.absolutePath.contains("generated")
            }.asFileTree

            classpath = javadocTask.classpath

            options {
                encoding = JavadocConfig.encoding.name

                // Doclet fully qualified name.
                doclet = "io.spine.tools.javadoc.ExcludeInternalDoclet"

                // Path to the JAR containing the doclet.
                docletpath = excludeInternalDoclet.files.toList()
            }

            val docletOptions = options as StandardJavadocDocletOptions
            JavadocConfig.registerCustomTags(docletOptions)
        }
    }
}

/**
 * Finds a [Javadoc] Gradle task by the passed name.
 */
fun TaskContainer.javadocTask(named: String) = this.getByName(named) as Javadoc

/**
 * Finds a default [Javadoc] Gradle task.
 */
fun TaskContainer.javadocTask() = this.getByName(JavadocTask.name) as Javadoc
