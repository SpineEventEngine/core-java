/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.spine.core.CommandContext;
import io.spine.core.UserId;
import io.spine.server.model.declare.given.MethodParamsTestEnv.DeleteEntityParamSpec;
import io.spine.system.server.DeleteEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static io.spine.server.model.declare.MethodParams.consistsOfSingle;
import static io.spine.server.model.declare.MethodParams.consistsOfTwo;
import static io.spine.server.model.declare.MethodParams.consistsOfTypes;
import static io.spine.server.model.declare.MethodParams.findMatching;
import static io.spine.server.model.declare.MethodParams.isFirstParamCommand;
import static io.spine.server.model.declare.given.MethodParamsTestEnv.fiveParamMethodStringAnyEmptyInt32UserId;
import static io.spine.server.model.declare.given.MethodParamsTestEnv.singleParamDeleteEntity;
import static io.spine.server.model.declare.given.MethodParamsTestEnv.twoParamDeleteEntityAndCtx;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("WeakerAccess")   // JUnit test methods are `public` as per the library contract
@DisplayName("`MethodParams` utility should ")
public class MethodParamsTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveParameterlessCtor() {
        assertHasPrivateParameterlessCtor(MethodParams.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(MethodParams.class);
    }

    @Test
    @DisplayName("detect that a method has exactly one parameter of an expected type")
    public void detectSingleParam() {
        assertTrue(consistsOfSingle(singleParamDeleteEntity().getParameterTypes(),
                                    DeleteEntity.class));
        assertFalse(consistsOfSingle(twoParamDeleteEntityAndCtx().getParameterTypes(),
                                     DeleteEntity.class));
        assertFalse(consistsOfSingle(fiveParamMethodStringAnyEmptyInt32UserId().getParameterTypes(),
                                     DeleteEntity.class));

    }

    @Test
    @DisplayName("detect that a method has exactly two parameters of expected types")
    public void detectTwoParams() {
        assertTrue(consistsOfTwo(twoParamDeleteEntityAndCtx().getParameterTypes(),
                                 DeleteEntity.class, CommandContext.class));
        assertFalse(consistsOfTwo(singleParamDeleteEntity().getParameterTypes(),
                                  DeleteEntity.class, CommandContext.class));
        assertFalse(consistsOfTwo(fiveParamMethodStringAnyEmptyInt32UserId().getParameterTypes(),
                                  DeleteEntity.class, CommandContext.class));

    }

    @Test
    @DisplayName("detect that a method has lots of parameters of expected types")
    public void detectLotsOfParams() {
        assertTrue(consistsOfTypes(fiveParamMethodStringAnyEmptyInt32UserId().getParameterTypes(),
                                   of(String.class, Any.class,
                                      Empty.class, Int32Value.class, UserId.class)));
        assertFalse(consistsOfTypes(singleParamDeleteEntity().getParameterTypes(),
                                    of(String.class, Any.class,
                                       Empty.class, Int32Value.class, UserId.class)));
        assertFalse(consistsOfTypes(twoParamDeleteEntityAndCtx().getParameterTypes(),
                                    of(String.class, Any.class,
                                       Empty.class, Int32Value.class, UserId.class)));

    }

    @Test
    @DisplayName("find a matching signature for the method among the predefined set of values")
    public void findMatchingSignature() {
        Optional<DeleteEntityParamSpec> matching = findMatching(twoParamDeleteEntityAndCtx(),
                                                                DeleteEntityParamSpec.class);
        assertTrue(matching.isPresent());
        assertEquals(DeleteEntityParamSpec.MESSAGE_AND_CONTEXT, matching.get());
    }

    @Test
    @DisplayName("return `Optional.empty()` if there is no matching signature")
    public void returnOptionalEmptyIfNoSignatureMatch() {
        Optional<DeleteEntityParamSpec> matching = findMatching(singleParamDeleteEntity(),
                                                                DeleteEntityParamSpec.class);
        assertTrue(!matching.isPresent());
    }

    @Test
    @DisplayName("detect if the first method parameter is a Command message")
    public void detectFirstCommandParameter() {
        assertTrue(isFirstParamCommand(singleParamDeleteEntity()));
        assertTrue(isFirstParamCommand(twoParamDeleteEntityAndCtx()));
        assertFalse(isFirstParamCommand(fiveParamMethodStringAnyEmptyInt32UserId()));
    }
}
