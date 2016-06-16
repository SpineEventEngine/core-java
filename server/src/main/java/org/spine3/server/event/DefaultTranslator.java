/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.protobuf.Message;

import javax.annotation.Nullable;

/**
 * The default mechanism for enriching messages based on {@code FieldOptions} of
 * Protobuf message definitions.
 *
 * @param <M> a type of the message of the event to enrich
 * @param <E> a type of the enrichment message
 *
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
/* package */ class DefaultTranslator<M extends Message, E extends Message> implements Function<M, E> {

    private EnricherImpl enricher;

    /* package */ void setEnricherImpl(EnricherImpl enricher) {
        this.enricher = enricher;
    }

    @Nullable
    @Override
    public E apply(@Nullable M input) {
        if (input == null) {
            return null;
        }


        //TODO:2016-06-14:alexander.yevsyukov: Implement
        return null;
    }
}
