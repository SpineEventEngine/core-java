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
import io.spine.core.Event;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.type.EventEnvelope;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.dispatch.DispatchOutcomeHandler.OutcomeHandler.doNothing;

//TODO:2019-10-18:ysergiichuk: add tests.

/**
 * The holder of an {@code DispatchOutcome} which provides convenient configurable handling for
 * different outcome cases.
 *
 * <p>By default all the handling are configured to {@code do nothing}.
 */
public final class DispatchOutcomeHandler {

    private final DispatchOutcome outcome;

    private OutcomeHandler<Error> errorHandler = doNothing();
    private OutcomeHandler<RejectionEnvelope> rejectionHandler = doNothing();
    private OutcomeHandler<Success> successHandler = doNothing();
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
     * Accepts {@code rejection} {@code handler} case consumer.
     */
    public DispatchOutcomeHandler onRejection(OutcomeHandler<RejectionEnvelope> handler) {
        this.rejectionHandler = checkNotNull(handler);
        return this;
    }

    /**
     * Accepts {@code Success} case handler.
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
     * Accepts {@code Ignored} case consumer.
     */
    public DispatchOutcomeHandler onIgnored(OutcomeHandler<Ignore> handler) {
        this.ignoreHandler = checkNotNull(handler);
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
                    Event rejection = success.getRejection();
                    rejectionHandler.accept(RejectionEnvelope.from(EventEnvelope.of(rejection)));
                } else {
                    successHandler.accept(success);
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
