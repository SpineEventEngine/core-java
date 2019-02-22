/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.model.verify;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.concurrent.LazyInit;
import io.spine.logging.Logging;
import io.spine.tools.gradle.ProjectHierarchy;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.deepToString;
import static java.util.stream.Collectors.toList;

/**
 * The class loader of the Gradle project.
 */
final class ProjectClassLoader implements Logging {

    private static final URL[] EMPTY_URL_ARRAY = new URL[0];

    /**
     * The enclosed project instance.
     */
    private final Project project;

    /**
     * The class loader lazily created for the project.
     */
    @LazyInit
    private @MonotonicNonNull ClassLoader classLoader;

    ProjectClassLoader(Project project) {
        this.project = project;
    }

    ClassLoader get() {
        if (classLoader == null) {
            classLoader = createClassLoader();
        }
        return classLoader;
    }

    /**
     * Creates a class loader for the Gradle project searching through the output dirs of its
     * {@link JavaCompile} tasks.
     *
     * @throws IllegalStateException
     *         if the class loader cannot be initialized due to security issues
     */
    @SuppressWarnings("ClassLoaderInstantiation") // Caught exception.
    private URLClassLoader createClassLoader() {
        Collection<JavaCompile> tasks = allJavaCompile();
        URL[] compiledCodePath = extractDestinationDirs(tasks);
        log().debug("Initializing ClassLoader for URLs: {}", deepToString(compiledCodePath));
        try {
            ClassLoader projectClassloader = project.getBuildscript()
                                                    .getClassLoader();
            URLClassLoader result = new URLClassLoader(compiledCodePath, projectClassloader);
            return result;
        } catch (SecurityException e) {
            String msg = format("Cannot create ClassLoader for the project %s", project);
            throw new IllegalStateException(msg, e);
        }
    }

    private Collection<JavaCompile> allJavaCompile() {
        Collection<JavaCompile> tasks = newArrayList();
        ProjectHierarchy.applyToAll(project.getRootProject(),
                                    p -> tasks.addAll(javaCompile(p)));
        return tasks;
    }

    private static Collection<JavaCompile> javaCompile(Project project) {
        return project.getTasks()
                      .withType(JavaCompile.class);
    }

    private static URL[] extractDestinationDirs(Collection<JavaCompile> tasks) {
        Collection<URL> urls = tasks.stream()
                                    .map(GetDestinationDir.FUNCTION)
                                    .collect(toList());
        URL[] result = urls.toArray(EMPTY_URL_ARRAY);
        return result;
    }

    /**
     * A function which retrieves the output directory from the passed Gradle task.
     */
    @VisibleForTesting
    enum GetDestinationDir implements Function<JavaCompile, URL> {
        FUNCTION;

        @Override
        public @Nullable URL apply(@Nullable JavaCompile task) {
            checkNotNull(task);
            File dir = task.getDestinationDir();
            if (dir == null) {
                return null;
            }
            URI uri = dir.toURI();
            try {
                URL url = uri.toURL();
                return url;
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(format(
                        "Could not retrieve destination directory for task `%s`.",
                        task.getName()), e);
            }
        }
    }
}
