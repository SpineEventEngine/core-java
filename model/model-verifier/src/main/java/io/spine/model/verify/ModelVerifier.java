/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.logging.Logging;
import io.spine.model.CommandHandlers;
import io.spine.server.command.Assign;
import io.spine.server.command.model.CommandHandlerSignature;
import io.spine.server.command.model.DuplicateHandlerCheck;
import io.spine.server.model.Model;
import io.spine.server.model.declare.MethodSignature;
import io.spine.tools.gradle.ProjectHierarchy;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.deepToString;
import static java.util.stream.Collectors.toList;

/**
 * A utility for verifying Spine model.
 *
 * @implNote The full name of this class is used by {@link Model#dropAllModels()} via a
 *         string literal for security check.
 */
final class ModelVerifier implements Logging {

    private static final URL[] EMPTY_URL_ARRAY = new URL[0];

    private final URLClassLoader projectClassLoader;

    /**
     * Creates a new instance of the {@code ModelVerifier}.
     *
     * @param project
     *         the Gradle project to verify the model upon
     */
    ModelVerifier(Project project) {
        this.projectClassLoader = createClassLoader(project);
    }

    /**
     * Verifies Spine model upon the given Gradle project.
     *
     * @param handlers the listing of the Spine model classes
     */
    void verify(CommandHandlers handlers) {
        ClassSet classSet = new ClassSet(projectClassLoader,
                                         handlers.getCommandHandlingTypesList());
        classSet.reportNotFoundIfAny(log());
        handlersMatchContract(classSet);
        DuplicateHandlerCheck.newInstance()
                             .check(classSet.elements());
    }

    private static void handlersMatchContract(ClassSet handlers) {
        MethodSignature signature = new CommandHandlerSignature();
        handlers.elements()
                .stream()
                .map(Class::getDeclaredMethods)
                .flatMap(Arrays::stream)
                .filter(method -> method.isAnnotationPresent(Assign.class))
                .forEach(signature::matches);
    }

    /**
     * Creates a ClassLoader for the passed project.
     */
    private URLClassLoader createClassLoader(Project project) {
        Collection<JavaCompile> tasks = allJavaCompile(project);
        URL[] compiledCodePath = extractDestinationDirs(tasks);
        log().debug("Initializing ClassLoader for URLs: {}", deepToString(compiledCodePath));
        try {
            ClassLoader projectClassloader = project.getBuildscript()
                                                    .getClassLoader();
            @SuppressWarnings("ClassLoaderInstantiation") // Caught exception.
            URLClassLoader result = new URLClassLoader(compiledCodePath, projectClassloader);
            return result;
        } catch (SecurityException e) {
            String msg = format("Cannot create ClassLoader for the project %s", project);
            throw new IllegalStateException(msg, e);
        }
    }

    private static Collection<JavaCompile> allJavaCompile(Project project) {
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
