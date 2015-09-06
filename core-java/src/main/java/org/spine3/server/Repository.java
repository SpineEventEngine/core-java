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
                            C extends Message> extends ManyCommandHandler {

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

    //TODO:2015-09-06:alexander.yevsyukov: In order to remove this method, we need to have loadOrCreate which
    // would serve for finding an object when it's already stored or created when it's not.

    class TypeInfo {

        public static final int AGGREGATE_ID_CLASS_GENERIC_INDEX = 0;
        public static final int AGGREGATE_ROOT_CLASS_GENERIC_IDEX = 1;

        private TypeInfo() {}

        /**
         * Returns {@link Class} object representing the aggregate id type of the given repository.
         *
         * @return the aggregate id {@link Class}
         */
        public static <I extends Message> Class<I> getAggregateIdClass(Repository repository) {
            return ServerMethods.getGenericParameterType(repository, AGGREGATE_ID_CLASS_GENERIC_INDEX);
        }

        /**
         * Returns {@link Class} object representing the aggregate root type of the given repository.
         *
         * @return the aggregate root {@link Class}
         */
        public static <R extends AggregateRoot> Class<R> getAggregateRootClass(Repository repository) {
            return ServerMethods.getGenericParameterType(repository, AGGREGATE_ROOT_CLASS_GENERIC_IDEX);
        }
    }
}
