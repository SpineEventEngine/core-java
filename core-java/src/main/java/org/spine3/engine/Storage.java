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
 * that is used to read and write Protobuf messages.
 *
 * @author Mikhail Mikhaylov
 */
public interface Storage {

    /**
     * Stores singleton instance, e.g. Snapshot.
     *
     * @param id      entity id
     * @param message entity value
     */
    void storeSingleton(Message id, Message message);

    /**
     * Stores common messages: CommandRequest of EventRecord.
     *
     * @param message Protobuf message to store.
     */
    void store(Message message);

    /**
     * Reads singleton instance.
     *
     * @param id  instance id
     * @param <T> instance type
     * @return read instance
     */
    <T extends Message> T readSingleton(Message id);

    /**
     * Queries the storage by Message's class.
     *
     * @param clazz the desired message's class
     * @param <T>   message type
     * @return list of messages
     */
    <T extends Message> List<T> query(Class clazz);

    /**
     * Queries the storage by Message's class and AggregateRoot's Id.
     *
     * @param clazz           the desired message's class
     * @param aggregateRootId the id of aggregateRoot
     * @param <T>             message type
     * @return list of messages
     */
    <T extends Message> List<T> query(Class clazz, Message aggregateRootId);

    /**
     * Queries the storage by Message's class, AggregateRoot Id and version.
     *
     * @param clazz           the desired message's class
     * @param aggregateRootId the id of aggregate root
     * @param version         the version of the aggregate root used as lower threshold for the result list
     * @param <T>             message type
     * @return list of messages
     */
    <T extends Message> List<T> query(Class clazz, Message aggregateRootId, int version);

    /**
     * Queries the storage by Message's class and AggregateRoot Id from timestamp.
     *
     * @param clazz           the desired message's class
     * @param aggregateRootId the id of aggregate root
     * @param from            timestamp to query from
     * @param <T>             message type
     * @return list of messages
     */
    <T extends Message> List<T> query(Class clazz, Message aggregateRootId, Timestamp from);

    /**
     * Queries the storage by Message's class from timestamp.
     *
     * @param clazz the desired message's class
     * @param from  timestamp to query from
     * @param <T>   message type
     * @return list of messages
     */
    <T extends Message> List<T> query(Class clazz, Timestamp from);

}
