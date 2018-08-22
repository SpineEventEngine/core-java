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

package io.spine.testing.client.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.RejectionClass;

/**
 * An abstract verifier of Bounded Context acknowledgements. Its implementations throw assertion
 * errors if the acknowledgements observed in the Bounded Context do not meet the verifier criterion.
 *
 * <p>Contains static factory methods for creating acknowledgement verifiers, checking that
 * commands were acknowledged, responded with rejections, and errors.
 *
 * <p>Allows combining verifiers using {@link #and(VerifyAcknowledgements) and()} or factory
 * method shortcuts: {@code ackedWithoutErrors().and(ackedWithRejection(rej))} can be simplified
 * to {@code ackedWithoutErrors().withRejection(rej)}.
 *
 * @author Mykhailo Drachuk
 */
@SuppressWarnings("ClassWithTooManyMethods")
@VisibleForTesting
public abstract class VerifyAcknowledgements {

    /**
     * Executes the acknowledgement verifier throwing an assertion error if the
     * data does not match the rule..
     *
     * @param acks acknowledgements of handling commands by the Bounded Context
     */
    public abstract void verify(Acknowledgements acks);

    /**
     * Verifies that Bounded Context responded with a specified number of acknowledgements.
     *
     * @param expectedCount an expected amount of acknowledgements observed in Bounded Context
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements acked(Count expectedCount) {
        return new CountVerify(expectedCount);
    }

    /*
     * Factory methods for verifying acks with errors.
     ******************************************************************************/

    /**
     * Verifies that the command handling did not respond with {@link Error error}.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithoutErrors() {
        return new ErrorAbsenceVerify();
    }

    /**
     * Verifies that a command or an event was handled responding with some {@link Error error}.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithErrors() {
        return new ErrorPresenceVerify();
    }

    /**
     * Verifies that a command or an event was handled responding with specified number of
     * {@link Error errors}.
     *
     * @param expectedCount an amount of errors that are expected to be observed
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithErrors(Count expectedCount) {
        return new ErrorCountVerify(expectedCount);
    }

    /**
     * Verifies that a command or an event was handled responding with an error matching a provided
     * {@link ErrorCriterion error criterion}.
     *
     * @param criterion an error criterion specifying which kind of error should be a part
     *                  of acknowledgement
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithErrors(ErrorCriterion criterion) {
        return new SpecificErrorPresenceVerify(criterion);
    }

    /**
     * Verifies that a command or an event was handled responding with an error matching a provided
     * {@link ErrorCriterion error criterion}.
     *
     * @param criterion     an error criterion specifying which kind of error should be a part
     *                      of acknowledgement
     * @param expectedCount an amount of errors that are expected to match the criterion
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithErrors(ErrorCriterion criterion,
                                                         Count expectedCount) {
        return new SpecificErrorCountVerify(criterion, expectedCount);
    }

    /*
     * Factory methods for verifying acks with rejections.
     ******************************************************************************/

    /**
     * Verifies that a command handling did not respond with any rejections.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithoutRejections() {
        return new RejectionAbsenceVerify();
    }

    /**
     * Verifies that a command or an event was handled with a rejection.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithRejections() {
        return new RejectionPresenceVerify();
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection of the provided
     * type.
     *
     * @param type rejection type in a form of message class
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithRejections(Class<? extends Message> type) {
        RejectionClass rejectionClass = RejectionClass.of(type);
        return ackedWithRejections(rejectionClass);
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection
     * of the provided type.
     *
     * @param type rejection type in a form of {@link RejectionClass RejectionClass}
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithRejections(RejectionClass type) {
        return new RejectionOfTypePresenceVerify(type);
    }

    /**
     * Verifies that a command or an event was handled responding with rejection matching the
     * provided predicate.
     *
     * @param type      a type of a domain rejection specified by a message class
     * @param predicate a predicate filtering the domain rejections
     * @param <T>       a domain rejection type
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static <T extends Message> VerifyAcknowledgements
    ackedWithRejections(Class<T> type, RejectionCriterion<T> predicate) {
        return new SpecificRejectionPresenceVerify<>(type, predicate);
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection specified
     * amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithRejections(Count expectedCount) {
        return new RejectionCountVerify(expectedCount);
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection of a provided
     * type specified amount of times.
     *
     * @param type          rejection type in a form of message class
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithRejections(Class<? extends Message> type,
                                                             Count expectedCount) {
        RejectionClass rejectionClass = RejectionClass.of(type);
        return ackedWithRejections(rejectionClass, expectedCount);
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection of a provided
     * type specified amount of times.
     *
     * @param type          rejection type in a form of {@link RejectionClass RejectionClass}
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static VerifyAcknowledgements ackedWithRejections(RejectionClass type,
                                                             Count expectedCount) {
        return new RejectionOfTypeCountVerify(type, expectedCount);
    }

    /**
     * Verifies that a command or an event was handled responding with a provided domain rejection
     * specified amount of times.
     *
     * @param <T>           a domain rejection type
     * @param type          a type of a domain rejection specified by a message class
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param criterion     a criterion filtering domain rejections
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public static <T extends Message> VerifyAcknowledgements
    ackedWithRejections(Class<T> type, Count expectedCount, RejectionCriterion<T> criterion) {
        return new SpecificRejectionCountVerify<>(type, expectedCount, criterion);
    }

    /*
     * Verifier combination shortcuts.
     ******************************************************************************/

    /**
     * Combines current verifier with a provided verifier, making them execute sequentially.
     *
     * @param otherVerifier a verifier executed after the current verifier
     * @return a verifier that executes both current and provided assertions
     */
    public VerifyAcknowledgements and(VerifyAcknowledgements otherVerifier) {
        return CombinationVerify.of(this, otherVerifier);
    }

    /**
     * Creates a new verifier adding a check to not contain any {@link Error errors}.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withoutErrors() {
        VerifyAcknowledgements noErrors = ackedWithoutErrors();
        return this.and(noErrors);
    }

    /**
     * Creates a new verifier adding a check to contain at least one {@link Error error}.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withErrors() {
        VerifyAcknowledgements withError = ackedWithErrors();
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain specified amount of {@link Error errors}.
     *
     * @param expectedCount an amount of errors that are expected to be observed in Bounded Context
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withErrors(Count expectedCount) {
        VerifyAcknowledgements withError = ackedWithErrors(expectedCount);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain an {@link Error error} that matches
     * the criterion.
     *
     * @param criterion an error criterion specifying which kind of error should be a part
     *                  of acknowledgement
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withErrors(ErrorCriterion criterion) {
        VerifyAcknowledgements withError = ackedWithErrors(criterion);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain an {@link Error error} that matches
     * the criterion.
     *
     * @param criterion     an error criterion specifying which kind of error should be a part
     *                      of acknowledgement
     * @param expectedCount an amount of errors that are expected to match the criterion
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withErrors(ErrorCriterion criterion, Count expectedCount) {
        VerifyAcknowledgements withError = ackedWithErrors(criterion, expectedCount);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to not contain any {@link Error errors} or rejections.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withoutErrorsOrRejections() {
        VerifyAcknowledgements noRejections = ackedWithoutRejections();
        VerifyAcknowledgements noErrors = ackedWithoutErrors();
        return this.and(noErrors.and(noRejections));
    }

    /**
     * Creates a new verifier adding a check to not contain any rejections.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withoutRejections() {
        VerifyAcknowledgements noRejections = ackedWithoutRejections();
        return this.and(noRejections);
    }

    /**
     * Creates a new verifier adding a check to contain some rejection.
     *
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withRejections() {
        VerifyAcknowledgements someRejection = ackedWithRejections();
        return this.and(someRejection);
    }

    /**
     * Creates a new verifier adding a check to contain a rejection of a
     * type specified by {@code class}.
     *
     * @param type a type of a domain rejection specified by message class
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withRejections(Class<? extends Message> type) {
        VerifyAcknowledgements rejectedType = ackedWithRejections(type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a rejection of a
     * type specified by a {@link RejectionClass rejection class}.
     *
     * @param type a type of a domain rejection specified by a {@link RejectionClass}
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withRejections(RejectionClass type) {
        VerifyAcknowledgements rejectedType = ackedWithRejections(type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a rejection of a
     * type specified by a {@link RejectionClass rejection class} specified amount of times.
     *
     * @param type          rejection type in a form of message class
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withRejections(Class<? extends Message> type,
                                                 Count expectedCount) {
        VerifyAcknowledgements rejectedType = ackedWithRejections(type, expectedCount);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a rejection of a
     * type specified by a {@link RejectionClass rejection class} specified amount of times.
     *
     * @param type          rejection type in a form of {@link RejectionClass RejectionClass}
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public VerifyAcknowledgements withRejections(RejectionClass type, Count expectedCount) {
        VerifyAcknowledgements rejectedType = ackedWithRejections(type, expectedCount);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a provided domain rejection.
     *
     * @param type      a type of a domain rejection specified by a {@link RejectionClass}
     * @param predicate a predicate filtering domain rejections
     * @param <T>       a domain rejection type
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public <T extends Message> VerifyAcknowledgements
    withRejections(Class<T> type, RejectionCriterion<T> predicate) {
        VerifyAcknowledgements oneRejection = ackedWithRejections(type, predicate);
        return this.and(oneRejection);
    }

    /**
     * Creates a new verifier adding a check to contain a provided domain rejection.
     *
     * @param <T>           a domain rejection type
     * @param type          a type of a domain rejection specified by a {@link RejectionClass}
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param predicate     a predicate filtering domain rejections
     * @return a new {@link VerifyAcknowledgements} instance
     */
    public <T extends Message> VerifyAcknowledgements
    withRejections(Class<T> type, Count expectedCount, RejectionCriterion<T> predicate) {
        VerifyAcknowledgements oneRejection = ackedWithRejections(type, expectedCount, predicate);
        return this.and(oneRejection);
    }
}
