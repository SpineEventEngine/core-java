/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.protobuf;

import com.google.common.collect.UnmodifiableIterator;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.util.Iterator;

/**
 * An iterator that packs messages from the source iterator.
 *
 * @author Alexander Yevsyukov
 * @see AnyPacker#pack(Iterator)
 */
class PackingIterator extends UnmodifiableIterator<Any> {

    private final Iterator<Message> source;

    PackingIterator(Iterator<Message> source) {
        super();
        this.source = source;
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    /**
     * Takes the message from the source iterator, wraps it into {@code Any}
     * and returns.
     *
     * <p>If the source iterator returns {@code null} message, the default instance
     * of {@code Any} will be returned.
     *
     * @return the packed message or default {@code Any}
     */
    @Override
    public Any next() {
        final Message next = source.next();
        final Any result = next != null
                           ? AnyPacker.pack(next)
                           : Any.getDefaultInstance();
        return result;
    }
}
