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

package io.spine.server.event.model;

import io.spine.server.model.declare.MethodSignatureTest;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findContextArgumentMismatch;
import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findFirstArgumentMismatch;
import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findMessageAndCmdContext;
import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findMessageAndCommand;
import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findMessageAndCommandMessageAndContext;
import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findMessageAndContext;
import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findMessageOnly;
import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findReturnsValue;
import static io.spine.server.event.model.given.SubscriberSignatureTestEnv.findThrowsUnchckedException;

class SubscriberSignatureTest extends MethodSignatureTest<SubscriberSignature> {

    @Override
    protected Stream<Method> validMethods() {
        return Stream.of(findMessageOnly(),
                         findMessageAndContext(),
                         findMessageAndCommand(),
                         findMessageAndCmdContext(),
                         findMessageAndCommandMessageAndContext());
    }

    @Override
    protected Stream<Method> invalidMethods() {
        return Stream.of(findReturnsValue(),
                         findContextArgumentMismatch(),
                         findFirstArgumentMismatch(),
                         findThrowsUnchckedException());
    }

    @Override
    protected SubscriberSignature signature() {
        return new SubscriberSignature();
    }
}
