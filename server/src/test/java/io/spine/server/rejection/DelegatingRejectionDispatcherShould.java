/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyDeleted;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DelegatingRejectionDispatcherShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private EmptyRejectionDispatcherDelegate delegate;
    private DelegatingRejectionDispatcher delegatingDispatcher;
    private RejectionEnvelope rejectionEnvelope;

    @Before
    public void setUp() {
        delegate = new EmptyRejectionDispatcherDelegate();
        delegatingDispatcher = DelegatingRejectionDispatcher.of(delegate);

        final Command command = requestFactory.generateCommand();
        final Message rejectionMessage =
                EntityAlreadyDeleted.newBuilder()
                                    .setEntityId(Identifier.pack(getClass().getName()))
                                    .build();
        final Rejection rejection = Rejections.createRejection(rejectionMessage, command);
        rejectionEnvelope = RejectionEnvelope.of(rejection);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(RejectionDispatcherDelegate.class, delegate)
                .testAllPublicStaticMethods(DelegatingRejectionDispatcher.class);
    }

    @Test
    public void return_rejection_classes_of_the_delegate() {
        assertEquals(delegatingDispatcher.getMessageClasses(),
                     delegate.getRejectionClasses());
    }

    @Test
    public void dispatch_rejection() {
        delegatingDispatcher.dispatch(rejectionEnvelope);

        assertTrue(delegate.dispatchCalled());
    }

    @Test
    public void delegate_onError() {
        delegatingDispatcher.onError(rejectionEnvelope, new RuntimeException(getClass().getName()));

        assertTrue(delegate.onErrorCalled());
    }

    /*
     * Test Environment
     ****************************/

    private static final class EmptyRejectionDispatcherDelegate
            implements RejectionDispatcherDelegate<String> {

        private boolean onErrorCalled;
        private boolean dispatchCalled;

        @Override
        public Set<RejectionClass> getRejectionClasses() {
            return ImmutableSet.of(RejectionClass.of(EntityAlreadyArchived.class));
        }

        @Override
        public Set<String> dispatchRejection(RejectionEnvelope envelope) {
            dispatchCalled = true;
            return ImmutableSet.of(toString());
        }

        @Override
        public void onError(RejectionEnvelope envelope, RuntimeException exception) {
            onErrorCalled = true;
        }

        private boolean onErrorCalled() {
            return onErrorCalled;
        }

        private boolean dispatchCalled() {
            return dispatchCalled;
        }
    }
}
