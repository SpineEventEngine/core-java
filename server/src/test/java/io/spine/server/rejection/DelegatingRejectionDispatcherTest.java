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

package io.spine.server.rejection;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyDeleted;
import io.spine.server.rejection.given.DelegatingRejectionDispatcherTestEnv.EmptyRejectionDispatcherDelegate;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("DelegatingRejectionDispatcher should")
class DelegatingRejectionDispatcherTest {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private EmptyRejectionDispatcherDelegate delegate;
    private DelegatingRejectionDispatcher delegatingDispatcher;
    private RejectionEnvelope rejectionEnvelope;

    @BeforeEach
    void setUp() {
        delegate = new EmptyRejectionDispatcherDelegate();
        delegatingDispatcher = DelegatingRejectionDispatcher.of(delegate);

        Command command = requestFactory.generateCommand();
        Message rejectionMessage =
                EntityAlreadyDeleted.newBuilder()
                                    .setEntityId(Identifier.pack(getClass().getName()))
                                    .build();
        Rejection rejection = Rejections.createRejection(rejectionMessage, command);
        rejectionEnvelope = RejectionEnvelope.of(rejection);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(RejectionDispatcherDelegate.class, delegate)
                .testAllPublicStaticMethods(DelegatingRejectionDispatcher.class);
    }

    @Test
    @DisplayName("return rejection classes of delegate")
    void getDelegateRejectionTypes() {
        assertEquals(delegatingDispatcher.getMessageClasses(),
                     delegate.getRejectionClasses());
    }

    @SuppressWarnings("CheckReturnValue") // Can ignore in this test.
    @Test
    @DisplayName("dispatch rejection")
    void dispatchRejection() {
        delegatingDispatcher.dispatch(rejectionEnvelope);

        assertTrue(delegate.dispatchCalled());
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("delegate `onError`")
    void delegateOnError() {
        delegatingDispatcher.onError(rejectionEnvelope, new RuntimeException(getClass().getName()));

        assertTrue(delegate.onErrorCalled());
    }
}
