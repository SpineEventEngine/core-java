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

package io.spine.core;

import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.MessageContext;
import io.spine.core.Enrichment.Container;

import java.util.Optional;

import static io.spine.core.Enrichments.containerIn;

/**
 * A common interface for message contexts that hold enrichments.
 */
@GeneratedMixin
public interface EnrichableMessageContext extends MessageContext {

    /**
     * Obtains an instance of {@link Enrichment} from the context of the message.
     */
    @SuppressWarnings("override") // in generated code.
    Enrichment getEnrichment();

    default <E extends Message> Optional<E> find(Class<E> cls) {
        Optional<Container> container = containerIn(this);
        Optional<E> result = container.flatMap(c -> Enrichments.find(cls, c));
        return result;
    }

    /**
     * Obtains enrichment of the passed class.
     *
     * @throws IllegalStateException if the enrichment is not found
     */
    default <E extends Message> E get(Class<E> cls) {
        Container container = containerIn(this).orElse(Container.getDefaultInstance());
        E result = Enrichments.get(cls, container);
        return result;
    }
}

