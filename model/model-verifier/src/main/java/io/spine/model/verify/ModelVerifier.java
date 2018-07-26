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
import io.spine.model.CommandHandlers;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.CommandHandler;
import io.spine.server.model.Model;
import io.spine.server.procman.ProcessManager;
import io.spine.tools.gradle.ProjectHierarchy;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.lang.String.format;
import static java.util.Arrays.deepToString;

/**
 * A utility for verifying Spine model.
 *
 * @author Dmytro Dashenkov
 */
final class ModelVerifier {

    private static final URL[] EMPTY_URL_ARRAY = new URL[0];

    private final Model model = Model.getInstance();
    private final URLClassLoader projectClassLoader;

    /**
     * Creates a new instance of the {@code ModelVerifier}.
     *
     * @param project the Gradle project to verify the model upon
     */
    ModelVerifier(Project project) {
        this.projectClassLoader = createClassLoaderForProject(project);
    }

    /**
     * Verifies Spine model upon the given Gradle project.
     *
     * @param spineModel the listing of the Spine model classes
     */
    void verify(CommandHandlers spineModel) {
        Logger log = log();
        model.clear();
        for (String commandHandlingClass : spineModel.getCommandHandlingTypesList()) {
            Class<?> cls;
            try {
                log.debug("Trying to load class \'{}\'", commandHandlingClass);
                cls = getModelClass(commandHandlingClass);
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load class {}." +
                         " Consider using io.spine.tools.spine-model-verifier plugin" +
                         " only for the modules with the sufficient classpath.",
                         commandHandlingClass);
                continue;
            }
            verifyClass(cls);
        }
    }

    @SuppressWarnings({
            "unchecked" /* Checked by the `if` statements */,
            "CheckReturnValue" /* Returned values for asXxxClass() are ignored because we use
                                  these methods only for verification of the classes. */
    })
    private void verifyClass(Class<?> cls) {
        Logger log = log();
        if (Aggregate.class.isAssignableFrom(cls)) {
            Class<? extends Aggregate> aggregateClass = (Class<? extends Aggregate>) cls;
            model.asAggregateClass(aggregateClass);
            log.debug("\'{}\' classified as Aggregate type.", aggregateClass);
        } else if (ProcessManager.class.isAssignableFrom(cls)) {
            Class<? extends ProcessManager> procManClass = (Class<? extends ProcessManager>) cls;
            model.asProcessManagerClass(procManClass);
            log.debug("\'{}\' classified as ProcessManager type.", procManClass);
        } else if (CommandHandler.class.isAssignableFrom(cls)) {
            Class<? extends CommandHandler> commandHandler = (Class<? extends CommandHandler>) cls;
            model.asCommandHandlerClass(commandHandler);
            log.debug("\'{}\' classified as CommandHandler type.", commandHandler);
        } else {
            throw newIllegalArgumentException(
                    "Class %s is not a command handling type.", cls.getName()
            );
        }
    }

    private Class<?> getModelClass(String fqn) throws ClassNotFoundException {
        return Class.forName(fqn, false, projectClassLoader);
    }

    private static URLClassLoader createClassLoaderForProject(Project project) {
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
            throw new IllegalStateException("Cannot analyze project source code.", e);
        }
    }

    private static Collection<JavaCompile> allJavaCompile(Project project) {
        Collection<JavaCompile> tasks = newLinkedList();
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
                                    .collect(Collectors.toList());
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
            File destDir = task.getDestinationDir();
            if (destDir == null) {
                return null;
            }
            URI destUri = destDir.toURI();
            try {
                URL destUrl = destUri.toURL();
                return destUrl;
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(format(
                        "Could not retrieve destination directory for task `%s`.",
                        task.getName()), e);
            }
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ModelVerifier.class);
    }
}
