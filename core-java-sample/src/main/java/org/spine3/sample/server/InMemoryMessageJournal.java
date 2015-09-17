/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.sample.server;

import com.google.protobuf.Message;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * In-memory-based implementation of the {@link com.google.protobuf.Message} repository.
 */
public class InMemoryMessageJournal<I, M extends Message> extends BaseMessageJournal<I, M> {

    private final Map<I, List<M>> messagesMap = newHashMap();


    public static <I, M extends Message> InMemoryMessageJournal<I, M> newInstance(Class<M> messageClass) {
        return new InMemoryMessageJournal<>(messageClass);
    }

    private InMemoryMessageJournal(Class<M> messageClass) {
        super(messageClass);
    }

    @Override
    protected List<M> getById(I id) {

        List<M> result = newArrayList();

        if (messagesMap.containsKey(id)) {
            result = messagesMap.get(id);
        }

        return result;
    }

    @Override
    protected List<M> getAll() {

        final List<M> result = newLinkedList();

        for (I key : messagesMap.keySet()){
            final List<M> messages = getById(key);
            result.addAll(messages);
        }

        return result;
    }

    @Override
    protected void save(I entityId, M message) {

        List<M> messagesById = newArrayList();

        if (messagesMap.containsKey(entityId)) {
            messagesById = messagesMap.get(entityId);
        }

        messagesById.add(message);

        messagesMap.put(entityId, messagesById);
    }
}
