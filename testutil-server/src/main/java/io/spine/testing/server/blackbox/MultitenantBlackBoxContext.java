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
import io.spine.core.TenantId;
import io.spine.server.QueryService;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.client.blackbox.VerifyAcknowledgements;
import io.spine.testing.server.blackbox.verify.state.VerifyState;
import io.spine.testing.server.blackbox.verify.state.VerifyState.VerifyStateByTenant;

/**
 * A black box bounded context for writing integration tests in a multitenant environment.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
@VisibleForTesting
public class MultitenantBlackBoxContext
        extends BlackBoxBoundedContext<MultitenantBlackBoxContext> {

    private final TenantId tenantId;

    /**
     * Creates a new multi-tenant instance.
     */
    MultitenantBlackBoxContext(BlackBoxBuilder builder) {
        super(true,
              builder.buildEnricher(),
              requestFactory(builder.buildTenant()));
        this.tenantId = builder.buildTenant();
    }

    /**
     * Creates a new bounded context with the default configuration.
     */
    public static MultitenantBlackBoxContext newInstance() {
        return newBuilder().build();
    }

    /**
     * Creates a builder for a black box bounded context.
     */
    public static BlackBoxBuilder newBuilder() {
        return new BlackBoxBuilder();
    }

    /**
     * Creates a new {@link io.spine.client.ActorRequestFactory actor request factory} for tests
     * with a provided tenant ID.
     *
     * @param tenantId
     *         an identifier of a tenant that is executing requests in this Bounded Context
     * @return a new request factory instance
     */
    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(MultitenantBlackBoxContext.class, tenantId);
    }

    /**
     * Verifies emitted events by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the events emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyEvents verifier) {
        EmittedEvents events = emittedEvents(tenantId);
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
    public MultitenantBlackBoxContext assertThat(VerifyAcknowledgements verifier) {
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
    public MultitenantBlackBoxContext assertThat(VerifyCommands verifier) {
        EmittedCommands commands = output().emittedCommands();
        verifier.verify(commands);
        return this;
    }

    /**
     * Does the same as {@link #assertThat(VerifyStateByTenant)}, but with a custom tenant ID.
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyState verifier) {
        QueryService queryService = queryService();
        verifier.verify(queryService);
        return this;
    }

    /**
     * Asserts the state of an entity using the {@link #tenantId}.
     *
     * @param verifyByTenant
     *         the function to produce {@link VerifyState} with specific tenant ID
     * @return current instance
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyStateByTenant verifyByTenant) {
        VerifyState verifier = verifyByTenant.apply(tenantId);
        return assertThat(verifier);
    }

    /**
     * Reads all events from the bounded context for the provided tenant.
     */
    private EmittedEvents emittedEvents(TenantId tenantId) {
        TenantAwareRunner tenantAwareRunner = TenantAwareRunner.with(tenantId);
        EmittedEvents events = tenantAwareRunner.evaluate(() -> output().emittedEvents());
        return events;
    }
}
