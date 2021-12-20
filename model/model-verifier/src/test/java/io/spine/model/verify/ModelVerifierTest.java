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

import io.spine.model.CommandReceivers;
import io.spine.model.verify.ModelVerifier.GetDestinationDir;
import io.spine.model.verify.given.DuplicateCommandAssignee;
import io.spine.model.verify.given.EditAggregate;
import io.spine.model.verify.given.InvalidCommander;
import io.spine.model.verify.given.InvalidDeleteAggregate;
import io.spine.model.verify.given.InvalidEnhanceAggregate;
import io.spine.model.verify.given.InvalidRestoreAggregate;
import io.spine.model.verify.given.RenameProcMan;
import io.spine.model.verify.given.UploadCommandAssignee;
import io.spine.server.command.model.CommandAssigneeSignature;
import io.spine.server.model.DuplicateCommandHandlerError;
import io.spine.server.model.ExternalCommandReceiverMethodError;
import io.spine.server.model.SignatureMismatchException;
import io.spine.testing.TempDir;
import io.spine.testing.logging.LoggingTest;
import io.spine.testing.logging.mute.MuteLogging;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

import static io.spine.tools.gradle.task.JavaTaskName.compileJava;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`ModelVerifier` should")
class ModelVerifierTest {

    private Project project = null;

    static Project actualProject() {
        var result = ProjectBuilder.builder().build();
        result.getPluginManager().apply("java");
        return result;
    }

    @BeforeEach
    void setUp() {
       project = actualProject();
    }

    @Test
    @DisplayName("verify model from classpath")
    void verifyModel() {
        var verifier = new ModelVerifier(project);
        var commandAssigneeTypeName = UploadCommandAssignee.class.getName();
        var aggregateTypeName = EditAggregate.class.getName();
        var procManTypeName = RenameProcMan.class.getName();
        var spineModel = CommandReceivers.newBuilder()
                .addCommandReceiverType(commandAssigneeTypeName)
                .addCommandReceiverType(aggregateTypeName)
                .addCommandReceiverType(procManTypeName)
                .build();
        verifier.verify(spineModel);
    }

    @ParameterizedTest
    @DisplayName("fail on an invalid command receiving method")
    @MethodSource("getBadReceivers")
    void throwOnSignatureMismatch(String badReceiver) {
        var verifier = new ModelVerifier(project);
        var model = CommandReceivers.newBuilder()
                .addCommandReceiverType(badReceiver)
                .build();
        assertThrows(SignatureMismatchException.class, () -> verifier.verify(model));
    }

    private static Stream<Arguments> getBadReceivers() {
        return Stream.of(
                Arguments.of(InvalidDeleteAggregate.class.getName()),
                Arguments.of(InvalidEnhanceAggregate.class.getName()));
    }

    @Test
    @DisplayName("fail on duplicate command receivers")
    void failOnDuplicateAssignees() {
        var verifier = new ModelVerifier(project);
        var firstType = UploadCommandAssignee.class.getName();
        var secondType = DuplicateCommandAssignee.class.getName();

        var spineModel = CommandReceivers.newBuilder()
                .addCommandReceiverType(firstType)
                .addCommandReceiverType(secondType)
                .build();
        assertThrows(DuplicateCommandHandlerError.class, () -> verifier.verify(spineModel));
    }

    @Test
    @DisplayName("fail on command receiving methods marked as external")
    void failOnExternalCommandReceivers() {
        var verifier = new ModelVerifier(project);
        var invalidProcman = InvalidCommander.class.getName();
        var spineModel = CommandReceivers.newBuilder()
                .addCommandReceiverType(invalidProcman)
                .build();
        assertThrows(ExternalCommandReceiverMethodError.class, () -> verifier.verify(spineModel));
    }

    @Nested
    @DisplayName("produce a warning")
    class WarnLogging extends LoggingTest {

        private final Class<?> aggregateClass = InvalidRestoreAggregate.class;

        WarnLogging() {
            super(CommandAssigneeSignature.class, Level.WARNING);
        }

        @BeforeEach
        void verifyModel() {
            var verifier = new ModelVerifier(project);
            // Add an assignee here to avoid unnecessary logging.
            interceptLogging();
            var model = CommandReceivers.newBuilder()
                    .addCommandReceiverType(aggregateClass.getName())
                    .build();
            verifier.verify(model);
        }

        @AfterEach
        void removeLogHook() {
            restoreLogging();
        }

        @Test
        @DisplayName("on `private` command receiving methods")
        void onPrivateMethod() {
            var assertRecord = assertLog().record();
            assertRecord.hasLevelThat()
                        .isEqualTo(level());
            assertRecord.hasMessageThat()
                        .contains(aggregateClass.getName());
        }
    }

    @Test
    @MuteLogging
    @DisplayName("ignore invalid class names")
    void ignoreInvalidClassNames() {
        var invalidClassname = "non.existing.class.Name";
        var spineModel = CommandReceivers.newBuilder()
                .addCommandReceiverType(invalidClassname)
                .build();
        new ModelVerifier(project).verify(spineModel);
    }

    @Test
    @DisplayName("not accept non-CommandReceiver types")
    void rejectNonAssigneeTypes() {
        var invalidClassname = ModelVerifierTest.class.getName();
        var spineModel = CommandReceivers.newBuilder()
                .addCommandReceiverType(invalidClassname)
                .build();
        assertThrows(IllegalArgumentException.class,
                     () -> new ModelVerifier(project).verify(spineModel));
    }

    @Test
    @DisplayName("retrieve compilation destination directory from task")
    void getCompilationDestDir() throws MalformedURLException {
        var compileTask = actualProject()
                .getTasks()
                .withType(JavaCompile.class)
                .getByName(compileJava.name());
        var dest = TempDir.forClass(getClass());
        compileTask.getDestinationDirectory().set(dest);
        Function<JavaCompile, URL> func = GetDestinationDir.FUNCTION;
        var destUrl = dest.toURI().toURL();
        assertEquals(destUrl, func.apply(compileTask));
    }

    @Test
    @DisplayName("retrieve `null` if destination directory is null")
    void getNullDestDir() {
        var compileTask = actualProject()
                .getTasks()
                .withType(JavaCompile.class)
                .getByName(compileJava.name());
        compileTask.getDestinationDirectory().set((File) null);
        Function<JavaCompile, URL> func = GetDestinationDir.FUNCTION;
        assertNull(func.apply(compileTask));
    }
}
