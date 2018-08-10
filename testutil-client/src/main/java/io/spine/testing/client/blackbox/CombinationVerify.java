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
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * A special kind of a {@link VerifyAcknowledgements Acknowledgements Verifier} that
 * executes a list of assertions one by one.
 * 
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
final class CombinationVerify extends VerifyAcknowledgements {

    private final List<VerifyAcknowledgements> verifiers;

    /**
     * Creates a combination of two verifiers. More verifiers are appended using
     * {@link #and(VerifyAcknowledgements) and()}.
     */
    private CombinationVerify(Collection<VerifyAcknowledgements> verifiers) {
        super();
        this.verifiers = ImmutableList.copyOf(verifiers);
    }

    /**
     * Creates a new {@link CombinationVerify AcksVerifierCombination} from two regular
     * {@link VerifyAcknowledgements ack verifiers}.
     *
     * @param first  a verifier that will be executed first upon {@link #verify(Acknowledgements)}
     * @param second a verifier executed second
     * @return a new verifier instance
     */
    static CombinationVerify of(VerifyAcknowledgements first,
                                VerifyAcknowledgements second) {
        List<VerifyAcknowledgements> verifiers = newArrayList();
        addVerifierToList(first, verifiers);
        addVerifierToList(second, verifiers);
        return new CombinationVerify(verifiers);
    }

    /**
     * Creates a new {@link CombinationVerify AcksVerifierCombination} from a list of
     * {@link VerifyAcknowledgements verifiers} and a new verifier.
     *
     * @param items       verifiers that will be executed first upon
     *                    {@link #verify(Acknowledgements)}
     * @param newVerifier a verifier executed added to the end of the verifiers list
     * @return a new verifier instance
     */
    static CombinationVerify of(Iterable<VerifyAcknowledgements> items,
                                VerifyAcknowledgements newVerifier) {
        List<VerifyAcknowledgements> verifiers = newArrayList(items);
        addVerifierToList(newVerifier, verifiers);
        return new CombinationVerify(verifiers);
    }

    private static void addVerifierToList(VerifyAcknowledgements verifier,
                                          Collection<VerifyAcknowledgements> items) {
        if (verifier instanceof CombinationVerify) {
            items.addAll(((CombinationVerify) verifier).verifiers);
        } else {
            items.add(verifier);
        }
    }

    /**
     * Executes all of the verifiers that were combined using
     * {@link #and(VerifyAcknowledgements) and()}.
     *
     * @param acks acknowledgements of handling commands by the Bounded Context
     */
    @Override
    public void verify(Acknowledgements acks) {
        for (VerifyAcknowledgements verifier : verifiers) {
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
    public CombinationVerify and(VerifyAcknowledgements verifier) {
        return of(verifiers, verifier);
    }
}
