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
package org.spine3.engine;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import java.util.List;

/**
 * Defines the low level data interface of the storage
 * that is used to read/write events, commands, snapshots, etc.
 *
 * @author Mikhail Mikhaylov
 */
public interface Storage {

    void storeSingleton(Message id, Message message);

    void store(Class clazz, Message message, Message messageId, Message aggregateRootId, Timestamp timestamp, int version);

    <T extends Message> T readSingleton(Message id);

    <T extends Message> List<T> query(Class clazz);

    <T extends Message> List<T> query(Class clazz, Message aggregateRootId);

    <T extends Message> List<T> query(Class clazz, Message aggregateRootId, int version);

    <T extends Message> List<T> query(Class clazz, Message aggregateRootId, Timestamp from);

    <T extends Message> List<T> query(Class clazz, Timestamp from);

}
