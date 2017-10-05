/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.gradle.SpinePlugin;
import io.spine.model.CommandHandlers;
import io.spine.model.assemble.AssignLookup;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static io.spine.gradle.TaskName.CLASSES;
import static io.spine.gradle.TaskName.COMPILE_JAVA;
import static io.spine.gradle.TaskName.VERIFY_MODEL;
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
        final Path rawModelStorage = rawModelPath(project);
        // Ensure right environment (`main` scope sources with the `java` plugin)
        if (project.getTasks().findByPath(CLASSES.getValue()) != null) {
            createTask(rawModelStorage, project);
        }
    }

    private void createTask(Path rawModelStorage, Project project) {
        log().debug("Adding task {}", VERIFY_MODEL.getValue());
        newTask(VERIFY_MODEL, action(rawModelStorage)).insertBeforeTask(CLASSES)
                                                      .insertAfterTask(COMPILE_JAVA)
                                                      .withInputFiles(rawModelStorage)
                                                      .applyNowTo(project);
    }

    private static Path rawModelPath(Project project) {
        final Path rootDir = project.getRootDir().toPath();
        final Path result = rootDir.resolve(RELATIVE_RAW_MODEL_PATH);
        return result;
    }

    private static Action<Task> action(Path path) {
        return new VerifierAction(path);
    }

    /**
     * Verifies the {@link CommandHandlers} upon the {@linkplain Project Gradle project}.
     *
     * @param model   the Spine model to process
     * @param project the Gradle project to process the model upon
     */
    private static void verifyModel(CommandHandlers model, Project project) {
        final ModelVerifier verifier = new ModelVerifier(project);
        verifier.verify(model);
    }

    /**
     * The action performing the model processing.
     *
     * <p>The action is executed only if the passed {@code rawModelPath} is present.
     *
     * <p>Reads the {@link CommandHandlers} from the given file and {@linkplain #verifyModel processes}
     * the model.
     */
    private static class VerifierAction implements Action<Task> {

        private final Path rawModelPath;

        private VerifierAction(Path rawModelPath) {
            this.rawModelPath = rawModelPath;
        }

        @Override
        public void execute(Task task) {
            if (!exists(rawModelPath)) {
                log().warn("No Spine model description found under {}. Completing the task.",
                           rawModelPath);
                return;
            }
            final CommandHandlers model;
            try (InputStream in = newInputStream(rawModelPath, StandardOpenOption.READ)) {
                model = CommandHandlers.parseFrom(in);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            verifyModel(model, task.getProject());
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ModelVerifierPlugin.class);
    }
}
