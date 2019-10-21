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

package io.spine.server.dispatch;

import io.spine.base.Error;
import io.spine.core.Command;
import io.spine.core.Event;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.dispatch.DispatchOutcomeHandler.OutcomeHandler.doNothing;

/**
 * The holder of an {@code DispatchOutcome} which provides convenient configurable handling for
 * different outcome cases.
 *
 * <p>By default all the handling are configured to {@code do nothing}.
 */
public final class DispatchOutcomeHandler {

    private final DispatchOutcome outcome;

    private OutcomeHandler<Error> errorHandler = doNothing();
    private OutcomeHandler<Success> successHandler = doNothing();
    private OutcomeHandler<List<Command>> producedCommandsHandler = doNothing();
    private OutcomeHandler<List<Event>> producedEventsHandler = doNothing();
    private OutcomeHandler<Event> rejectionHandler = doNothing();
    private OutcomeHandler<Interruption> interruptionHandler = doNothing();
    private OutcomeHandler<Ignore> ignoreHandler = doNothing();

    private DispatchOutcomeHandler(DispatchOutcome outcome) {
        this.outcome = outcome;
    }

    /**
     * Creates instance for the passed outcome.
     */
    public static DispatchOutcomeHandler from(DispatchOutcome outcome) {
        checkNotNull(outcome);
        return new DispatchOutcomeHandler(outcome);
    }

    /**
     * Accepts {@code Error} case handler.
     */
    public DispatchOutcomeHandler onError(OutcomeHandler<Error> handler) {
        this.errorHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Accepts {@code rejection} {@code handler} case handler.
     */
    public DispatchOutcomeHandler onRejection(OutcomeHandler<Event> handler) {
        this.rejectionHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Accepts {@code Success} case handler.
     *
     * <p>The {@code rejection} success case is handled using a separate
     * {@link #onRejection(OutcomeHandler)} handler.
     */
    public DispatchOutcomeHandler onSuccess(OutcomeHandler<Success> handler) {
        this.successHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Accepts {@code Interruption} case handler.
     */
    public DispatchOutcomeHandler onInterruption(OutcomeHandler<Interruption> handler) {
        this.interruptionHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Accepts {@code Ignored} case handler.
     */
    public DispatchOutcomeHandler onIgnored(OutcomeHandler<Ignore> handler) {
        this.ignoreHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Accepts {@code ProducedCommands} success case handler.
     */
    public DispatchOutcomeHandler onCommands(OutcomeHandler<List<Command>> handler) {
        this.producedCommandsHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Accepts {@code ProducedEvents} success case handler.
     */
    public DispatchOutcomeHandler onEvents(OutcomeHandler<List<Event>> handler) {
        this.producedEventsHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Processes the {@code outcome} with the configured handlers and returns the {@code outcome}
     * to the caller.
     */
    public DispatchOutcome process() {
        switch (outcome.getResultCase()) {
            case SUCCESS:
                Success success = outcome.getSuccess();
                if (outcome.hasRejection()) {
                    rejectionHandler.accept(success.getRejection());
                } else {
                    handleSuccess(success);
                }
                break;
            case ERROR:
                errorHandler.accept(outcome.getError());
                break;
            case INTERRUPTED:
                interruptionHandler.accept(outcome.getInterrupted());
                break;
            case IGNORED:
                ignoreHandler.accept(outcome.getIgnored());
                break;
            case RESULT_NOT_SET:
            default:
                break;
        }
        return outcome;
    }

    private void handleSuccess(Success success) {
        switch (success.getExhaustCase()) {
            case PRODUCED_EVENTS:
                producedEventsHandler.accept(success.getProducedEvents()
                                                    .getEventList());
                break;
            case PRODUCED_COMMANDS:
                producedCommandsHandler.accept(success.getProducedCommands()
                                                      .getCommandList());
                break;
            case REJECTION:
            case EXHAUST_NOT_SET:
            default:
                break;
        }
        successHandler.accept(success);
    }

    /**
     * A {@code DispatchOutcome} handler.
     */
    @FunctionalInterface
    public interface OutcomeHandler<T> extends Consumer<T> {

        OutcomeHandler DO_NOTHING = o -> {
        };

        /**
         * Creates a new no-op handler.
         */
        @SuppressWarnings("unchecked")
        static <T> OutcomeHandler<T> doNothing() {
            return (OutcomeHandler<T>) DO_NOTHING;
        }
    }
}
