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

package io.spine.system.server;

import io.spine.annotation.Internal;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.Enricher;
import io.spine.server.event.EventBus;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of {@link BoundedContext} used for the System domain.
 *
 * <p>Orchestrates the system entities that serve the goal of monitoring, auditing, and debugging
 * the domain-specific entities.
 *
 * <p>Each {@link BoundedContext} has an associated {@code SystemBoundedContext}.
 * The system entities describe the meta information about the domain entities of the associated
 * {@link BoundedContext}. A system bounded context does NOT have an associated bounded
 * context.
 *
 * @apiNote The framework users should not access a System Bounded Context directly.
 * Programmers extending the framework should see {@link SystemGateway} for
 * the front-facing API of the System bounded context.
 *
 * @author Dmytro Dashenkov
 * @see SystemGateway
 * @see BoundedContext
 */
@Internal
public final class SystemContext extends BoundedContext {

    private @MonotonicNonNull SystemBus ownBus;

    private SystemContext(BoundedContextBuilder builder) {
        super(builder);
    }

    /**
     * Creates a new instance of {@code SystemBoundedContext} from the given
     * {@link BoundedContextBuilder}.
     *
     * @param builder the configuration of the instance to create
     * @return new {@code SystemBoundedContext}
     */
    public static SystemContext newInstance(BoundedContextBuilder builder) {
        CommandLifecycleRepository repository = new CommandLifecycleRepository();
        BoundedContextBuilder preparedBuilder = prepareEnricher(builder, repository);
        SystemContext result = new SystemContext(preparedBuilder);
        result.init();
        result.registerRepositories(repository);
        return result;
    }

    private static BoundedContextBuilder prepareEnricher(BoundedContextBuilder builder,
                                                         CommandLifecycleRepository repository) {
        EventBus.Builder busBuilder = builder.getEventBus()
                                             .orElseGet(EventBus::newBuilder);
        Enricher enricher = SystemEnricher.create(repository);
        EventBus.Builder builderWithEnricher = busBuilder.setEnricher(enricher);
        return builder.setEventBus(builderWithEnricher);
    }

    private void init() {
        this.ownBus = SystemBus.newInstance(this);
    }

    private void registerRepositories(CommandLifecycleRepository commandLifecycle) {
        register(commandLifecycle);
        register(new EntityHistoryRepository());

        register(new ScheduledCommandRepository());
        register(new MirrorRepository());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Since a system bounded context does not have an associated system bounded context, returns
     * a {@link NoOpSystemGateway} instance.
     */
    @Override
    public NoOpSystemGateway getSystemGateway() {
        return NoOpSystemGateway.INSTANCE;
    }

    @Override
    public SystemBus getSystemBus() {
        return checkNotNull(ownBus);
    }
}
