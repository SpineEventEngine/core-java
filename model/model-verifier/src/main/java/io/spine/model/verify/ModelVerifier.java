/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Function;
import io.spine.gradle.ProjectHierarchy;
import io.spine.model.CommandHandlers;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.CommandHandler;
import io.spine.server.model.Model;
import io.spine.server.procman.ProcessManager;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
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
    @SuppressWarnings({
            "IfStatementWithTooManyBranches", // OK in this case.
            "unchecked" // Checked by the `if` statements
    })
    void verify(CommandHandlers spineModel) {
        for (String commandHandlingClass : spineModel.getCommandHandlingTypesList()) {
            final Class<?> cls;
            try {
                log().debug("Trying to load class \'{}\'", commandHandlingClass);
                cls = getModelClass(commandHandlingClass);
            } catch (ClassNotFoundException e) {
                log().warn("Failed to load class {}." +
                                   " Consider using io.spine.tools.spine-model-verifier plugin" +
                                   " only for the modules with the sufficient classpath.",
                           commandHandlingClass);
                continue;
            }
            if (Aggregate.class.isAssignableFrom(cls)) {
                final Class<? extends Aggregate> aggregateClass =
                        (Class<? extends Aggregate>) cls;
                model.asAggregateClass(aggregateClass);
                log().debug("\'{}\' classified as Aggregate type.", aggregateClass);
            } else if (ProcessManager.class.isAssignableFrom(cls)) {
                final Class<? extends ProcessManager> procManClass =
                        (Class<? extends ProcessManager>) cls;
                model.asProcessManagerClass(procManClass);
                log().debug("\'{}\' classified as ProcessManager type.", procManClass);
            } else if (CommandHandler.class.isAssignableFrom(cls)) {
                final Class<? extends CommandHandler> commandHandler =
                        (Class<? extends CommandHandler>) cls;
                model.asCommandHandlerClass(commandHandler);
                log().debug("\'{}\' classified as CommandHandler type.", commandHandler);
            } else {
                throw newIllegalArgumentException("Class %s is not a command handling type.",
                                                  cls.getName());
            }
        }
    }

    private Class<?> getModelClass(String fqn) throws ClassNotFoundException {
        return Class.forName(fqn, false, projectClassLoader);
    }

    private static URLClassLoader createClassLoaderForProject(Project project) {
        final Collection<JavaCompile> tasks = allJavaCompile(project);
        final URL[] compiledCodePath = extractDestinationDirs(tasks);
        log().debug("Initializing ClassLoader for URLs: {}", deepToString(compiledCodePath));
        try {
            @SuppressWarnings("ClassLoaderInstantiation") // Caught exception.
            final URLClassLoader result =
                    new URLClassLoader(compiledCodePath, ModelVerifier.class.getClassLoader());
            return result;
        } catch (SecurityException e) {
            throw new IllegalStateException("Cannot analyze project source code.", e);
        }
    }

    private static Collection<JavaCompile> allJavaCompile(Project project) {
        final Collection<JavaCompile> tasks = newLinkedList();
        ProjectHierarchy.applyToAll(project.getRootProject(), new Action<Project>() {
            @Override
            public void execute(Project project) {
                tasks.addAll(javaCompile(project));
            }
        });
        return tasks;
    }

    private static Collection<JavaCompile> javaCompile(Project project) {
        return project.getTasks().withType(JavaCompile.class);
    }

    private static URL[] extractDestinationDirs(Collection<JavaCompile> tasks) {
        final Collection<URL> urls = transform(tasks, GetDestinationDir.FUNCTION);
        final URL[] result = urls.toArray(EMPTY_URL_ARRAY);
        return result;
    }

    /**
     * A function which retrieves the output directory from the passed Gradle task.
     */
    @VisibleForTesting
    enum GetDestinationDir implements Function<JavaCompile, URL> {
        FUNCTION;

        @Nullable
        @Override
        public URL apply(@Nullable JavaCompile task) {
            checkNotNull(task);
            final File destDir = task.getDestinationDir();
            if (destDir == null) {
                return null;
            }
            final URI destUri = destDir.toURI();
            try {
                final URL destUrl = destUri.toURL();
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
