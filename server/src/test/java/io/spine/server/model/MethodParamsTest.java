/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.spine.core.CommandContext;
import io.spine.core.UserId;
import io.spine.test.model.ModCreateProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.model.MethodParams.firstIsCommand;
import static io.spine.server.model.TypeMatcher.exactly;
import static io.spine.server.model.given.MethodParamsTestEnv.fiveParamMethodStringAnyEmptyInt32UserId;
import static io.spine.server.model.given.MethodParamsTestEnv.singleParamCommand;
import static io.spine.server.model.given.MethodParamsTestEnv.twoParamCommandAndCtx;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`MethodParams` utility should ")
class MethodParamsTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
            .setDefault(Method.class, singleParamCommand())
            .testStaticMethods(MethodParams.class, Visibility.PACKAGE);
    }

    @Test
    @DisplayName("obtain the number of parameters")
    void size() {
        assertThat(MethodParams.of(singleParamCommand()).size())
                .isEqualTo(1);
        assertThat(MethodParams.of(twoParamCommandAndCtx()).size())
                .isEqualTo(2);
        assertThat(MethodParams.of(fiveParamMethodStringAnyEmptyInt32UserId()).size())
                .isEqualTo(5);
    }

    @Test
    @DisplayName("detect that a method has exactly one parameter of an expected type")
    void detectSingleParam() {
        assertTrue(MethodParams.of(singleParamCommand())
                               .is(exactly(ModCreateProject.class)));
        assertFalse(MethodParams.of(twoParamCommandAndCtx())
                                .is(exactly(ModCreateProject.class)));
        assertFalse(MethodParams.of(fiveParamMethodStringAnyEmptyInt32UserId())
                                .is(exactly(ModCreateProject.class)));
    }

    @Test
    @DisplayName("detect that a method has exactly two parameters of expected types")
    void detectTwoParams() {
        assertTrue(MethodParams.of(twoParamCommandAndCtx())
                               .match(exactly(ModCreateProject.class),
                                      exactly(CommandContext.class)));
        assertFalse(MethodParams.of(singleParamCommand())
                                .match(exactly(ModCreateProject.class),
                                       exactly(CommandContext.class)));
        assertFalse(MethodParams.of(fiveParamMethodStringAnyEmptyInt32UserId())
                                .match(exactly(ModCreateProject.class),
                                       exactly(CommandContext.class)));
    }

    @Test
    @DisplayName("detect that a method has lots of parameters of expected types")
    void detectLotsOfParams() {
        assertTrue(MethodParams.of(fiveParamMethodStringAnyEmptyInt32UserId())
                               .are(String.class, Any.class, Empty.class,
                                    Int32Value.class, UserId.class)
        );
        assertFalse(MethodParams.of(singleParamCommand())
                                .are(String.class, Any.class, Empty.class));
        assertFalse(MethodParams.of(twoParamCommandAndCtx())
                                .are(String.class, Any.class, Empty.class));
    }

    @Test
    @DisplayName("detect if the first method parameter is a Command message")
    void detectFirstCommandParameter() {
        assertTrue(firstIsCommand(singleParamCommand()));
        assertTrue(firstIsCommand(twoParamCommandAndCtx()));
        assertFalse(firstIsCommand(fiveParamMethodStringAnyEmptyInt32UserId()));
    }

    @Test
    @DisplayName("provide `equals()` and `hashCode()`")
    void equality() {
        MethodParams oneParamMethod = MethodParams.of(singleParamCommand());
        new EqualsTester().addEqualityGroup(oneParamMethod, oneParamMethod,
                                            MethodParams.of(singleParamCommand()))
                          .addEqualityGroup(firstIsCommand(twoParamCommandAndCtx()))
                          .addEqualityGroup(
                                  MethodParams.of(fiveParamMethodStringAnyEmptyInt32UserId()))
                          .testEquals();
    }
}
