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

//TODO:2019-10-18:ysergiichuk: add tests.

/**
 * The holder of an {@code DispatchOutcome} which provides convenient configurable handling for
 * different outcome cases.
 *
 * <p>By default all the handling are configured to {@code do nothing}.
 */
public final class DispatchOutcomeHandler {

    private final DispatchOutcome outcome;

    private Consumer<Error> errorConsumer = DispatchOutcomeHandler::doNothing;
    private Consumer<RejectionEnvelope> rejectionConsumer = DispatchOutcomeHandler::doNothing;
    private Consumer<Success> successConsumer = DispatchOutcomeHandler::doNothing;
    private Consumer<Interruption> interruptionConsumer = DispatchOutcomeHandler::doNothing;
    private Consumer<Ignore> ignoreConsumer = DispatchOutcomeHandler::doNothing;

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
     * Accepts {@code Error} case consumer.
     */
    public DispatchOutcomeHandler onError(Consumer<Error> errorConsumer) {
        this.errorConsumer = checkNotNull(errorConsumer);
        return this;
    }

    /**
     * Accepts {@code rejection} {@code success} case consumer.
     *
     * <p>The consumer is called just before the {@code successConsumer}.
     */
    public DispatchOutcomeHandler onRejection(Consumer<RejectionEnvelope> rejectionConsumer) {
        this.rejectionConsumer = checkNotNull(rejectionConsumer);
        return this;
    }

    /**
     * Accepts {@code Success} case consumer.
     */
    public DispatchOutcomeHandler onSuccess(Consumer<Success> successConsumer) {
        this.successConsumer = checkNotNull(successConsumer);
        return this;
    }

    /**
     * Accepts {@code Interruption} case consumer.
     */
    public DispatchOutcomeHandler onInterruption(Consumer<Interruption> interruptionConsumer) {
        this.interruptionConsumer = checkNotNull(interruptionConsumer);
        return this;
    }

    /**
     * Accepts {@code Ignored} case consumer.
     */
    public DispatchOutcomeHandler onIgnored(Consumer<Ignore> ignoreConsumer) {
        this.ignoreConsumer = checkNotNull(ignoreConsumer);
        return this;
    }

    /**
     * Processes the {@code outcome} with the configured consumers and returns the {@code outcome}
     * to the caller.
     *
     * <p>The {@code rejection} outcome is processed along the {@code Success} outcome and both
     * consumers could be invoked during the same processing.
     */
    public DispatchOutcome process() {
        switch (outcome.getResultCase()) {
            case SUCCESS:
                if (outcome.hasRejection()) {
                    Event rejection = outcome.getSuccess()
                                             .getRejection();
                    rejectionConsumer.accept(RejectionEnvelope.from(EventEnvelope.of(rejection)));
                }
                successConsumer.accept(outcome.getSuccess());
                break;
            case ERROR:
                errorConsumer.accept(outcome.getError());
                break;
            case INTERRUPTED:
                interruptionConsumer.accept(outcome.getInterrupted());
                break;
            case IGNORED:
                ignoreConsumer.accept(outcome.getIgnored());
                break;
            case RESULT_NOT_SET:
            default:
                break;
        }
        return outcome;
    }

    private static <T> void doNothing(T t) {
        // do nothing
    }
}
