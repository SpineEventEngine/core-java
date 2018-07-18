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
package io.spine.core;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.type.MessageClass;

import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object holding a class of a business rejection.
 *
 * @author Alex Tymchenko
 */
public class RejectionClass extends MessageClass {

    private static final long serialVersionUID = 0L;

    protected RejectionClass(Class<? extends Message> value) {
        super(value);
    }

    /**
     * Creates a new instance of the rejection class.
     *
     * @param value a value to hold
     * @return new instance
     */
    public static RejectionClass of(Class<? extends Message> value) {
        return new RejectionClass(checkNotNull(value));
    }

    /**
     * Creates a new instance of the rejection class by passed rejection instance.
     *
     * <p>If an instance of {@link Rejection} (which implements {@code Message}) is
     * passed to this method, enclosing rejection message will be un-wrapped to determine
     * the class of the rejection.
     *
     * @param rejectionOrMessage a rejection instance
     * @return new instance
     */
    public static RejectionClass of(Message rejectionOrMessage) {
        Message message = Rejections.ensureMessage(rejectionOrMessage);
        RejectionClass result = of(message.getClass());
        return result;
    }

    /** Creates an immutable set of {@code RejectionClass} from the passed classes. */
    public static Set<RejectionClass> setOf(Iterable<Class<? extends Message>> classes) {
        ImmutableSet.Builder<RejectionClass> builder = ImmutableSet.builder();
        for (Class<? extends Message> cls : classes) {
            builder.add(of(cls));
        }
        return builder.build();
    }

    /** Creates an immutable set of {@code RejectionClass} from the passed classes. */
    @SafeVarargs
    public static Set<RejectionClass> setOf(Class<? extends Message> ...classes) {
        return setOf(Arrays.asList(classes));
    }
}
