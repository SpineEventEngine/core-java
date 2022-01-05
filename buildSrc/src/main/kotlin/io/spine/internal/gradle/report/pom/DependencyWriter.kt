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

package io.spine.internal.gradle.report.pom

import groovy.xml.MarkupBuilder
import java.io.Writer
import java.util.*
import kotlin.reflect.full.isSubclassOf
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency
import org.gradle.kotlin.dsl.withGroovyBuilder

/**
 * Writes the dependencies of a Gradle project in a `pom.xml` format.
 *
 * Includes the dependencies of the subprojects. Does not include the transitive dependencies.
 *
 * ```
 *  <dependencies>
 *      <dependency>
 *          <groupId>io.spine</groupId>
 *          <artifactId>base</artifactId>
 *          <version>2.0.0-pre1</version>
 *      </dependency>
 *      ...
 *  </dependencies>
 * ```
 *
 * @see PomGenerator
 */
internal class DependencyWriter
private constructor(
    private val dependencies: SortedSet<ScopedDependency>
) {
    internal companion object {

        /**
         * Creates the `ProjectDependenciesAsXml` for the passed [project].
         */
        fun of(project: Project): DependencyWriter {
            return DependencyWriter(project.dependencies())
        }
    }

    /**
     * Writes the dependencies in their `pom.xml` format to the passed [out] writer.
     *
     * <p>Used writer will not be closed.
     */
    fun writeXmlTo(out: Writer) {
        val xml = MarkupBuilder(out)
        xml.withGroovyBuilder {
            "dependencies" {
                dependencies.forEach { scopedDep ->
                    val dependency = scopedDep.dependency()
                    "dependency" {
                        "groupId" { xml.text(dependency.group) }
                        "artifactId" { xml.text(dependency.name) }
                        "version" { xml.text(dependency.version) }
                        if (scopedDep.hasDefinedScope()) {
                            "scope" { xml.text(scopedDep.scopeName()) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns the [scoped dependencies][ScopedDependency] of a Gradle project.
 */
fun Project.dependencies(): SortedSet<ScopedDependency> {
    val dependencies = mutableSetOf<ScopedDependency>()
    dependencies.addAll(this.depsFromAllConfigurations())

    this.subprojects.forEach { subproject ->
        val subprojectDeps = subproject.depsFromAllConfigurations()
        dependencies.addAll(subprojectDeps)
    }
    return dependencies.toSortedSet()
}

/**
 * Returns the scoped dependencies of the project from all the project configurations.
 */
private fun Project.depsFromAllConfigurations(): Set<ScopedDependency> {
    val result = mutableSetOf<ScopedDependency>()
    this.configurations.forEach { configuration ->
        if (configuration.isCanBeResolved) {
            // Force resolution of the configuration.
            configuration.resolvedConfiguration
        }
        configuration.dependencies.forEach {
            if (it.isExternal()) {
                val dependency = ScopedDependency.of(it, configuration)
                result.add(dependency)
            }
        }
    }
    return result
}

/**
 * Tells whether the dependency is an external module dependency.
 */
private fun Dependency.isExternal(): Boolean {
    return this.javaClass.kotlin.isSubclassOf(AbstractExternalModuleDependency::class)
}
