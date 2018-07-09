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
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An abstract verifier of command acknowledgements.
 *
 * <p> Contains static factory methods for creating acknowledgement verifiers, checking that
 * commands were acknowledged, responded with rejections, and errors.
 *
 * <p>Allows combining verifiers using {@link #and(CommandAcksVerifier) and()} or factory method
 * shortcuts: {@code ackedWithoutErrors().and(ackedWithRejection(rej))} can be simplified
 * to {@code ackedWithoutErrors().withRejection(rej)}.
 *
 * @author Mykhailo Drachuk
 */
@SuppressWarnings("ClassWithTooManyMethods")
@VisibleForTesting
public abstract class CommandAcksVerifier {

    /**
     * Executes the command acknowledgement verifier throwing an assertion error if the
     * data does not match the rule..
     *
     * @param acks acknowledgements of handling commands by the Bounded Context
     */
    abstract void verify(CommandAcks acks);

    /**
     * Verifies that Bounded Context responded with a specified number of acknowledgements.
     *
     * @param expectedCount an expected amount of acknowledgements observed in Bounded Context
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier acked(int expectedCount) {
        checkArgument(expectedCount >= 0, "0 or more acknowledgements must be expected.");

        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                int actualCount = acks.count();
                String moreOrLess = compare(actualCount, expectedCount);
                assertEquals(
                        expectedCount, actualCount,
                        "Bounded Context acknowledged " + moreOrLess + " commands than expected"
                );
            }
        };
    }

    /**
     * Compares two integers returning a string stating if the first value is less, more or
     * same number as the second.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static String compare(int firstValue, int secondValue) {
        if (firstValue > secondValue) {
            return "more";
        }
        if (firstValue < secondValue) {
            return "less";
        }
        return "same number";
    }

    /*
     * Factory methods for verifying acks with errors.
     ******************************************************************************/

    /**
     * Verifies that the command handling did not respond with {@link Error error}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithoutErrors() {
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                if (acks.containErrors()) {
                    fail("Bounded Context unexpectedly erred");
                }
            }
        };
    }

    /**
     * Verifies that a command was handled responding with some {@link Error error}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithError() {
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                if (!acks.containErrors()) {
                    fail("Bounded Context unexpectedly did not err");
                }
            }
        };
    }

    /**
     * Verifies that a command was handled responding with a provided {@link Error error}.
     *
     * @param error an error that matches the one in acknowledgement
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithError(Error error) {
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                if (!acks.containErrors(error)) {
                    fail("Bounded Context did not contain an expected error" +
                                 error.getMessage());
                }
            }
        };
    }

    /**
     * Verifies that a command was handled responding with an error matching a provided
     * {@link ErrorQualifier error qualifier}.
     *
     * @param qualifier an error qualifier specifying which kind of error should be a part
     *                  of acknowledgement
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithError(ErrorQualifier qualifier) {
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                if (!acks.containErrors(qualifier)) {
                    fail("Bounded Context did not contain an expected error. "
                                 + qualifier.description());
                }
            }
        };
    }

    /*
     * Factory methods for verifying acks with rejections.
     ******************************************************************************/

    /**
     * Verifies that a command handling did not respond with any {@link Rejection rejections}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithoutRejections() {
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                if (acks.containRejections()) {
                    fail("Bounded Context unexpectedly rejected a message");
                }
            }
        };
    }

    /**
     * Verifies that a command was handled responding with some {@link Rejection rejection}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithRejections() {
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                if (!acks.containRejections()) {
                    fail("Bounded Context did not reject any messages");
                }
            }
        };
    }

    /**
     * Verifies that a command was handled responding with a {@link Rejection rejection}
     * of the provided type.
     *
     * @param type rejection type in a form of message class
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithRejection(Class<? extends Message> type) {
        RejectionClass rejectionClass = RejectionClass.of(type);
        return ackedWithRejection(rejectionClass);
    }

    /**
     * Verifies that a command was handled responding with a {@link Rejection rejection}
     * of the provided type.
     *
     * @param type rejection type in a form of {@link RejectionClass RejectionClass}
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithRejection(RejectionClass type) {
        Class<? extends Message> domainRejection = type.value();
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                if (!acks.containRejections(type)) {
                    fail("Bounded Context did not reject a message of type:" +
                                 domainRejection.getSimpleName());
                }
            }
        };
    }

    /**
     * Verifies that a command was handled responding with a provided {@link Rejection rejection}.
     *
     * @param predicate a predicate filtering the domain rejections
     * @param <T>       a domain rejection type
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static <T extends Message> CommandAcksVerifier
    ackedWithRejection(Class<T> clazz, RejectionPredicate<T> predicate) {
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                if (!acks.containRejection(clazz, predicate)) {
                    fail("Bounded Context did not reject a message:"
                                 + predicate.message());
                }
            }
        };
    }

    /**
     * Verifies that a command was handled responding with a provided domain rejection
     * specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithRejections(int expectedCount) {
        checkArgument(expectedCount >= 0, "0 or more rejections must be expected.");
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                assertEquals(expectedCount, acks.countRejections(),
                             "Bounded Context did not contain a rejection expected amount of times.");
            }
        };
    }

    /**
     * Verifies that a command was handled responding with a {@link Rejection rejection}
     * of a provided type specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          rejection type in a form of message class
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithRejections(int expectedCount,
                                                          Class<? extends Message> type) {
        RejectionClass rejectionClass = RejectionClass.of(type);
        return ackedWithRejections(expectedCount, rejectionClass);
    }

    /**
     * Verifies that a command was handled responding with a {@link Rejection rejection}
     * of a provided type specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param type          rejection type in a form of {@link RejectionClass RejectionClass}
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static CommandAcksVerifier ackedWithRejections(int expectedCount, RejectionClass type) {
        checkArgument(expectedCount >= 0, "0 or more rejections of type must be expected.");
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                Class<? extends Message> rejectionClass = type.value();
                assertEquals(expectedCount, acks.countRejections(),
                             "Bounded Context did not contain " + rejectionClass.getSimpleName() +
                                     "rejection expected amount of times.");
            }
        };
    }

    /**
     * Verifies that a command was handled responding with a provided domain rejection
     * specified amount of times.
     *
     * @param expectedCount an amount of rejection that are expected in Bounded Context
     * @param predicate     a predicate filtering domain rejections
     * @param <T>           a domain rejection type
     * @return a new {@link CommandAcksVerifier} instance
     */
    public static <T extends Message> CommandAcksVerifier
    ackedWithRejections(int expectedCount, Class<T> clazz, RejectionPredicate<T> predicate) {
        checkArgument(expectedCount >= 0, "0 or more specified rejections must be expected.");
        return new CommandAcksVerifier() {
            @Override
            void verify(CommandAcks acks) {
                assertEquals(expectedCount, acks.countRejections(clazz, predicate),
                             "Bounded Context did not contain a rejection expected amount of times:"
                                     + predicate.message());
            }
        };
    }

    /*
     * Command acknowledgements verifier combination.
     ******************************************************************************/

    /**
     * Combines current verifier with a provided verifier, making them execute sequentially.
     *
     * @param otherVerifier a verifier executed after the current verifier
     * @return a verifier that executes both current and provided assertions
     */
    public CommandAcksVerifier and(CommandAcksVerifier otherVerifier) {
        return CommandAcksVerifierCombination.of(this, otherVerifier);
    }

    /**
     * A special kind of a {@link CommandAcksVerifier Command Acknowledgements Verifier} that
     * executes a list of assertions one by one.
     */
    private static class CommandAcksVerifierCombination extends CommandAcksVerifier {

        private final List<CommandAcksVerifier> verifiers;

        /**
         * Creates a combination of two verifiers. More verifiers are appended using
         * {@link #and(CommandAcksVerifier) and()}.
         */
        private CommandAcksVerifierCombination(Collection<CommandAcksVerifier> verifiers) {
            super();
            this.verifiers = ImmutableList.copyOf(verifiers);
        }

        public static CommandAcksVerifierCombination of(CommandAcksVerifier first,
                                                        CommandAcksVerifier second) {
            List<CommandAcksVerifier> verifiers = newArrayList();
            addVerifierToList(first, verifiers);
            addVerifierToList(second, verifiers);
            return new CommandAcksVerifierCombination(verifiers);
        }

        public static CommandAcksVerifierCombination of(Iterable<CommandAcksVerifier> items,
                                                        CommandAcksVerifier newVerifier) {
            List<CommandAcksVerifier> verifiers = newArrayList(items);
            addVerifierToList(newVerifier, verifiers);
            return new CommandAcksVerifierCombination(verifiers);
        }

        private static void addVerifierToList(CommandAcksVerifier verifier,
                                              Collection<CommandAcksVerifier> items) {
            if (verifier instanceof CommandAcksVerifierCombination) {
                items.addAll(((CommandAcksVerifierCombination) verifier).verifiers);
            } else {
                items.add(verifier);
            }
        }

        /**
         * Executes all of the verifiers that were combined using
         * {@link #and(CommandAcksVerifier) and()}.
         *
         * @param acks acknowledgements of handling commands by the Bounded Context
         */
        @Override
        void verify(CommandAcks acks) {
            for (CommandAcksVerifier verifier : verifiers) {
                verifier.verify(acks);
            }
        }

        /**
         * Creates a new verifier appending the provided verifier to the current combination.
         *
         * @param verifier a verifier to be added to a combination
         * @return a new verifier instance
         */
        @Override
        public CommandAcksVerifierCombination and(CommandAcksVerifier verifier) {
            return of(verifiers, verifier);
        }
    }

    /*
     * Verifier combination shortcuts.
     ******************************************************************************/

    /**
     * Creates a new verifier adding a check to not contain any {@link Error errors}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withoutErrors() {
        CommandAcksVerifier noErrors = ackedWithoutErrors();
        return this.and(noErrors);
    }

    /**
     * Creates a new verifier adding a check to contain at least one {@link Error error}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withError() {
        CommandAcksVerifier withError = ackedWithError();
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain the specified {@link Error error}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withError(Error error) {
        CommandAcksVerifier withError = ackedWithError(error);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to contain an {@link Error error} that
     * matches the qualifier.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withError(ErrorQualifier qualifier) {
        CommandAcksVerifier withError = ackedWithError(qualifier);
        return this.and(withError);
    }

    /**
     * Creates a new verifier adding a check to not contain any {@link Error errors} or
     * {@link Rejection rejections}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withoutErrorsOrRejections() {
        CommandAcksVerifier noRejections = ackedWithoutRejections();
        CommandAcksVerifier noErrors = ackedWithoutErrors();
        return this.and(noErrors.and(noRejections));
    }

    /**
     * Creates a new verifier adding a check to not contain any {@link Rejection rejections}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withoutRejections() {
        CommandAcksVerifier noRejections = ackedWithoutRejections();
        return this.and(noRejections);
    }

    /**
     * Creates a new verifier adding a check to contain some {@link Rejection rejection}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withRejections() {
        CommandAcksVerifier someRejection = ackedWithRejections();
        return this.and(someRejection);
    }

    /**
     * Creates a new verifier adding a check to contain a {@link Rejection rejection} of a
     * type specified by {@code class}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withRejections(Class<? extends Message> type) {
        CommandAcksVerifier rejectedType = ackedWithRejection(type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a {@link Rejection rejection} of a
     * type specified by a {@link RejectionClass rejection class}.
     *
     * @return a new {@link CommandAcksVerifier} instance
     */
    public CommandAcksVerifier withRejections(RejectionClass type) {
        CommandAcksVerifier rejectedType = ackedWithRejection(type);
        return this.and(rejectedType);
    }

    /**
     * Creates a new verifier adding a check to contain a provided domain rejection.
     *
     * @param predicate a predicate filtering domain rejections
     * @param <T>       a domain rejection type
     * @return a new {@link CommandAcksVerifier} instance
     */
    public <T extends Message> CommandAcksVerifier
    withRejection(Class<T> clazz, RejectionPredicate<T> predicate) {
        CommandAcksVerifier oneRejection = ackedWithRejection(clazz, predicate);
        return this.and(oneRejection);
    }
}
