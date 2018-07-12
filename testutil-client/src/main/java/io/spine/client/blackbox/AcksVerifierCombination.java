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

package io.spine.client.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * A special kind of a {@link AcknowledgementsVerifier Acknowledgements Verifier} that
 * executes a list of assertions one by one.
 * 
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
class AcksVerifierCombination extends AcknowledgementsVerifier {

    private final List<AcknowledgementsVerifier> verifiers;

    /**
     * Creates a combination of two verifiers. More verifiers are appended using
     * {@link #and(AcknowledgementsVerifier) and()}.
     */
    private AcksVerifierCombination(Collection<AcknowledgementsVerifier> verifiers) {
        super();
        this.verifiers = ImmutableList.copyOf(verifiers);
    }

    /**
     * Creates a new {@link AcksVerifierCombination AcksVerifierCombination} from two regular
     * {@link AcknowledgementsVerifier ack verifiers}.
     *
     * @param first  a verifier that will be executed first upon {@link #verify(Acknowledgements)}
     * @param second a verifier executed second
     * @return a new verifier instance
     */
    static AcksVerifierCombination of(AcknowledgementsVerifier first,
                                      AcknowledgementsVerifier second) {
        List<AcknowledgementsVerifier> verifiers = newArrayList();
        addVerifierToList(first, verifiers);
        addVerifierToList(second, verifiers);
        return new AcksVerifierCombination(verifiers);
    }

    /**
     * Creates a new {@link AcksVerifierCombination AcksVerifierCombination} from a list of
     * {@link AcknowledgementsVerifier verifiers} and a new verifier.
     *
     * @param items       verifiers that will be executed first upon
     *                    {@link #verify(Acknowledgements)}
     * @param newVerifier a verifier executed added to the end of the verifiers list
     * @return a new verifier instance
     */
    static AcksVerifierCombination of(Iterable<AcknowledgementsVerifier> items,
                                      AcknowledgementsVerifier newVerifier) {
        List<AcknowledgementsVerifier> verifiers = newArrayList(items);
        addVerifierToList(newVerifier, verifiers);
        return new AcksVerifierCombination(verifiers);
    }

    private static void addVerifierToList(AcknowledgementsVerifier verifier,
                                          Collection<AcknowledgementsVerifier> items) {
        if (verifier instanceof AcksVerifierCombination) {
            items.addAll(((AcksVerifierCombination) verifier).verifiers);
        } else {
            items.add(verifier);
        }
    }

    /**
     * Executes all of the verifiers that were combined using
     * {@link #and(AcknowledgementsVerifier) and()}.
     *
     * @param acks acknowledgements of handling commands by the Bounded Context
     */
    @Override
    public void verify(Acknowledgements acks) {
        for (AcknowledgementsVerifier verifier : verifiers) {
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
    public AcksVerifierCombination and(AcknowledgementsVerifier verifier) {
        return of(verifiers, verifier);
    }
}
