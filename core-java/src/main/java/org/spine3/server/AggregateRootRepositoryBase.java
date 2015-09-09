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

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.util.MessageHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;

/**
 * Abstract base for aggregate root repositories.
 *
 * @param <R> the type of the aggregated root
 * @param <I> the type of the aggregated root id
 * @param <C> the type of the command to create a new aggregate root instance
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods") // we can not have instances of AbstractRepository.
public abstract class AggregateRootRepositoryBase<I extends Message,
                                                  R extends AggregateRoot<I, ?>,
                                                  C extends Message> implements AggregateRootRepository<I, R, C> {

    private static final String DISPATCH_METHOD_NAME = "dispatch";

    private RepositoryEventStore eventStore;

    private final Constructor<R> aggregateRootConstructor;

    @SuppressWarnings("ThisEscapedInObjectConstruction") // as we need 'this' to get the runtime generic type values
    protected AggregateRootRepositoryBase() {
        try {
            Class<R> rootClass = TypeInfo.getStoredObjectClass(this);
            Class<I> idClass = TypeInfo.getStoredObjectIdClass(this);

            aggregateRootConstructor = rootClass.getConstructor(idClass);
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }

    //TODO:2015-09-05:alexander.yevsyukov: This should be hidden!
    /**
     * Configures repository with passed implementation of the aggregate storage.
     * It is used for storing and loading aggregated root during handling
     * of the incoming commands.
     *
     * @param eventStore the event store implementation
     */
    public void configure(RepositoryEventStore eventStore) {
        this.eventStore = eventStore;
    }

    public Map<CommandClass, MessageHandler> getHandlers() {
        // Create subscribers that call dispatch() on message classes handled by the aggregate root.
        Map<CommandClass, MessageHandler> subscribers = createDelegatingSubscribers();

        // Add command handlers belonging to this repository.
        Map<CommandClass, MessageHandler> repoSubscribers = ServerMethods.scanForCommandHandlers(this);
        subscribers.putAll(repoSubscribers);

        return subscribers;
    }

    /**
     * Returns the reference to the method {@link #dispatch(Message, CommandContext)} of the passed repository.
     *
     * @return reference to the method
     */
    private MessageHandler toMessageSubscriber() {
        try {
            Method method = getClass().getMethod(DISPATCH_METHOD_NAME, Message.class, CommandContext.class);
            final MessageHandler result = new MessageHandler(this, method);
            return result;
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }

    /**
     * Creates a map of subscribers that call {@link AggregateRootRepository#dispatch(Message, CommandContext)}
     * method for all commands of the aggregate root class of this repository.
     */
    private Map<CommandClass, MessageHandler> createDelegatingSubscribers() {
        Map<CommandClass, MessageHandler> result = Maps.newHashMap();

        Class<? extends AggregateRoot> rootClass = TypeInfo.getStoredObjectClass(this);
        Set<CommandClass> commandClasses = ServerMethods.getCommandClasses(rootClass);

        MessageHandler subscriber = toMessageSubscriber();
        for (CommandClass commandClass : commandClasses) {
            result.put(commandClass, subscriber);
        }
        return result;
    }

    /**
     * Loads the an aggregate by given id.
     *
     * @param aggregateId id of the aggregate to load
     * @return the loaded object
     * @throws IllegalStateException if the repository wasn't configured prior to calling this method
     */
    @Override
    public R load(I aggregateId) throws IllegalStateException {
        checkConfigured();

        try {
            Snapshot snapshot = eventStore.getLastSnapshot(aggregateId);
            if (snapshot != null) {
                List<EventRecord> trail = eventStore.getEvents(aggregateId, snapshot.getVersion());
                R result = create(aggregateId);
                result.restore(snapshot);
                result.play(trail);
                return result;
            } else {
                List<EventRecord> events = eventStore.getAllEvents(aggregateId);
                R result = create(aggregateId);
                result.play(events);
                return result;
            }
        } catch (InvocationTargetException e) {
            throw propagate(e);
        }
    }

    private void checkConfigured() {
        if (eventStore == null) {
            throw new IllegalStateException("Repository instance is not configured."
                    + "Call the configure() method before trying to load/save the aggregate root.");
        }
    }

    /**
     * Stores the passed aggregate root.
     *
     * @param aggregateRoot an instance to store
     */
    @Override
    public void store(R aggregateRoot) {
        //TODO:2015-09-05:alexander.yevsyukov: It's too late to check it at this stage.
        checkConfigured();

        //TODO:2015-09-05:alexander.yevsyukov: Store snapshots every Xxx messages, which should be configured at the repository's level.

        Snapshot snapshot = aggregateRoot.toSnapshot();

        //noinspection unchecked
        final I aggregateRootId = (I) aggregateRoot.getId();
        eventStore.storeSnapshot(aggregateRootId, snapshot);
    }

    @Override
    public List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException {
        //TODO:2015-09-05:alexander.yevsyukov: Where do we handle a command processed by a repository's method?

        I aggregateId = getAggregateId(command);
        R aggregateRoot = load(aggregateId);

        aggregateRoot.dispatch(command, context);

        //noinspection unchecked
        final List<EventRecord> eventRecords = aggregateRoot.getUncommittedEvents();

        //TODO:2015-06-24:mikhail.melnik: possibly we do not need to store every state change.
        store(aggregateRoot);

        return eventRecords;
    }

    /**
     * Creates, initializes, and stores a new aggregated root.
     * <p>
     * The command is passed to the newly created root with the default state.
     *
     * @param command creation command
     * @param context creation command context
     * @return a list of the event records
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    @Subscribe
    @Override
    public List<EventRecord> handleCreate(C command, CommandContext context) throws InvocationTargetException {
        I id = getAggregateId(command);

        R aggregateRoot = create(id);

        aggregateRoot.dispatch(command, context);

        //noinspection unchecked
        final List<EventRecord> eventRecords = aggregateRoot.getUncommittedEvents();

        store(aggregateRoot);

        return eventRecords;
    }

    @SuppressWarnings("unchecked")
    // We cast to this type because assume that all commands for our aggregate refer to ID of the same type <I>.
    // If this assumption fails, we would get ClassCastException.
    // A better way would be to check all the aggregate commands for the presence of the ID field and
    // correctness of the type on compile-time.
    private I getAggregateId(Message command) {
        return (I) AggregateCommand.getAggregateId(command).value();
    }

    /**
     * Returns a new instance of the aggregate root object
     * with id equals to passed id parameter and all field values set by default,
     *
     * @param id the id of the aggregate root to be created
     * @return the aggregate root instance
     */
    protected R create(I id) {
        try {
            R result = aggregateRootConstructor.newInstance(id);

            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw propagate(e);
        }
    }

}
