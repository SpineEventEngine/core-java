/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.integration;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An abstract verifier of Bounded Context acknowledgements. Its intended to be executed by the
 * {@link BlackBoxBoundedContext Black Box Bounded Context}. Its implementations throw assertion
 * errors if the acknowledgements observed in the Bounded Context do not meet the verifier criteria.
 *
 * <p>Contains static factory methods for creating acknowledgement verifiers, checking that
 * commands were acknowledged, responded with rejections, and errors.
 *
 * <p>Allows combining verifiers using {@link #and(AcknowledgementsVerifier) and()} or factory method
 * shortcuts: {@code ackedWithoutErrors().and(ackedWithRejection(rej))} can be simplified
 * to {@code ackedWithoutErrors().withRejection(rej)}.
 *
 * @author Mykhailo Drachuk
 */
@SuppressWarnings("ClassWithTooManyMethods")
@VisibleForTesting
public abstract class AcknowledgementsVerifier {

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
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier acked(int expectedCount) {
        checkArgument(expectedCount >= 0, "0 or more acknowledgements must be expected.");
        return new AcksCountVerifier(expectedCount);
    }

    /*
     * Factory methods for verifying acks with errors.
     ******************************************************************************/

    /**
     * Verifies that the command handling did not respond with {@link Error error}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithoutErrors() {
        return new AcksErrorAbsenceVerifier();
    }

    /**
     * Verifies that a command or an event was handled responding with some {@link Error error}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithErrors() {
        return new AcksErrorPresenceVerifier();
    }

    /**
     * Verifies that a command or an event was handled responding with specified number of
     * {@link Error errors}.
     *
     * @param expectedCount an amount of errors that are expected to match the qualifier
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithErrors(int expectedCount) {
        checkArgument(expectedCount >= 0,
                      "0 or more errors must be expected.");
        return new AcksErrorCountVerifier(expectedCount);
    }

    /**
     * Verifies that a command or an event was handled responding with an error matching a provided
     * {@link ErrorQualifier error qualifier}.
     *
     * @param qualifier an error qualifier specifying which kind of error should be a part
     *                  of acknowledgement
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithErrors(ErrorQualifier qualifier) {
        return new AcksSpecificErrorPresenceVerifier(qualifier);
    }

    /**
     * Verifies that a command or an event was handled responding with an error matching a provided
     * {@link ErrorQualifier error qualifier}.
     *
     * @param expectedCount an amount of errors that are expected to match the qualifier
     * @param qualifier     an error qualifier specifying which kind of error should be a part
     *                      of acknowledgement
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithErrors(int expectedCount,
                                                           ErrorQualifier qualifier) {
        checkArgument(expectedCount >= 0, "0 or more errors matching qualifier must be expected.");
        return new AcksSpecificErrorCountVerifier(expectedCount, qualifier);
    }

    /*
     * Factory methods for verifying acks with rejections.
     ******************************************************************************/

    /**
     * Verifies that a command handling did not respond with any {@link Rejection rejections}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithoutRejections() {
        return new AcksRejectionAbsenceVerifier();
    }

    /**
     * Verifies that a command or an event was handled responding with some
     * {@link Rejection rejection}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithRejections() {
        return new AcksRejectionPresenceVerifier();
    }

    /**
     * Verifies that a command or an event was handled responding with a {@link Rejection rejection}
     * of the provided type.
     *
     * @param type rejection type in a form of message class
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithRejections(Class<? extends Message> type) {
        RejectionClass rejectionClass = RejectionClass.of(type);
        return ackedWithRejections(rejectionClass);
    }

    /**
     * Verifies that a command or an event was handled responding with a {@link Rejection rejection}
     * of the provided type.
     *
     * @param type rejection type in a form of {@link RejectionClass RejectionClass}
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithRejections(RejectionClass type) {
        return new AcksRejectionOfTypePresenceVerifier(type);
    }

    /**
     * Verifies that a command or an event was handled responding with rejection matching the
     * provided predicate.
     *
     * @param type      a type of a domain rejection specified by a message class
     * @param predicate a predicate filtering the domain rejections
     * @param <T>       a domain rejection type
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static <T extends Message> AcknowledgementsVerifier
    ackedWithRejections(Class<T> type, RejectionPredicate<T> predicate) {
        return new AcksSpecificRejectionPresenceVerifier<>(type, predicate);
    }

    /**
     * Verifies that a command or an event was handled responding with a rejection specified
     * amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithRejections(int expectedCount) {
        checkArgument(expectedCount >= 0, "0 or more rejections must be expected.");
        return new AcksRejectionCountVerifier(expectedCount);
    }

    /**
     * Verifies that a command or an event was handled responding with a {@link Rejection rejection}
     * of a provided type specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          rejection type in a form of message class
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithRejections(int expectedCount,
                                                               Class<? extends Message> type) {
        checkArgument(expectedCount >= 0,
                      "0 or more rejections of rejections of message class type must be expected.");
        RejectionClass rejectionClass = RejectionClass.of(type);
        return ackedWithRejections(expectedCount, rejectionClass);
    }

    /**
     * Verifies that a command or an event was handled responding with a {@link Rejection rejection}
     * of a provided type specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          rejection type in a form of {@link RejectionClass RejectionClass}
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static AcknowledgementsVerifier ackedWithRejections(int expectedCount,
                                                               RejectionClass type) {
        checkArgument(expectedCount >= 0,
                      "0 or more rejections of rejecetions of class must be expected.");
        return new AcksRejectionOfTypeCountVerifier(type, expectedCount);
    }

    /**
     * Verifies that a command or an event was handled responding with a provided domain rejection
     * specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          a type of a domain rejection specified by a message class
     * @param predicate     a predicate filtering domain rejections
     * @param <T>           a domain rejection type
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public static <T extends Message> AcknowledgementsVerifier
    ackedWithRejections(int expectedCount, Class<T> type, RejectionPredicate<T> predicate) {
        checkArgument(expectedCount >= 0, "0 or more specified rejections must be expected.");
        return new AcksSpecificRejectionCountVerifier<>(expectedCount, type, predicate);
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
    public AcknowledgementsVerifier and(AcknowledgementsVerifier otherVerifier) {
        return AcksVerifierCombination.of(this, otherVerifier);
    }

    /**
     * Creates a new verifier adding a check to not contain any {@link Error errors}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withoutErrors() {
        AcknowledgementsVerifier noErrors = ackedWithoutErrors();
        return this.and(noErrors);
    }

    /**
     * Creates a new verifier adding a check to contain at least one {@link Error error}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withErrors() {
        AcknowledgementsVerifier withError = ackedWithErrors();
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain specified amount of {@link Error errors}.
     *
     * @param expectedCount an amount of errors that are expected to be observed in Bounded Context
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withErrors(int expectedCount) {
        AcknowledgementsVerifier withError = ackedWithErrors(expectedCount);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain an {@link Error error} that
     * matches the qualifier.
     *
     * @param qualifier an error qualifier specifying which kind of error should be a part
     *                  of acknowledgement
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withErrors(ErrorQualifier qualifier) {
        AcknowledgementsVerifier withError = ackedWithErrors(qualifier);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain an {@link Error error} that
     * matches the qualifier.
     *
     * @param expectedCount an amount of errors that are expected to match the qualifier
     * @param qualifier     an error qualifier specifying which kind of error should be a part
     *                      of acknowledgement
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withErrors(int expectedCount, ErrorQualifier qualifier) {
        AcknowledgementsVerifier withError = ackedWithErrors(expectedCount, qualifier);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to not contain any {@link Error errors} or
     * {@link Rejection rejections}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withoutErrorsOrRejections() {
        AcknowledgementsVerifier noRejections = ackedWithoutRejections();
        AcknowledgementsVerifier noErrors = ackedWithoutErrors();
        return this.and(noErrors.and(noRejections));
    }

    /**
     * Creates a new verifier adding a check to not contain any {@link Rejection rejections}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withoutRejections() {
        AcknowledgementsVerifier noRejections = ackedWithoutRejections();
        return this.and(noRejections);
    }

    /**
     * Creates a new verifier adding a check to contain some {@link Rejection rejection}.
     *
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withRejections() {
        AcknowledgementsVerifier someRejection = ackedWithRejections();
        return this.and(someRejection);
    }

    /**
     * Creates a new verifier adding a check to contain a {@link Rejection rejection} of a
     * type specified by {@code class}.
     *
     * @param type a type of a domain rejection specified by message class
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withRejections(Class<? extends Message> type) {
        AcknowledgementsVerifier rejectedType = ackedWithRejections(type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a {@link Rejection rejection} of a
     * type specified by a {@link RejectionClass rejection class}.
     *
     * @param type a type of a domain rejection specified by a {@link RejectionClass}
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withRejections(RejectionClass type) {
        AcknowledgementsVerifier rejectedType = ackedWithRejections(type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a {@link Rejection rejection} of a
     * type specified by a {@link RejectionClass rejection class} specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          rejection type in a form of message class
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withRejections(int expectedCount,
                                                   Class<? extends Message> type) {
        AcknowledgementsVerifier rejectedType = ackedWithRejections(expectedCount, type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a {@link Rejection rejection} of a
     * type specified by a {@link RejectionClass rejection class} specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          rejection type in a form of {@link RejectionClass RejectionClass}
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public AcknowledgementsVerifier withRejections(int expectedCount, RejectionClass type) {
        AcknowledgementsVerifier rejectedType = ackedWithRejections(expectedCount, type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a provided domain rejection.
     *
     * @param type      a type of a domain rejection specified by a {@link RejectionClass}
     * @param predicate a predicate filtering domain rejections
     * @param <T>       a domain rejection type
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public <T extends Message> AcknowledgementsVerifier
    withRejections(Class<T> type, RejectionPredicate<T> predicate) {
        AcknowledgementsVerifier oneRejection = ackedWithRejections(type, predicate);
        return this.and(oneRejection);
    }

    /**
     * Creates a new verifier adding a check to contain a provided domain rejection.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          a type of a domain rejection specified by a {@link RejectionClass}
     * @param predicate     a predicate filtering domain rejections
     * @param <T>           a domain rejection type
     * @return a new {@link AcknowledgementsVerifier} instance
     */
    public <T extends Message> AcknowledgementsVerifier
    withRejections(int expectedCount, Class<T> type, RejectionPredicate<T> predicate) {
        AcknowledgementsVerifier oneRejection = ackedWithRejections(expectedCount, type, predicate);
        return this.and(oneRejection);
    }
}
