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

package io.spine.internal.gradle.report.pom

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

/**
 * A module's dependency.
 *
 * Contains information about a module and configuration, from which
 * the dependency comes.
 */
internal class ModuleDependency(
    val module: Project,
    val configuration: Configuration,
    private val dependency: Dependency,

) : Dependency by dependency, Comparable<ModuleDependency> {

    companion object {
        private val COMPARATOR = compareBy<ModuleDependency> { it.module }
            .thenBy { it.configuration.name }
            .thenBy { it.group }
            .thenBy { it.name }
            .thenBy { it.version }
    }

    /**
     * A project dependency with its [scope][DependencyScope].
     *
     * Doesn't contain any info about an origin module and configuration.
     */
    val scoped = ScopedDependency.of(dependency, configuration)

    /**
     * GAV coordinates of this dependency.
     *
     * Gradle's [Dependency] is a mutable object. Its properties can change their
     * values with time. In parcticular, the version can be changed as more
     * configurations are getting resolved. This is why this property is calculated.
     */
    val gav: String
        get() = "$group:$name:$version"

    override fun compareTo(other: ModuleDependency): Int = COMPARATOR.compare(this, other)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModuleDependency

        if (module != other.module) return false
        if (configuration != other.configuration) return false
        if (dependency != other.dependency) return false

        return true
    }

    override fun hashCode(): Int {
        var result = module.hashCode()
        result = 31 * result + configuration.hashCode()
        result = 31 * result + dependency.hashCode()
        return result
    }
}
