/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command.error;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Value;
import org.spine3.base.Command;
import org.spine3.base.Error;

import java.util.Map;

/**
 * Abstract base for exceptions related to commands.
 *
 * @author Alexander Yevsyukov
 */
public abstract class CommandException extends RuntimeException {

    private final Command command;
    private final Error error;

    public CommandException(String messageText, Command command, Error error) {
        super(messageText);
        this.error = error;
        this.command = command;
    }

    public static Map<String, Value> commandTypeAttribute(String commandType) {
        return ImmutableMap.of(Attribute.COMMAND_TYPE_NAME, Value.newBuilder().setStringValue(commandType).build());
    }

    public Command getCommand() {
        return command;
    }

    public Error getError() {
        return error;
    }

    private static final long serialVersionUID = 0L;

    /**
     * Attribute names for command-related business failures.
     */
    public interface Attribute {
        String COMMAND_TYPE_NAME = "commandType";
    }
}
