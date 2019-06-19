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
package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.command.DispatchCommand;
import io.spine.server.entity.AutoIncrement;
import io.spine.server.entity.CommandDispatchingPhase;
import io.spine.server.entity.EventDispatchingPhase;
import io.spine.server.entity.Phase;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.VersionIncrement;
import io.spine.server.event.EventDispatch;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;

import java.util.List;

import static com.google.common.base.Throwables.getRootCause;

/**
 * A transaction, within which {@linkplain ProcessManager ProcessManager instances} are modified.
 *
 * @param <I>
 *         the type of process manager IDs
 * @param <S>
 *         the type of process manager state
 * @param <B>
 *         the type of a {@code ValidatingBuilder} for the process manager state
 */
@Internal
public class PmTransaction<I,
                           S extends Message,
                           B extends ValidatingBuilder<S>>
        extends Transaction<I, ProcessManager<I, S, B>, S, B> {

    /**
     * The lifecycle rules which will be applied to the process manager instance.
     */
    private final LifecycleRules lifecycleRules;

    private PmTransaction(ProcessManager<I, S, B> processManager, LifecycleRules lifecycleRules) {
        super(processManager);
        this.lifecycleRules = lifecycleRules;
    }

    private PmTransaction(ProcessManager<I, S, B> processManager,
                          S state,
                          Version version,
                          LifecycleRules lifecycleRules) {
        super(processManager, state, version);
        this.lifecycleRules = lifecycleRules;
    }

    @VisibleForTesting
    protected PmTransaction(ProcessManager<I, S, B> processManager) {
        this(processManager, new LifecycleRules());
    }

    @VisibleForTesting
    protected PmTransaction(ProcessManager<I, S, B> processManager, S state, Version version) {
        this(processManager, state, version, new LifecycleRules());
    }

    /**
     * Executes the given command dispatch for the current entity in transaction.
     *
     * @param dispatch
     *         the {@code DispatchCommand} task
     * @return the events generated from the command dispatch
     * @see ProcessManager#dispatchCommand(CommandEnvelope)
     */
    List<Event> perform(DispatchCommand<I> dispatch) {
        VersionIncrement versionIncrement = createVersionIncrement();
        Phase<I, List<Event>> phase = new CommandDispatchingPhase<>(dispatch, versionIncrement);
        List<Event> events = doPropagate(phase);
        return events;
    }

    /**
     * Dispatches the given event to the current entity in transaction.
     *
     * @param event
     *         the event to dispatch
     * @return the events generated from the event dispatch
     * @see ProcessManager#dispatchEvent(EventEnvelope)
     */
    List<Event> dispatchEvent(EventEnvelope event) {
        VersionIncrement versionIncrement = createVersionIncrement();
        Phase<I, List<Event>> phase = new EventDispatchingPhase<>(
                new EventDispatch<>(this::dispatch, entity(), event),
                versionIncrement
        );
        List<Event> events = doPropagate(phase);
        return events;
    }

    /**
     * Propagates the phase and updates the process lifecycle after success.
     */
    private List<Event> doPropagate(Phase<I, List<Event>> phase) {
        List<Event> events = propagate(phase);
        updateLifecycle(events);
        return events;
    }

    /**
     * Updates and commits the process manager lifecycle flags after a rejection is thrown.
     */
    @Override
    protected void beforeRollback(Throwable cause) {
        super.beforeRollback(cause);
        Throwable rootCause = getRootCause(cause);
        if (rootCause instanceof ThrowableMessage) {
            updateLifecycle((ThrowableMessage) rootCause);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is overridden to expose itself to repositories, state builders,
     * and test utilities.
     */
    @Override
    protected final void commit() {
        super.commit();
    }

    /**
     * Creates a new transaction for a given {@code ProcessManager}.
     *
     * @param processManager
     *         the {@code ProcessManager} instance to start the transaction for
     * @param lifecycleRules
     *         the lifecycle rules to apply to the entity
     * @return the new transaction instance
     */
    static <I,
            S extends Message,
            B extends ValidatingBuilder<S>>
    PmTransaction<I, S, B> start(ProcessManager<I, S, B> processManager, LifecycleRules lifecycleRules) {
        PmTransaction<I, S, B> tx = new PmTransaction<>(processManager, lifecycleRules);
        return tx;
    }

    private List<Event> dispatch(ProcessManager<I, S, B> processManager, EventEnvelope event) {
        return processManager.dispatchEvent(event);
    }

    /**
     * Updates the process lifecycle based on a successful phase propagation result.
     */
    private void updateLifecycle(Iterable<Event> events) {
        if (lifecycleRules.shouldArchiveOn(events)) {
            setArchived(true);
        }
        if (lifecycleRules.shouldDeleteOn(events)) {
            setDeleted(true);
        }
    }

    /**
     * Updates the process lifecycle after a rejection is thrown.
     *
     * <p>Manually commits the changes as they are not going to be committed normally.
     */
    private void updateLifecycle(ThrowableMessage rejection) {
        if (lifecycleRules.shouldArchiveOn(rejection)) {
            setArchived(true);
        }
        if (lifecycleRules.shouldDeleteOn(rejection)) {
            setDeleted(true);
        }
        commitAttributeChanges();
    }

    private VersionIncrement createVersionIncrement() {
        return new AutoIncrement(this);
    }
}
