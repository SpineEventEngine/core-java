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

package io.spine.server.route;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.RejectionContext;
import io.spine.core.Rejections;

import java.util.Set;

/**
 * Internal utility class that provides default {@link RejectionRoute}s for obtaining a producer
 * from a rejection.
 *
 * @author Alexander Yevsyukov
 */
public class RejectionProducers {

    /** Prevents instantiation of this utility class. */
    private RejectionProducers() {}

    /**
     * Creates an rejection route that obtains producer ID from an {@code RejectionContext} and
     * returns it as a sole element of the immutable set.
     *
     * @param <I> the type of the entity IDs to which a rejection would be routed
     * @return new route instance
     */
    public static <I> RejectionRoute<I, Message> fromContext() {
        return new FromContext<>();
    }

    /**
     * Creates an rejection route that obtains producer ID from a {@code RejectionContext} and
     * returns it as a sole element of the the immutable set.
     *
     * @param <I> the type of the IDs of entities for which the rejection would be routed
     * @return new function instance
     */
    public static <I> RejectionRoute<I, Message> fromFirstMessageField() {
        return new FromFirstMessageField<>();
    }

    /**
     * Obtains a rejection producer ID from the context of the rejection.
     */
    private static final class FromContext<I> implements RejectionRoute<I, Message> {

        private static final long serialVersionUID = 0L;

        @Override
        public Set<I> apply(Message message, RejectionContext context) {
            Optional<I> id = Rejections.getProducer(context);
            if (!id.isPresent()) {
                return ImmutableSet.of();
            }
            return ImmutableSet.of(id.get());
        }

        @Override
        public String toString() {
            return "RejectionProducers.fromContext()";
        }
    }

    /**
     * The route that obtains a producer ID from the first field of the rejection message.
     */
    private static final class FromFirstMessageField<I> implements RejectionRoute<I, Message> {

        private static final long serialVersionUID = 0L;

        private final FromRejectionMessage<I> func = FromRejectionMessage.fieldAt(0);
        @Override
        public Set<I> apply(Message message, RejectionContext context) {
            I id = func.apply(message, context);
            return ImmutableSet.of(id);
        }
    }

    /**
     * Obtains a rejection producer ID from a field of a rejection message.
     */
    private static final class FromRejectionMessage<I>
            extends FieldAtIndex<I, Message, RejectionContext> {

        private static final long serialVersionUID = 0L;

        private FromRejectionMessage(int idIndex) {
            super(idIndex);
        }

        static <I> FromRejectionMessage<I> fieldAt(int index) {
            return new FromRejectionMessage<>(index);
        }
    }
}
