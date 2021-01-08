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

package io.spine.model.verify;

import com.google.common.flogger.FluentLogger;
import io.spine.annotation.Experimental;
import io.spine.logging.Logging;
import io.spine.model.CommandHandlers;
import io.spine.model.assemble.AssignLookup;
import io.spine.tools.gradle.SpinePlugin;
import io.spine.tools.gradle.compiler.ModelCompilerPlugin;
import io.spine.tools.type.MoreKnownTypes;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static io.spine.tools.gradle.JavaTaskName.classes;
import static io.spine.tools.gradle.JavaTaskName.compileJava;
import static io.spine.tools.gradle.ModelVerifierTaskName.verifyModel;
import static io.spine.tools.gradle.compiler.Extension.getMainDescriptorSet;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;

/**
 * The plugin performing the Spine type model verification.
 */
@Experimental
public final class ModelVerifierPlugin extends SpinePlugin {

    private static final String RELATIVE_RAW_MODEL_PATH = AssignLookup.DESTINATION_PATH;

    @Override
    public void apply(Project project) {
        _debug().log("Applying Spine model verifier plugin.");
        Path rawModelStorage = rawModelPath(project);
        // Ensure right environment (`main` scope sources with the `java` plugin)
        if (project.getTasks()
                   .findByPath(classes.name()) != null) {
            createTask(rawModelStorage, project);
        }
    }

    private void createTask(Path rawModelStorage, Project project) {
        _debug().log("Adding task `%s`.", verifyModel);
        newTask(verifyModel, action(rawModelStorage))
                .insertBeforeTask(classes)
                .insertAfterTask(compileJava)
                .applyNowTo(project);
    }

    private static Path rawModelPath(Project project) {
        Path rootDir = project.getRootDir().toPath();
        Path result = rootDir.resolve(RELATIVE_RAW_MODEL_PATH);
        return result;
    }

    private Action<Task> action(Path path) {
        return new VerifierAction(this, path);
    }

    /**
     * The action performing the model processing.
     *
     * <p>The action is executed only if the passed {@code rawModelPath} is present.
     *
     * <p>Reads the {@link CommandHandlers} from the given file and
     * {@linkplain #verifyModel processes} the model.
     */
    private static class VerifierAction implements Action<Task>, Logging {

        private final ModelVerifierPlugin parent;
        private final Path rawModelPath;

        private VerifierAction(ModelVerifierPlugin parent, Path rawModelPath) {
            this.parent = parent;
            this.rawModelPath = rawModelPath;
        }

        @Override
        public void execute(Task task) {
            if (!exists(rawModelPath)) {
                _warn().log("No Spine model definition found under `%s`.", rawModelPath);
            } else {
                Project project = task.getProject();
                extendKnownTypes(project);
                verifyModel(project);
            }
        }

        private void extendKnownTypes(Project project) {
            String pluginExtensionName = ModelCompilerPlugin.extensionName();
            if (project.getExtensions().findByName(pluginExtensionName) != null) {
                File descriptorFile = getMainDescriptorSet(project);
                tryExtend(descriptorFile);
            } else {
                _warn().log(
                        "`%s` plugin extension is not found." +
                                " Please apply the Spine model compiler plugin.",
                        pluginExtensionName
                );
            }
        }

        private void tryExtend(File descriptorFile) {
            if (descriptorFile.exists()) {
                _debug().log("Extending known types with types from `%s`.", descriptorFile);
                MoreKnownTypes.extendWith(descriptorFile);
            } else {
                _warn().log("Descriptor file `%s` does not exist.", descriptorFile);
            }
        }

        /**
         * Verifies the {@link CommandHandlers} upon the {@linkplain Project Gradle project}.
         *
         * @param project the Gradle project to process the model upon
         */
        private void verifyModel(Project project) {
            ModelVerifier verifier = new ModelVerifier(project);
            CommandHandlers commandHandlers = readCommandHandlers();
            verifier.verify(commandHandlers);
        }

        private CommandHandlers readCommandHandlers() {
            try (InputStream in = newInputStream(rawModelPath, StandardOpenOption.READ)) {
                return CommandHandlers.parseFrom(in);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public FluentLogger logger() {
            return parent.logger();
        }
    }
}
