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

package io.spine.server.event.model;

import io.spine.core.Subscribe;
import io.spine.server.event.model.given.subscriber.InvalidSubscriber;
import io.spine.server.event.model.given.subscriber.ValidSubscriber;
import io.spine.server.model.MethodSignatureTest;

import java.lang.reflect.Method;
import java.util.stream.Stream;

class EventSubscriberSignatureTest extends MethodSignatureTest<SubscriberSignature> {

    @Override
    protected Stream<Method> validMethods() {
        return methodsAnnotatedWith(Subscribe.class, ValidSubscriber.class).stream();
    }

    @Override
    protected Stream<Method> invalidMethods() {
        return methodsAnnotatedWith(Subscribe.class, InvalidSubscriber.class).stream();
    }

    @Override
    protected SubscriberSignature signature() {
        return new SubscriberSignature();
    }
}
