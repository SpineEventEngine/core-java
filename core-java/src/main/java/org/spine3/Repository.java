/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Base interface for aggregate root repositories.
 *
 * @param <R> the type of the aggregated root
 * @param <I> the type of the aggregated root id
 * @param <C> the type of the command to create aggregate root instance
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public interface Repository<I extends Message,
                            R extends AggregateRoot,
                            C extends Message> {

    /**
     * Stores the passed aggregate root.
     *
     * @param aggregateRoot an instance to store
     */
    void store(R aggregateRoot);

    /**
     * Loads the an aggregate by given id.
     *
     * @param aggregateId id of the aggregate to load
     * @return the loaded object
     */
    R load(I aggregateId);

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
     *
     */
    List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException;

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
