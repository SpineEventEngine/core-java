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

import com.google.common.collect.ImmutableSet;
import io.spine.code.proto.MessageType;
import io.spine.logging.Logging;
import io.spine.model.CommandHandlers;
import io.spine.model.verify.given.DuplicateCommandHandler;
import io.spine.model.verify.given.EditAggregate;
import io.spine.model.verify.given.InvalidDeleteAggregate;
import io.spine.model.verify.given.InvalidEnhanceAggregate;
import io.spine.model.verify.given.InvalidRestoreAggregate;
import io.spine.model.verify.given.RenameProcMan;
import io.spine.model.verify.given.UploadCommandHandler;
import io.spine.server.command.model.CommandHandlerSignature;
import io.spine.server.model.DuplicateCommandHandlerError;
import io.spine.server.model.TypeMismatchError;
import io.spine.server.model.UnknownReferencedTypeError;
import io.spine.server.model.declare.SignatureMismatchException;
import io.spine.test.model.verify.command.UploadPhoto;
import io.spine.test.model.verify.given.ArchiveState;
import io.spine.test.model.verify.given.DeleteState;
import io.spine.test.model.verify.given.RenameState;
import io.spine.testing.logging.MuteLogging;
import org.gradle.api.Project;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.Stream;

import static java.util.Collections.emptyIterator;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Model elements that are subject to verification should")
class ModelVerificationTest {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private ClassLoader projectClassLoader;

    @SuppressWarnings("unchecked") // OK for test mocks.
    @BeforeEach
    void setUp() {
        Project project = mock(Project.class);
        ScriptHandler buildScript = mock(ScriptHandler.class);
        when(buildScript.getClassLoader()).thenReturn(ModelVerificationTest.class.getClassLoader());
        when(project.getSubprojects()).thenReturn(emptySet());
        when(project.getRootProject()).thenReturn(project);
        when(project.getBuildscript()).thenReturn(buildScript);

        TaskContainer tasks = mock(TaskContainer.class);
        TaskCollection emptyTaskCollection = mock(TaskCollection.class);
        when(emptyTaskCollection.iterator()).thenReturn(emptyIterator());
        when(emptyTaskCollection.toArray()).thenReturn(EMPTY_ARRAY);
        when(tasks.withType(any(Class.class))).thenReturn(emptyTaskCollection);
        when(project.getTasks()).thenReturn(tasks);

        ProjectClassLoader classLoader = new ProjectClassLoader(project);
        projectClassLoader = classLoader.get();
    }

    @Test
    @DisplayName("be verified against classpath")
    void verifyAgainstClasspath() {
        String commandHandlerTypeName = UploadCommandHandler.class.getName();
        String aggregateTypeName = EditAggregate.class.getName();
        String procManTypeName = RenameProcMan.class.getName();
        CommandHandlers commandHandlers = CommandHandlers
                .newBuilder()
                .addCommandHandlingTypes(commandHandlerTypeName)
                .addCommandHandlingTypes(aggregateTypeName)
                .addCommandHandlingTypes(procManTypeName)
                .build();

        CommandHandlerSet handlerSet = spy(new CommandHandlerSet(commandHandlers));
        Model model = new Model(handlerSet, validLifecycle());
        model.verifyAgainst(projectClassLoader);

        verify(handlerSet, times(1)).checkAgainst(projectClassLoader);
    }

    @ParameterizedTest
    @DisplayName("fail with `SignatureMismatchException` on invalid command handler")
    @MethodSource("getBadHandlers")
    void throwOnSignatureMismatch(String badHandlerName) {
        CommandHandlers handlers = CommandHandlers
                .newBuilder()
                .addCommandHandlingTypes(badHandlerName)
                .build();
        Model model = modelWith(handlers);
        assertThrows(SignatureMismatchException.class,
                     () -> model.verifyAgainst(projectClassLoader));
    }

    private static Stream<Arguments> getBadHandlers() {
        return Stream.of(
                Arguments.of(InvalidDeleteAggregate.class.getName()),
                Arguments.of(InvalidEnhanceAggregate.class.getName()));
    }

    @Test
    @DisplayName("fail with `DuplicateCommandHandlerError` on duplicate command handlers")
    void failOnDuplicateHandlers() {
        String firstType = UploadCommandHandler.class.getName();
        String secondType = DuplicateCommandHandler.class.getName();

        CommandHandlers handlers = CommandHandlers
                .newBuilder()
                .addCommandHandlingTypes(firstType)
                .addCommandHandlingTypes(secondType)
                .build();
        Model model = modelWith(handlers);
        assertThrows(DuplicateCommandHandlerError.class,
                     () -> model.verifyAgainst(projectClassLoader));
    }

    @Test
    @DisplayName("produce a warning on private command handling methods")
    void warnOnPrivateHandlers() {
        Queue<SubstituteLoggingEvent> loggedMessages = redirectLogging();
        CommandHandlers handlers = CommandHandlers
                .newBuilder()
                .addCommandHandlingTypes(InvalidRestoreAggregate.class.getName())
                .build();
        Model model = modelWith(handlers);
        model.verifyAgainst(projectClassLoader);
        assertEquals(1, loggedMessages.size());
        SubstituteLoggingEvent event = loggedMessages.poll();
        assertEquals(event.getLevel(), Level.WARN);
    }

    /** Redirects logging produced by model verifier to a {@code Queue} that is returned. */
    private static Queue<SubstituteLoggingEvent> redirectLogging() {
        Queue<SubstituteLoggingEvent> loggedMessages = new ArrayDeque<>();
        Logging.redirect((SubstituteLogger) Logging.get(CommandHandlerSignature.class),
                         loggedMessages);
        return loggedMessages;
    }

    @Test
    @MuteLogging
    @DisplayName("pass the verification with invalid class names")
    void ignoreInvalidClassNames() {
        String invalidClassname = "non.existing.class.Name";
        CommandHandlers handlers = CommandHandlers
                .newBuilder()
                .addCommandHandlingTypes(invalidClassname)
                .build();
        Model model = modelWith(handlers);
        model.verifyAgainst(projectClassLoader);
    }

    @Test
    @DisplayName("fail with `IllegalArgumentException` on non-`CommandHandler` types")
    void rejectNonHandlerTypes() {
        String invalidClassname = ModelVerificationTest.class.getName();
        CommandHandlers handlers = CommandHandlers
                .newBuilder()
                .addCommandHandlingTypes(invalidClassname)
                .build();
        Model model = modelWith(handlers);
        assertThrows(IllegalArgumentException.class,
                     () -> model.verifyAgainst(projectClassLoader));
    }

    @Test
    @MuteLogging
    @DisplayName("fail with `TypeMismatchError` when lifecycle is declared for non-PM type")
    void failOnNonPmLifecycle() {
        MessageType nonPmType = MessageType.of(UploadPhoto.class);
        EntitiesLifecycle lifecycle = new EntitiesLifecycle(ImmutableSet.of(nonPmType));
        Model model = modelWith(lifecycle);
        assertThrows(TypeMismatchError.class, () -> model.verifyAgainst(projectClassLoader));
    }

    @Test
    @DisplayName("fail with `UnknownReferencedTypeError` when option references unknown types")
    void failOnUnknownLifecycleTriggers() {
        MessageType nonPmType = MessageType.of(ArchiveState.class);
        EntitiesLifecycle lifecycle = new EntitiesLifecycle(ImmutableSet.of(nonPmType));
        Model model = modelWith(lifecycle);
        assertThrows(UnknownReferencedTypeError.class,
                     () -> model.verifyAgainst(projectClassLoader));
    }

    @Test
    @DisplayName("fail with `TypeMismatchError` when option references non-event types")
    void failOnNonEventTriggers() {
        MessageType nonPmType = MessageType.of(DeleteState.class);
        EntitiesLifecycle lifecycle = new EntitiesLifecycle(ImmutableSet.of(nonPmType));
        Model model = modelWith(lifecycle);
        assertThrows(TypeMismatchError.class,
                     () -> model.verifyAgainst(projectClassLoader));
    }

    private static Model modelWith(CommandHandlers handlers) {
        CommandHandlerSet handlerSet = new CommandHandlerSet(handlers);
        EntitiesLifecycle lifecycle = validLifecycle();
        Model result = new Model(handlerSet, lifecycle);
        return result;
    }

    private static Model modelWith(EntitiesLifecycle lifecycle) {
        CommandHandlers handlers = CommandHandlers
                .newBuilder()
                .build();
        CommandHandlerSet handlerSet = new CommandHandlerSet(handlers);
        Model result = new Model(handlerSet, lifecycle);
        return result;
    }

    /**
     * Provides a valid lifecycle as entity lifecycle in {@link io.spine.type.KnownTypes} is
     * invalid due to {@link io.spine.test.model.verify.given.ArchiveState} and
     * {@link io.spine.test.model.verify.given.DeleteState}.
     */
    private static EntitiesLifecycle validLifecycle() {
        MessageType validPmType = MessageType.of(RenameState.class);
        return new EntitiesLifecycle(ImmutableSet.of(validPmType));
    }
}
