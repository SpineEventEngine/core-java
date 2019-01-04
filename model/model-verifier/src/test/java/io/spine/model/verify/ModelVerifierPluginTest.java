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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import io.spine.testing.server.model.ModelTests;
import io.spine.tools.gradle.GradleProject;
import io.spine.tools.gradle.TaskName;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.junitpioneer.jupiter.TempDirectory.TempDir;

import java.nio.file.Path;

import static io.spine.tools.gradle.TaskName.VERIFY_MODEL;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(TempDirectory.class)
@DisplayName("ModelVerifierPlugin should")
class ModelVerifierPluginTest {

    private static final String PROJECT_NAME = "model-verifier-test";
    private static final String VALID_AGGREGATE_JAVA =
            "io/spine/model/verify/ValidAggregate.java";
    private static final ImmutableCollection<String> PROTO_FILES = ImmutableList.of(
            "spine/model/verify/call_entity.proto",
            "spine/model/verify/commands.proto",
            "spine/model/verify/events.proto"
    );

    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path junitCreatedDir) {
        tempDir = junitCreatedDir;
        ModelTests.dropAllModels();
    }

    @Test
    @DisplayName("pass valid model classes")
    void passValidModelClasses() {
        newProjectWithJava(VALID_AGGREGATE_JAVA,
                           "io/spine/model/verify/ValidProcMan.java",
                           "io/spine/model/verify/ValidCommandHandler.java")
                .executeTask(VERIFY_MODEL);
    }

    @Test
    @DisplayName("halt build on duplicate command handling methods")
    void rejectDuplicateHandlingMethods() {
        BuildResult result = newProjectWithJava(
                "io/spine/model/verify/DuplicateAggregate.java",
                "io/spine/model/verify/DuplicateCommandHandler.java")
                .executeAndFail(VERIFY_MODEL);
        BuildTask task = result.task(toPath(VERIFY_MODEL));
        assertNotNull(task, result.getOutput());
        TaskOutcome generationResult = task.getOutcome();
        assertEquals(FAILED, generationResult, result.getOutput());
    }

    @Test
    @DisplayName("ignore duplicate entries in a Gradle project")
    void ignoreDuplicateEntries() {
        GradleProject project = newProjectWithJava(VALID_AGGREGATE_JAVA);
        project.executeTask(VERIFY_MODEL);
        project.executeTask(VERIFY_MODEL);
    }

    @Test
    @DisplayName("halt build on malformed command handling methods")
    void rejectMalformedHandlingMethods() {
        BuildResult result = newProjectWithJava("io/spine/model/verify/MalformedAggregate.java")
                .executeAndFail(VERIFY_MODEL);
        BuildTask task = result.task(toPath(VERIFY_MODEL));
        assertNotNull(task, result.getOutput());
        TaskOutcome generationResult = task.getOutcome();
        assertEquals(FAILED, generationResult, result.getOutput());
    }

    private GradleProject newProjectWithJava(String... fileNames) {
        return GradleProject.newBuilder()
                            .setProjectName(PROJECT_NAME)
                            .setProjectFolder(tempDir.toFile())
                            .addJavaFiles(fileNames)
                            .addProtoFiles(PROTO_FILES)
                            .build();
    }

    private static String toPath(TaskName name) {
        return ':' + name.getValue();
    }
}
