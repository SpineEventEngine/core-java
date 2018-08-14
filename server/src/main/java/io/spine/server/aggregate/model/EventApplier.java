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

package io.spine.server.aggregate.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodResult;
import io.spine.server.model.MethodSignature;

import java.lang.reflect.Method;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.model.MethodAccessChecker.forMethod;
import static io.spine.server.model.MethodSignatures.consistsOfSingle;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
public final class EventApplier
        extends AbstractHandlerMethod<Aggregate, EventClass, EventEnvelope, MethodResult<Empty>> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method   the applier method
     * @param signature {@link MethodSignature} which describes the method
     */
    private EventApplier(Method method,
                         MethodSignature<EventEnvelope> signature) {
        super(method, signature);
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }

    static EventApplier from(Method method,
                             MethodSignature<EventEnvelope> signature) {
        return new EventApplier(method, signature);
    }

    static MethodFactory<EventApplier, ?> factory() {
        return Factory.INSTANCE;
    }

    @Override
    protected MethodResult<Empty> toResult(Aggregate target, Object rawMethodOutput) {
        return MethodResult.empty();
    }

    /** The factory for filtering methods that match {@code EventApplier} specification. */
    private static class Factory extends MethodFactory<EventApplier, EventApplierSignature> {

        private static final Factory INSTANCE = new Factory();

        private Factory() {
            super(Apply.class, of(void.class));
        }

        @Override
        public Class<EventApplier> getMethodClass() {
            return EventApplier.class;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPrivate("Event applier method `{}` must be declared `private`.");
        }

        @Override
        protected EventApplier doCreate(Method method, EventApplierSignature acceptor) {
            return from(method, acceptor);
        }

        @Override
        protected Class<EventApplierSignature> getSignatureClass() {
            return EventApplierSignature.class;
        }
    }

    @VisibleForTesting
    @Immutable
    enum EventApplierSignature implements MethodSignature<EventEnvelope> {

        MESSAGE {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfSingle(methodParams, Message.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope envelope) {
                return new Object[] {envelope.getMessage()};
            }
        }
    }
}
