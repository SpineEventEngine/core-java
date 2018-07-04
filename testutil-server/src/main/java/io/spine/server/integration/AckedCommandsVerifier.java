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
import io.spine.base.Error;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public abstract class AckedCommandsVerifier {

    abstract void verify(AckedCommands acks);

    public static AckedCommandsVerifier acked(int expectedCount) {
        checkArgument(expectedCount >= 0, "0 or more acknowledgements must be expected.");

        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands acks) {
                int actualCount = acks.count();
                String moreOrLess = compare(actualCount, expectedCount);
                assertEquals(
                        "Bounded Context acknowledged " + moreOrLess + " commands than expected",
                        expectedCount, actualCount);
            }
        };
    }

    private static String compare(int actualCount, int expectedCount) {
        return (expectedCount < actualCount) ? "more" : "less";
    }

    public static AckedCommandsVerifier ackedWithoutErrors() {
        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands acks) {
                assertTrue("Bounded Context unexpectedly erred", acks.withoutErrors());
            }
        };
    }

    public static AckedCommandsVerifier ackedWithError() {
        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands acks) {
                assertTrue("Bounded Context unexpectedly did not err", acks.withErrors());
            }
        };
    }

    public static AckedCommandsVerifier ackedWithError(Error error) {
        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands acks) {
                assertTrue("Bounded Context did not contain an expected error"
                                   + error.getMessage(),
                           acks.withError(error));
            }
        };
    }

    public static AckedCommandsVerifier ackedWithError(ErrorQualifier qualifier) {
        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands acks) {
                assertTrue("Bounded Context did not contain an expected error. "
                                   + qualifier.description(),
                           acks.withError(qualifier));
            }
        };
    }

    public static AckedCommandsVerifier ackedWithoutRejections() {
        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands acks) {
                assertTrue("Bounded Context unexpectedly rejected a message",
                           acks.withoutRejections());
            }
        };
    }

    public AckedCommandsVerifier withError(Error error) {
        AckedCommandsVerifier current = this;
        AckedCommandsVerifier withError = ackedWithError(error);

        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands target) {
                current.verify(target);
                withError.verify(target);
            }
        };
    }

    public AckedCommandsVerifier withError(ErrorQualifier qualifier) {
        AckedCommandsVerifier current = this;
        AckedCommandsVerifier withError = ackedWithError(qualifier);

        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands target) {
                current.verify(target);
                withError.verify(target);
            }
        };
    }

    public AckedCommandsVerifier withoutErrorsOrRejections() {
        AckedCommandsVerifier current = this;
        AckedCommandsVerifier noRejections = ackedWithoutRejections();
        AckedCommandsVerifier noErrors = ackedWithoutErrors();

        return new AckedCommandsVerifier() {
            @Override
            void verify(AckedCommands target) {
                current.verify(target);
                noRejections.verify(target);
                noErrors.verify(target);
            }
        };
    }
}
