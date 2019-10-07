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

import com.google.common.io.Files;
import io.spine.model.CommandHandlers;
import io.spine.model.verify.ModelVerifier.GetDestinationDir;
import io.spine.model.verify.given.DuplicateCommandHandler;
import io.spine.model.verify.given.EditAggregate;
import io.spine.model.verify.given.InvalidCommander;
import io.spine.model.verify.given.InvalidDeleteAggregate;
import io.spine.model.verify.given.InvalidEnhanceAggregate;
import io.spine.model.verify.given.InvalidRestoreAggregate;
import io.spine.model.verify.given.RenameProcMan;
import io.spine.model.verify.given.UploadCommandHandler;
import io.spine.server.command.model.CommandHandlerSignature;
import io.spine.server.model.DuplicateCommandHandlerError;
import io.spine.server.model.ExternalCommandReceiverMethodError;
import io.spine.server.model.SignatureMismatchException;
import io.spine.testing.logging.LogRecordSubject;
import io.spine.testing.logging.LoggingTest;
import io.spine.testing.logging.MuteLogging;
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

import static io.spine.tools.gradle.JavaTaskName.compileJava;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ModelVerifier should")
class ModelVerifierTest {

    private Project project = null;

    static Project actualProject() {
        Project result = ProjectBuilder.builder().build();
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
        ModelVerifier verifier = new ModelVerifier(project);

        String commandHandlerTypeName = UploadCommandHandler.class.getName();
        String aggregateTypeName = EditAggregate.class.getName();
        String procManTypeName = RenameProcMan.class.getName();
        CommandHandlers spineModel = CommandHandlers
                .newBuilder()
                .addCommandHandlingType(commandHandlerTypeName)
                .addCommandHandlingType(aggregateTypeName)
                .addCommandHandlingType(procManTypeName)
                .build();
        verifier.verify(spineModel);
    }

    @ParameterizedTest
    @DisplayName("throw when attempting to verify a model that declares an invalid command handler")
    @MethodSource("getBadHandlers")
    void throwOnSignatureMismatch(String badHandlerName) {
        ModelVerifier verifier = new ModelVerifier(project);
        CommandHandlers model = CommandHandlers
                .newBuilder()
                .addCommandHandlingType(badHandlerName)
                .build();
        assertThrows(SignatureMismatchException.class, () -> verifier.verify(model));
    }

    private static Stream<Arguments> getBadHandlers() {
        return Stream.of(
                Arguments.of(InvalidDeleteAggregate.class.getName()),
                Arguments.of(InvalidEnhanceAggregate.class.getName()));
    }

    @Test
    @DisplayName("fail on duplicate command handlers")
    void failOnDuplicateHandlers() {
        ModelVerifier verifier = new ModelVerifier(project);
        String firstType = UploadCommandHandler.class.getName();
        String secondType = DuplicateCommandHandler.class.getName();

        CommandHandlers spineModel = CommandHandlers
                .newBuilder()
                .addCommandHandlingType(firstType)
                .addCommandHandlingType(secondType)
                .build();
        assertThrows(DuplicateCommandHandlerError.class, () -> verifier.verify(spineModel));
    }

    @Test
    @DisplayName("fail on command receiving methods marked as external")
    void failOnExternalCommandReceivers() {
        ModelVerifier verifier = new ModelVerifier(project);
        String invalidProcman = InvalidCommander.class.getName();
        CommandHandlers spineModel = CommandHandlers
                .newBuilder()
                .addCommandHandlingType(invalidProcman)
                .build();
        assertThrows(ExternalCommandReceiverMethodError.class, () -> verifier.verify(spineModel));
    }

    @Nested
    @DisplayName("produce a warning")
    class WarnLogging extends LoggingTest {

        private final Class<?> aggregateClass = InvalidRestoreAggregate.class;

        WarnLogging() {
            super(CommandHandlerSignature.class, Level.WARNING);
        }

        @BeforeEach
        void verifyModel() {
            ModelVerifier verifier = new ModelVerifier(project);
            // Add handler here to avoid unnecessary logging.
            interceptLogging();
            CommandHandlers model = CommandHandlers
                    .newBuilder()
                    .addCommandHandlingType(aggregateClass.getName())
                    .build();
            verifier.verify(model);
        }

        @AfterEach
        void removeLogHook() {
            restoreLogging();
        }

        @Test
        @DisplayName("on `private` command handling methods")
        void onPrivateMethod() {
            LogRecordSubject assertRecord = assertLog().record();
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
        String invalidClassname = "non.existing.class.Name";
        CommandHandlers spineModel = CommandHandlers
                .newBuilder()
                .addCommandHandlingType(invalidClassname)
                .build();
        new ModelVerifier(project).verify(spineModel);
    }

    @Test
    @DisplayName("not accept non-CommandHandler types")
    void rejectNonHandlerTypes() {
        String invalidClassname = ModelVerifierTest.class.getName();
        CommandHandlers spineModel = CommandHandlers
                .newBuilder()
                .addCommandHandlingType(invalidClassname)
                .build();
        assertThrows(IllegalArgumentException.class,
                     () -> new ModelVerifier(project).verify(spineModel));
    }

    @Test
    @DisplayName("retrieve compilation destination directory from task")
    void getCompilationDestDir() throws MalformedURLException {
        JavaCompile compileTask = actualProject().getTasks()
                                                 .withType(JavaCompile.class)
                                                 .getByName(compileJava.name());
        File dest = Files.createTempDir();
        compileTask.setDestinationDir(dest);
        Function<JavaCompile, URL> func = GetDestinationDir.FUNCTION;
        URL destUrl = dest.toURI().toURL();
        assertEquals(destUrl, func.apply(compileTask));
    }

    @Test
    @DisplayName("retrieve null if destination directory is null")
    void getNullDestDir() {
        JavaCompile compileTask = actualProject().getTasks()
                                                 .withType(JavaCompile.class)
                                                 .getByName(compileJava.name());
        compileTask.setDestinationDir((File) null);
        Function<JavaCompile, URL> func = GetDestinationDir.FUNCTION;
        assertNull(func.apply(compileTask));
    }
}
