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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.server.event.Enricher;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.client.blackbox.VerifyAcknowledgements;
import io.spine.testing.server.blackbox.verify.state.VerifyState;

/**
 * A black box bounded context for writing integration tests in a single tenant environment.
 */
@VisibleForTesting
public class SingletenantBlackBoxContext
        extends BlackBoxBoundedContext<SingletenantBlackBoxContext> {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(SingletenantBlackBoxContext.class);

    private SingletenantBlackBoxContext(Enricher enricher) {
        super(false, enricher, requestFactory);
    }

    /**
     * Creates a new bounded context with the default configuration.
     */
    public static SingletenantBlackBoxContext newInstance() {
        Enricher enricher = Enricher.newBuilder()
                                    .build();
        return new SingletenantBlackBoxContext(enricher);
    }

    /**
     * Creates a new bounded context with the specified event enricher.
     */
    public static SingletenantBlackBoxContext newInstance(Enricher enricher) {
        return new SingletenantBlackBoxContext(enricher);
    }

    /**
     * Verifies emitted events by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the events emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public SingletenantBlackBoxContext assertThat(VerifyEvents verifier) {
        EmittedEvents events = output().emittedEvents();
        verifier.verify(events);
        return this;
    }

    /**
     * Executes the provided verifier, which throws an assertion error in case of
     * unexpected results.
     *
     * @param verifier
     *         a verifier that checks the acknowledgements in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public SingletenantBlackBoxContext assertThat(VerifyAcknowledgements verifier) {
        Acknowledgements acks = output().commandAcks();
        verifier.verify(acks);
        return this;
    }

    /**
     * Verifies emitted commands by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the commands emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public SingletenantBlackBoxContext assertThat(VerifyCommands verifier) {
        EmittedCommands commands = output().emittedCommands();
        verifier.verify(commands);
        return this;
    }

    /**
     * Verifies states of entities using the verifier.
     *
     * @param verifier
     *         a verifier of entity states
     * @return current instance
     */
    @CanIgnoreReturnValue
    public SingletenantBlackBoxContext assertThat(VerifyState verifier) {
        verifier.verify(boundedContext(), requestFactory.query());
        return this;
    }
}
