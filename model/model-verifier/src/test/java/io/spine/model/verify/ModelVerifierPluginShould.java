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

import io.spine.gradle.GradleProject;
import io.spine.gradle.TaskName;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static io.spine.gradle.TaskName.VERIFY_MODEL;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dmytro Dashenkov
 */
public class ModelVerifierPluginShould {

    private static final String PROJECT_NAME = "model-verifier-test";
    private static final String COMPILING_TEST_ENTITY_PATH =
            "io/spine/model/verify/ValidAggregate.java";

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void pass_valid_model_classes() {
        newProjectWithJava(COMPILING_TEST_ENTITY_PATH,
                           "io/spine/model/verify/ValidProcMan.java",
                           "io/spine/model/verify/ValidCommandHandler.java")
                .executeTask(VERIFY_MODEL);
    }

    @Test
    public void halt_build_on_duplicate_command_handling_methods() {
        final BuildResult result = newProjectWithJava(
                "io/spine/model/verify/DuplicateAggregate.java",
                "io/spine/model/verify/DuplicateCommandHandler.java")
                .executeAndFail(VERIFY_MODEL);
        final BuildTask task = result.task(toPath(VERIFY_MODEL));
        assertNotNull(task);
        final TaskOutcome generationResult = task.getOutcome();
        assertEquals(FAILED, generationResult);
    }

    @Test
    public void ignore_duplicate_entries() {
        final GradleProject project = newProjectWithJava(COMPILING_TEST_ENTITY_PATH);
        project.executeTask(VERIFY_MODEL);
        project.executeTask(VERIFY_MODEL);
    }

    @Ignore // TODO:2017-08-25:dmytro.dashenkov: Re-enable when Model is capable of checking the handler methods.
            // https://github.com/SpineEventEngine/base/issues/49
    @Test
    public void halt_build_on_malformed_command_handling_methods() {
        final BuildResult result =
                newProjectWithJava("io/spine/model/verify/MalformedAggregate.java")
                .executeAndFail(VERIFY_MODEL);
        final BuildTask task = result.task(toPath(VERIFY_MODEL));
        assertNotNull(task);
        final TaskOutcome generationResult = task.getOutcome();
        assertEquals(FAILED, generationResult);
    }

    private GradleProject newProjectWithJava(String... fileNames) {
        return GradleProject.newBuilder()
                            .setProjectName(PROJECT_NAME)
                            .setProjectFolder(testProjectDir)
                            .addJavaFiles(fileNames)
                            .build();
    }

    private static String toPath(TaskName name) {
        return ':' + name.getValue();
    }
}
