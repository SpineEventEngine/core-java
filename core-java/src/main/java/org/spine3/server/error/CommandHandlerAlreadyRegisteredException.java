/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.error;

import org.spine3.CommandClass;

/**
 * Exception that is thrown when more than one handler
 * of the same command type were found in the command dispatcher instance.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class CommandHandlerAlreadyRegisteredException extends RuntimeException {

    /**
     * Accepts event type and both old and new handlers.
     *
     * @param commandClass         the class of the command
     * @param currentHandler    a method name of the currently registered for the given message type
     * @param discoveredHandler another method name discovered for the command type
     */
    public CommandHandlerAlreadyRegisteredException(
            CommandClass commandClass,
            String currentHandler,
            String discoveredHandler) {

        super(String.format("The command %s already has associated handler method %s. "
                        + " There can be only one handler per command class. "
                        + " Duplicating handler encountered: %s. "
                        + " Consider un-registering the current handler first.",
                commandClass, currentHandler, discoveredHandler));
    }

    private static final long serialVersionUID = 0L;
}
