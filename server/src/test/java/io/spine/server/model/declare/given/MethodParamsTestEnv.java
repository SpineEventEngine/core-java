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

package io.spine.server.model.declare.given;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.spine.base.CommandMessage;
import io.spine.core.CommandContext;
import io.spine.core.UserId;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.type.CommandEnvelope;
import io.spine.system.server.ScheduleCommand;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static io.spine.server.model.declare.MethodParams.consistsOfTwo;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The test environment for {@link io.spine.server.model.declare.MethodParamsTest MethodParamsTest}
 * class.
 *
 * @author Alex Tymchenko
 */
public class MethodParamsTestEnv {

    public static Method singleParamCommand() {
        return findMethod("singleParam");
    }

    public static Method twoParamCommandAndCtx() {
        return findMethod("twoParam");
    }

    public static Method fiveParamMethodStringAnyEmptyInt32UserId() {
        return findMethod("fiveParam");
    }

    public void singleParam(ScheduleCommand command) {
    }

    public void twoParam(ScheduleCommand command, CommandContext context) {
    }

    public void fiveParam(String eurasia,
                          Any australia,
                          Empty antractida,
                          Int32Value northAmerica,
                          UserId southAmerica) {
    }

    private static Method findMethod(String methodName) {
        Optional<Method> method = Arrays.stream(MethodParamsTestEnv.class.getDeclaredMethods())
                                        .filter(m -> m.getName().equals(methodName))
                                        .findFirst();
        if (!method.isPresent()) {
            throw newIllegalStateException("Test method `%s` is missing.", methodName);
        }
        return method.get();
    }

    @Immutable
    public enum ScheduleCommandParamSpec implements ParameterSpec<CommandEnvelope> {

        MESSAGE_AND_CONTEXT {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfTwo(methodParams, CommandMessage.class, CommandContext.class);
            }

            @Override
            public Object[] extractArguments(CommandEnvelope envelope) {
                return new Object[]{envelope.message(), envelope.context()};
            }
        }
    }
}
