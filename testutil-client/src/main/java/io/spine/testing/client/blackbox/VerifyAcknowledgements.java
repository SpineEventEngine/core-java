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

package io.spine.testing.client.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Descriptors.Descriptor;
import io.spine.base.Error;
import io.spine.base.RejectionMessage;
import io.spine.type.RejectionType;
import io.spine.type.TypeName;

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
 */
@SuppressWarnings({"ClassWithTooManyMethods", "unused", "RedundantSuppression"})
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
     * @return a new instance
     */
    public static VerifyAcknowledgements ackedWithoutErrors() {
        return new ErrorAbsenceVerify();
    }

    /**
     * Verifies that a command or an event was handled responding with some {@link Error error}.
     *
     * @return a new instance
     */
    public static VerifyAcknowledgements ackedWithErrors() {
        return new ErrorPresenceVerify();
    }

    /**
     * Verifies that a command or an event was handled responding with specified number of
     * {@link Error errors}.
     */
    public static VerifyAcknowledgements ackedWithErrors(Count expected) {
        return new ErrorCountVerify(expected);
    }

    /**
     * Verifies that a command or an event was handled responding with an error matching a provided
     * {@link ErrorCriterion}.
     */
    public static VerifyAcknowledgements ackedWithErrors(ErrorCriterion criterion) {
        return new SpecificErrorPresenceVerify(criterion);
    }

    /**
     * Verifies that a command or an event was handled responding with an error matching a provided
     * {@link ErrorCriterion error criterion}.
     */
    public static VerifyAcknowledgements ackedWithErrors(ErrorCriterion criterion, Count expected) {
        return new SpecificErrorCountVerify(criterion, expected);
    }

    /*
     * Factory methods for verifying acks with rejections.
     ******************************************************************************/

    /**
     * Verifies that a command handling did not respond with any rejections.
     *
     * @return a new instance
     */
    public static VerifyAcknowledgements ackedWithoutRejections() {
        return new RejectionAbsenceVerify();
    }

    /**
     * Verifies that a command or an event was handled with a rejection.
     *
     * @return a new instance
     */
    public static VerifyAcknowledgements ackedWithRejections() {
        return new RejectionPresenceVerify();
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection of the provided
     * type.
     *
     * @param cls rejection type in a form of message class
     * @return a new instance
     */
    public static VerifyAcknowledgements
    ackedWithRejections(Class<? extends RejectionMessage> cls) {
        RejectionType rt = toType(cls);
        return ackedWithRejections(rt);
    }

    private static RejectionType toType(Class<? extends RejectionMessage> cls) {
        Descriptor descriptor = TypeName.of(cls)
                                        .messageDescriptor();
        RejectionType result = new RejectionType(descriptor);
        return result;
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection
     * of the provided type.
     */
    public static VerifyAcknowledgements ackedWithRejections(RejectionType type) {
        return new RejectionOfTypePresenceVerify(type);
    }

    /**
     * Verifies that a command or an event was handled responding with rejection matching the
     * provided predicate.
     *
     * @param type      a type of a domain rejection specified by a message class
     * @param predicate a predicate filtering the domain rejections
     * @param <T>       a domain rejection type
     * @return a new instance
     */
    public static <T extends RejectionMessage> VerifyAcknowledgements
    ackedWithRejections(Class<T> type, RejectionCriterion<T> predicate) {
        return new SpecificRejectionPresenceVerify<>(type, predicate);
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection specified
     * number of times.
     */
    public static VerifyAcknowledgements ackedWithRejections(Count expectedCount) {
        return new RejectionCountVerify(expectedCount);
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection of a provided
     * type specified number of times.
     */
    public static VerifyAcknowledgements
    ackedWithRejections(Class<? extends RejectionMessage> cls, Count expected) {
        RejectionType rt = toType(cls);
        return ackedWithRejections(rt, expected);
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection of a provided
     * type specified number of times.
     */
    public static VerifyAcknowledgements ackedWithRejections(RejectionType type, Count expected) {
        return new RejectionOfTypeCountVerify(type, expected);
    }

    /**
     * Verifies that a command or an event was handled responding with a provided domain rejection
     * specified number of times.
     *
     * @param <T>           a domain rejection type
     * @param type          a type of a domain rejection specified by a message class
     * @param expected a number of rejection that are expected in Bounded Context
     * @param criterion     a criterion filtering domain rejections
     * @return a new instance
     */
    public static <T extends RejectionMessage> VerifyAcknowledgements
    ackedWithRejections(Class<T> type, Count expected, RejectionCriterion<T> criterion) {
        return new SpecificRejectionCountVerify<>(type, expected, criterion);
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
     */
    public VerifyAcknowledgements withoutErrors() {
        VerifyAcknowledgements noErrors = ackedWithoutErrors();
        return this.and(noErrors);
    }

    /**
     * Creates a new verifier adding a check to contain at least one {@link Error error}.
     */
    public VerifyAcknowledgements withErrors() {
        VerifyAcknowledgements withError = ackedWithErrors();
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain specified number of
     * {@linkplain Error errors}.
     */
    public VerifyAcknowledgements withErrors(Count expected) {
        VerifyAcknowledgements withError = ackedWithErrors(expected);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain an {@link Error error} that matches
     * the criterion.
     *
     * @param criterion
     *         an error criterion specifying which kind of error should be a part of acknowledgement
     * @return a new instance
     */
    public VerifyAcknowledgements withErrors(ErrorCriterion criterion) {
        VerifyAcknowledgements withError = ackedWithErrors(criterion);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain an {@link Error error} that matches
     * the criterion.
     *
     * @param criterion
     *         an error criterion specifying which kind of error should be a part of acknowledgement
     * @param expected
     *         a number of errors that are expected to match the criterion
     * @return a new instance
     */
    public VerifyAcknowledgements withErrors(ErrorCriterion criterion, Count expected) {
        VerifyAcknowledgements withError = ackedWithErrors(criterion, expected);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to not contain any {@link Error errors} or rejections.
     *
     * @return a new instance
     */
    public VerifyAcknowledgements withoutErrorsOrRejections() {
        VerifyAcknowledgements noRejections = ackedWithoutRejections();
        VerifyAcknowledgements noErrors = ackedWithoutErrors();
        return this.and(noErrors.and(noRejections));
    }

    /**
     * Creates a new verifier adding a check to not contain any rejections.
     *
     * @return a new instance
     */
    public VerifyAcknowledgements withoutRejections() {
        VerifyAcknowledgements noRejections = ackedWithoutRejections();
        return this.and(noRejections);
    }

    /**
     * Creates a new verifier adding a check to contain some rejection.
     *
     * @return a new instance
     */
    public VerifyAcknowledgements withRejections() {
        VerifyAcknowledgements someRejection = ackedWithRejections();
        return this.and(someRejection);
    }

    /**
     * Creates a new verifier adding a check to contain a rejection of a
     * type specified by {@code class}.
     */
    public VerifyAcknowledgements withRejections(Class<? extends RejectionMessage> cls) {
        VerifyAcknowledgements rejectedType = ackedWithRejections(cls);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a rejection of the specified type.
     */
    public VerifyAcknowledgements withRejections(RejectionType type) {
        VerifyAcknowledgements rejectedType = ackedWithRejections(type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a rejection of the passed type
     * specified number of times.
     */
    public
    VerifyAcknowledgements withRejections(Class<? extends RejectionMessage> cls, Count expected) {
        VerifyAcknowledgements rejectedType = ackedWithRejections(cls, expected);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a rejection of the passed type
     * specified number of times.
     */
    public VerifyAcknowledgements withRejections(RejectionType type, Count expected) {
        VerifyAcknowledgements rejectedType = ackedWithRejections(type, expected);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a provided domain rejection.
     */
    public <T extends RejectionMessage>
    VerifyAcknowledgements withRejections(Class<T> type, RejectionCriterion<T> predicate) {
        VerifyAcknowledgements oneRejection = ackedWithRejections(type, predicate);
        return this.and(oneRejection);
    }

    /**
     * Creates a new verifier adding a check to contain a provided domain rejection.
     *
     * @param <T>
     *         a domain rejection type
     * @param type
     *         a class of the domain rejections
     * @param expected
     *         a number of rejection that are expected in Bounded Context
     * @param predicate
     *         a predicate filtering domain rejections
     * @return a new instance
     */
    public <T extends RejectionMessage> VerifyAcknowledgements
    withRejections(Class<T> type, Count expected, RejectionCriterion<T> predicate) {
        VerifyAcknowledgements oneRejection = ackedWithRejections(type, expected, predicate);
        return this.and(oneRejection);
    }
}
