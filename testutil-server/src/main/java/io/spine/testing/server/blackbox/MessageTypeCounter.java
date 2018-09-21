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

package io.spine.testing.server.blackbox;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import io.spine.type.MessageClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Counts messages sent in the wrapper objects of the type {@code <W>}
 * by their class.
 *
 * @param <C> the type of message class
 * @param <W> the type of objects that contain messages
 * @author Mykhailo Drachuk
 * @author Alexander Yevsyukov
 */
class MessageTypeCounter<C extends MessageClass, W extends Message, M extends Message> {

    private final Map<C, Integer> countByType;
    private final Function<Class<? extends M>, C> rawClassFn;

    MessageTypeCounter(List<W> wrappers,
                       Function<W, C> classFn,
                       Function<Class<? extends M>, C> fn) {
        checkNotNull(wrappers);
        Function<W, C> classFn1 = checkNotNull(classFn);
        Map<C, Integer> counters = new HashMap<>(wrappers.size());
        for (W wrapper : wrappers) {
            C cls = classFn1.apply(wrapper);
            int currentCount = counters.getOrDefault(cls, 0);
            counters.put(cls, currentCount + 1);
        }
        this.countByType = ImmutableMap.copyOf(counters);
        this.rawClassFn = checkNotNull(fn);
    }

    /**
     * Converts the raw class of a message into an instance of the generic
     * parameter {@code <C>}.
     */
    private C toMessageClass(Class<? extends M> rawClass) {
        return rawClassFn.apply(rawClass);
    }

    int get(C messageClass) {
        checkNotNull(messageClass);
        return countByType.getOrDefault(messageClass, 0);
    }

    int get(Class<? extends M> messageClass) {
        checkNotNull(messageClass);
        C cls = toMessageClass(messageClass);
        return get(cls);
    }

    boolean contains(Class<? extends M> classOfMessage) {
        checkNotNull(classOfMessage);
        C cls = toMessageClass(classOfMessage);
        boolean result = contains(cls);
        return result;
    }

    boolean contains(C messageClass) {
        checkNotNull(messageClass);
        boolean result = countByType.containsKey(messageClass);
        return result;
    }
}
