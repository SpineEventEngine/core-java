/*
 *  Copyright 2018, TeamDev. All rights reserved.
 *
 *  Redistribution and use in source and/or binary forms, with or without
 *  modification, must retain the above copyright notice and the following
 *  disclaimer.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.tuple;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Abstract base for a values that can be one of the possible types.
 *
 * @author Alexander Yevsyukov
 */
public abstract class Either implements Iterable<Message>, Serializable {

    private static final long serialVersionUID = 0L;

    private final GeneratedMessageV3 value;
    private final int index;

    protected Either(Message value, int index) {
        /* We need instances of GeneratedMessageV3 as they are Serializable.
           The only known case of message class, which does not descend from
           GeneratedMessageV3 is DynamicMessage, which Spine does not support. */
        this.value = (GeneratedMessageV3) checkNotNull(value);
        checkArgument(index >= 0, "Index must be greater or equal zero");
        this.index = index;
    }

    /**
     * Obtains the stored value.
     */
    protected final Message getValue() {
        return value;
    }

    /**
     * Obtains a zero-based index of the value.
     */
    protected final int getIndex() {
        return index;
    }

    protected static <T> T get(Either either, int index) {
        if (index != either.getIndex()) {
            String errMsg =
                    format("`Either` instance has value of a different type than requested. " +
                                   "Value index in `Either` is %d. Requested index: %d",
                           either.getIndex(), index);
            throw new IllegalStateException(errMsg);
        }

        @SuppressWarnings("unchecked") // It's the caller responsibility to ensure correct type.
        T result = (T) either.getValue();
        return result;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value, index);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Either other = (Either) obj;
        return Objects.equals(this.value, other.value)
                && Objects.equals(this.index, other.index);
    }

    @Override
    public final Iterator<Message> iterator() {
        Set<Message> singleton = Collections.singleton((Message) value);
        return singleton.iterator();
    }
}
