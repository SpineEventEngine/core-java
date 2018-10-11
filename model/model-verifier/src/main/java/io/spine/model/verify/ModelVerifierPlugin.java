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

import io.spine.annotation.Experimental;
import io.spine.logging.Logging;
import io.spine.model.CommandHandlers;
import io.spine.model.assemble.AssignLookup;
import io.spine.tools.gradle.SpinePlugin;
import io.spine.tools.gradle.compiler.Extension;
import io.spine.tools.gradle.compiler.ModelCompilerPlugin;
import io.spine.tools.type.MoreKnownTypes;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static io.spine.tools.gradle.TaskName.CLASSES;
import static io.spine.tools.gradle.TaskName.COMPILE_JAVA;
import static io.spine.tools.gradle.TaskName.VERIFY_MODEL;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;

/**
 * The plugin performing the Spine type model verification.
 *
 * @author Dmytro Dashenkov
 */
@Experimental
public final class ModelVerifierPlugin extends SpinePlugin {

    private static final String RELATIVE_RAW_MODEL_PATH = AssignLookup.DESTINATION_PATH;

    @Override
    public void apply(Project project) {
        log().debug("Applying Spine model verifier plugin.");
        Path rawModelStorage = rawModelPath(project);
        // Ensure right environment (`main` scope sources with the `java` plugin)
        if (project.getTasks()
                   .findByPath(CLASSES.getValue()) != null) {
            createTask(rawModelStorage, project);
        }
    }

    private void createTask(Path rawModelStorage, Project project) {
        log().debug("Adding task {}", VERIFY_MODEL.getValue());
        newTask(VERIFY_MODEL, action(rawModelStorage))
                .insertBeforeTask(CLASSES)
                .insertAfterTask(COMPILE_JAVA)
                .withInputFiles(rawModelStorage)
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
     * Opens the method to the helper class.
     */
    @SuppressWarnings("RedundantMethodOverride") // See Javadoc.
    @Override
    protected Logger log() {
        return super.log();
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
                _warn("No Spine model definition found under {}.", rawModelPath);
            } else {
                Project project = task.getProject();
                extendKnownTypes(project);
                verifyModel(project);
            }
        }

        private void extendKnownTypes(Project project) {
            String pluginExtensionName = ModelCompilerPlugin.extensionName();
            if (project.getExtensions().findByName(pluginExtensionName) != null) {
                String path = Extension.getMainDescriptorSetPath(project);
                File descriptorFile = new File(path);
                if (descriptorFile.exists()) {
                    _debug("Extending known types with types from {}.", descriptorFile);
                    MoreKnownTypes.extendWith(descriptorFile);
                } else {
                    _warn("Descriptor file {} does not exist.", descriptorFile);
                }
            } else {
                _warn("{} plugin extension is not found. Apply the Spine model compiler plugin.",
                      pluginExtensionName);
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
        public Logger log() {
            return parent.log();
        }
    }
}
