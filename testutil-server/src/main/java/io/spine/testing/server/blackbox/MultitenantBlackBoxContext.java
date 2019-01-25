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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import io.spine.core.Command;
import io.spine.core.Commands;
import io.spine.core.TenantId;
import io.spine.server.event.enrich.Enricher;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.testing.client.TestActorRequestFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * Test fixture for multi-tenant Bounded Contexts.
 */
@VisibleForTesting
public final class MultitenantBlackBoxContext
        extends BlackBoxBoundedContext<MultitenantBlackBoxContext> {

    private TenantId tenantId;

    /**
     * Creates a new multi-tenant instance.
     */
    MultitenantBlackBoxContext(Enricher enricher) {
        super(true, enricher);
    }

    /**
     * Switches the bounded context to operate on behalf of the specified tenant.
     *
     * @param tenant
     *         new tenant ID
     * @return current instance
     */
    MultitenantBlackBoxContext withTenant(TenantId tenant) {
        this.tenantId = tenant;
        return this;
    }

    @Override
    protected TestActorRequestFactory requestFactory() {
        return requestFactory(tenantId());
    }

    @Override
    protected EmittedCommands emittedCommands(CommandMemoizingTap commandTap) {
        List<Command> allCommands = commandTap.commands();
        List<Command> tenantCommands = allCommands.stream()
                                                  .filter(new MatchesTenant(tenantId))
                                                  .collect(Collectors.toList());
        return new EmittedCommands(tenantCommands);
    }

    @Override
    protected EmittedEvents emittedEvents() {
        TenantAwareRunner tenantAwareRunner = TenantAwareRunner.with(tenantId);
        EmittedEvents events = tenantAwareRunner.evaluate(super::emittedEvents);
        return events;
    }

    @Override
    protected <D> @Nullable D readOperation(Supplier<D> supplier) {
        TenantAwareRunner tenantAwareRunner = TenantAwareRunner.with(tenantId);
        D result = tenantAwareRunner.evaluate(() -> super.readOperation(supplier));
        return result;
    }

    private TenantId tenantId() {
        checkState(tenantId != null,
                   "Set a tenant ID before calling receive and assert methods");
        return tenantId;
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
     * A predicate to match commands with a {@linkplain #tenantToMatch tenant ID}.
     */
    private static class MatchesTenant implements Predicate<Command> {

        private final TenantId tenantToMatch;

        private MatchesTenant(TenantId tenantToMatch) {
            this.tenantToMatch = tenantToMatch;
        }

        @Override
        public boolean test(Command command) {
            TenantId commandTenant = Commands.getTenantId(command);
            return commandTenant.equals(tenantToMatch);
        }
    }
}
