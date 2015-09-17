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

/**
 * {@code MessageJournal} based on file system.
 *
 * {@inheritDoc}
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public class FileSystemMessageJournal<I, M extends Message> extends BaseMessageJournal<I, M> {

    public static <I, M extends Message> FileSystemMessageJournal<I, M> newInstance(Class<M> messageClass) {
        return new FileSystemMessageJournal<>(messageClass);
    }

    private FileSystemMessageJournal(Class<M> messageClass) {
        super(messageClass);
    }

    @Override
    protected List<M> getById(Class<M> messageClass, I parentId) {
        final List<M> messages = FileSystemHelper.read(messageClass, parentId);
        return messages;
    }

    @Override
    protected List<M> getAll(Class<M> messageClass) {
        final List<M> messages = FileSystemHelper.readAll(messageClass);
        return messages;
    }

    @Override
    protected void save(I entityId, M message) {
        FileSystemHelper.write(message); // TODO[alexander.litus]: entityId is not used!
    }
}
