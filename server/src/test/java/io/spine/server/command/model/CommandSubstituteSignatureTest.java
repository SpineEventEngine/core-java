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

package io.spine.server.command.model;

import io.spine.server.command.Command;
import io.spine.server.model.MethodSignatureTest;
import io.spine.server.tuple.Pair;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdAssignTask;
import io.spine.test.command.CmdStartTask;

import java.lang.reflect.Method;
import java.util.stream.Stream;

class CommandSubstituteSignatureTest extends MethodSignatureTest<CommandSubstituteSignature> {

    @Override
    protected Stream<Method> validMethods() {
        return Stream.of(
                findMethod(ValidHandler.class, "singleMessageSingleResult"),
                findMethod(ValidHandler.class, "singleMessagePairResult"));
    }

    @Override
    protected Stream<Method> invalidMethods() {
        return Stream.of();
    }

    @Override
    protected CommandSubstituteSignature signature() {
        return new CommandSubstituteSignature();
    }

    /**
     * A command handler which declares valid {@link io.spine.server.command.Command
     * Command} substitution methods.
     */
    private static final class ValidHandler {

        @Command
        CmdStartTask singleMessageSingleResult(CmdAssignTask command) {
            return CmdStartTask.getDefaultInstance();
        }

        @Command
        Pair<CmdAssignTask, CmdStartTask> singleMessagePairResult(CmdAddTask command) {
            return Pair.of(CmdAssignTask.getDefaultInstance(), CmdStartTask.getDefaultInstance());
        }
    }
}
