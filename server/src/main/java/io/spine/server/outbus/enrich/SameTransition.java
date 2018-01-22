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

package io.spine.server.outbus.enrich;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper predicate that allows to find functions with the same transition from
 * source event to enrichment class.
 *
 * <p>Such functions are not necessarily equal because they may have different translators.
 *
 * @see EnrichmentFunction
 * @author Alexander Yevsyukov
 */
final class SameTransition implements Predicate<EnrichmentFunction> {

    private final EnrichmentFunction function;

    static SameTransition asFor(EnrichmentFunction function) {
        checkNotNull(function);
        return new SameTransition(function);
    }

    private SameTransition(EnrichmentFunction function) {
        this.function = function;
    }

    @Override
    public boolean apply(@Nullable EnrichmentFunction input) {
        if (input == null) {
            return false;
        }
        final boolean sameSourceClass = function.getSourceClass()
                                                .equals(input.getSourceClass());
        final boolean sameEnrichmentClass = function.getEnrichmentClass()
                                                    .equals(input.getEnrichmentClass());
        return sameSourceClass && sameEnrichmentClass;
    }
}
