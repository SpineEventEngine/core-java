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

import com.google.common.base.Joiner;
import io.spine.server.model.HandlerMethod;

/**
 * Thrown for {@linkplain io.spine.server.model.HandlerMethod handler method} in case
 * its {@link HandlerMethod#rawMethod() wrapped method} does not match
 * {@linkplain MethodSignature method signature}, set for the handler.
 *
 * @author Alex Tymchenko
 */
public class SignatureMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    SignatureMismatchException(Iterable<SignatureMismatch> mismatches) {
        super(formatMsg(mismatches));
    }

    private static String formatMsg(Iterable<SignatureMismatch> mismatches) {
        return "Error declaring a method. Mismatches: " + Joiner.on(", ")
                                                                .join(mismatches);
    }
}
