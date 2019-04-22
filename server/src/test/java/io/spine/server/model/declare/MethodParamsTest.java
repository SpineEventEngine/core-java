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

package io.spine.server.model.declare;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.spine.core.CommandContext;
import io.spine.core.UserId;
import io.spine.server.model.declare.given.MethodParamsTestEnv.ScheduleCommandParamSpec;
import io.spine.system.server.command.ScheduleCommand;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.server.model.declare.MethodParams.consistsOfSingle;
import static io.spine.server.model.declare.MethodParams.consistsOfTwo;
import static io.spine.server.model.declare.MethodParams.consistsOfTypes;
import static io.spine.server.model.declare.MethodParams.findMatching;
import static io.spine.server.model.declare.MethodParams.isFirstParamCommand;
import static io.spine.server.model.declare.given.MethodParamsTestEnv.fiveParamMethodStringAnyEmptyInt32UserId;
import static io.spine.server.model.declare.given.MethodParamsTestEnv.singleParamCommand;
import static io.spine.server.model.declare.given.MethodParamsTestEnv.twoParamCommandAndCtx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`MethodParams` utility should ")
class MethodParamsTest extends UtilityClassTest<MethodParams> {

    private MethodParamsTest() {
        super(MethodParams.class);
    }

    @Test
    @DisplayName("detect that a method has exactly one parameter of an expected type")
    void detectSingleParam() {
        assertTrue(consistsOfSingle(singleParamCommand().getParameterTypes(),
                                    ScheduleCommand.class));
        assertFalse(consistsOfSingle(twoParamCommandAndCtx().getParameterTypes(),
                                     ScheduleCommand.class));
        assertFalse(consistsOfSingle(fiveParamMethodStringAnyEmptyInt32UserId().getParameterTypes(),
                                     ScheduleCommand.class));

    }

    @Test
    @DisplayName("detect that a method has exactly two parameters of expected types")
    void detectTwoParams() {
        assertTrue(consistsOfTwo(twoParamCommandAndCtx().getParameterTypes(),
                                 ScheduleCommand.class, CommandContext.class));
        assertFalse(consistsOfTwo(singleParamCommand().getParameterTypes(),
                                  ScheduleCommand.class, CommandContext.class));
        assertFalse(consistsOfTwo(fiveParamMethodStringAnyEmptyInt32UserId().getParameterTypes(),
                                  ScheduleCommand.class, CommandContext.class));

    }

    @Test
    @DisplayName("detect that a method has lots of parameters of expected types")
    void detectLotsOfParams() {
        assertTrue(consistsOfTypes(fiveParamMethodStringAnyEmptyInt32UserId().getParameterTypes(),
                                   ImmutableList.of(String.class, Any.class,
                                                    Empty.class, Int32Value.class, UserId.class)));
        assertFalse(consistsOfTypes(singleParamCommand().getParameterTypes(),
                                    ImmutableList.of(String.class, Any.class,
                                       Empty.class, Int32Value.class, UserId.class)));
        assertFalse(consistsOfTypes(twoParamCommandAndCtx().getParameterTypes(),
                                    ImmutableList.of(String.class, Any.class,
                                       Empty.class, Int32Value.class, UserId.class)));

    }

    @Test
    @DisplayName("find a matching signature for the method among the predefined set of values")
    void findMatchingSignature() {
        Optional<ScheduleCommandParamSpec> matching =
                findMatching(twoParamCommandAndCtx(),
                             ImmutableList.copyOf(ScheduleCommandParamSpec.values()));
        assertTrue(matching.isPresent());
        assertEquals(ScheduleCommandParamSpec.MESSAGE_AND_CONTEXT, matching.get());
    }

    @Test
    @DisplayName("return `Optional.empty()` if there is no matching signature")
    void returnOptionalEmptyIfNoSignatureMatch() {
        Optional<ScheduleCommandParamSpec> matching =
                findMatching(singleParamCommand(),
                             ImmutableList.copyOf(ScheduleCommandParamSpec.values()));
        assertTrue(!matching.isPresent());
    }

    @Test
    @DisplayName("detect if the first method parameter is a Command message")
    void detectFirstCommandParameter() {
        assertTrue(isFirstParamCommand(singleParamCommand()));
        assertTrue(isFirstParamCommand(twoParamCommandAndCtx()));
        assertFalse(isFirstParamCommand(fiveParamMethodStringAnyEmptyInt32UserId()));
    }
}
