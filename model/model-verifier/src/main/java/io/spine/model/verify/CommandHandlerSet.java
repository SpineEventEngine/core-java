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

import io.spine.logging.Logging;
import io.spine.model.CommandHandlers;
import io.spine.server.command.model.DuplicateHandlerCheck;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.file.Files.newInputStream;

final class CommandHandlerSet implements Logging {

    private final CommandHandlers handlers;

    private CommandHandlerSet(CommandHandlers handlers) {
        this.handlers = handlers;
    }

    static CommandHandlerSet parse(Path modelPath) {
        CommandHandlers handlers = readCommandHandlers(modelPath);
        return new CommandHandlerSet(handlers);
    }

    void verifyAgainst(ClassLoader classLoader) {
        ClassSet classSet = new ClassSet(classLoader,
                                         handlers.getCommandHandlingTypesList());
        classSet.reportNotFoundIfAny(log());
        DuplicateHandlerCheck.newInstance()
                             .check(classSet.elements());
    }

    private static CommandHandlers readCommandHandlers(Path modelPath) {
        try (InputStream in = newInputStream(modelPath, StandardOpenOption.READ)) {
            return CommandHandlers.parseFrom(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
