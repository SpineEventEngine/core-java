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

package org.spine3.server;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * The common interface for aggregate root repositories.
 *
 * @author Alexander Yevsyukov
 */
public interface AggregateRootRepository<I extends Message,
        R extends AggregateRoot,
        C extends Message> extends Repository<I, R, C> {
    /**
     * Processes the command by dispatching it one of the repository methods or
     * to a method of an aggregate root.
     * <p>
     * If the passed command is for an aggregate, its instance is loaded by ID
     * obtained from the passed command.
     * <p>
     * For more details on writing aggregate commands read
     * <a href="http://github.com/SpineEventEngine/core/wiki/Writing-Aggregate-Commands">"Writing Aggregate Commands"</a>.
     *
     * @param command the command to dispatch
     * @param context context info of the command
     * @return a list of the event records
     * @throws InvocationTargetException if an exception occurs during command dispatching
     *
     * @see <a href="http://github.com/SpineEventEngine/core/wiki/Writing-Aggregate-Commands">Writing Aggregate Commands</a>
     */
    List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException;

    //TODO:2015-09-06:alexander.yevsyukov: In order to remove this method, we need to have loadOrCreate which
    // would serve for finding an object when it's already stored or created when it's not.
    /**
     * Creates, initializes, and stores a new aggregated root.
     * <p>
     * The initial state of the aggregate root is taken from the creation command.
     *
     * @param command creation command
     * @param context creation command context
     * @return a list of the event records
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    @Subscribe
    List<EventRecord> handleCreate(C command, CommandContext context) throws InvocationTargetException;
}
