/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.examples.aggregate.server;

import com.google.common.base.Function;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.client.CommandRequest;
import org.spine3.eventbus.EventBus;
import org.spine3.examples.aggregate.Client;
import org.spine3.examples.aggregate.OrderId;
import org.spine3.server.BoundedContext;
import org.spine3.server.CommandDispatcher;
import org.spine3.server.CommandStore;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.stream.EventStore;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.spine3.server.util.Identifiers.IdConverterRegistry;
import static org.spine3.server.util.Identifiers.NULL_ID_OR_FIELD;

/**
 * A sample application showing basic usage of the framework.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
public class Application implements AutoCloseable {

    private final StorageFactory storageFactory;
    private final BoundedContext boundedContext;
    private final EventLogger eventLogger = new EventLogger();

    /**
     * Creates a new sample with the specified storage factory.
     * @param storageFactory factory used to create and set up storages.
     */
    public Application(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;

        this.boundedContext = BoundedContext.newBuilder()
                .setStorageFactory(storageFactory)
                .setCommandDispatcher(createCommandDispatcher())
                .setEventBus(createEventBus(storageFactory))
                .build();
    }

    private static CommandDispatcher createCommandDispatcher() {
        return CommandDispatcher.create(new CommandStore(InMemoryStorageFactory.getInstance().createCommandStorage()));
    }

    private static EventBus createEventBus(StorageFactory storageFactory) {
        final EventStore eventStore = EventStore.newBuilder()
                .setStreamExecutor(MoreExecutors.directExecutor())
                .setStorage(storageFactory.createEventStorage())
                .setLogger(EventStore.log())
                .build();

        return EventBus.newInstance(eventStore);
    }

    /**
     * The entry point of the sample.
     * To change the storage implementation, change {@link #getStorageFactory()} method implementation.
     */
    public static void main(String[] args) throws Exception {
        final StorageFactory factory = getStorageFactory();

        try (final Application app = new Application(factory)) {
            app.execute();
        } catch (IOException e) {
            log().error("", e);
        }
    }

    /**
     * Executes the sample: generates some command requests and then the {@link BoundedContext} processes them.
     */
    public void execute() {
        setUp();

        // Generate test requests
        final List<CommandRequest> requests = Client.generateRequests();

        // Process requests
        for (CommandRequest request : requests) {
            boundedContext.process(request);
        }

        log().info("All the requests were handled.");
    }

    /**
     * Sets up the storage, initializes the bounded contexts, registers repositories, handlers etc.
     */
    public void setUp() {
        // Register repository with the bounded context. This will register it in the CommandDispatcher too.
        final OrderRepository repository = new OrderRepository(boundedContext);
        repository.initStorage(storageFactory);

        boundedContext.register(repository);

        // Register event handlers.
        boundedContext.getEventBus().register(eventLogger);

        //TODO:2015-11-10:alexander.yevsyukov: This must be called by the repository or something belonging to business logic.
        // Register id converters
        IdConverterRegistry.getInstance().register(OrderId.class, new OrderIdToStringConverter());
    }

    /**
     * Tear down storages, unregister event handlers and close the bounded context.
     */
    @Override
    public void close() throws Exception {
        boundedContext.close();
    }

    /**
     * Retrieves the storage factory instance.
     * Change this method implementation if needed.
     *
     * @return the {@link StorageFactory} implementation.
     */
    public static StorageFactory getStorageFactory() {
        return org.spine3.server.storage.memory.InMemoryStorageFactory.getInstance();

        /**
         * To run the sample on the file system storage, use the following line instead of one above.
         */
        // return org.spine3.server.storage.filesystem.FileSystemStorageFactory.newInstance(Sample.class);
    }

    public BoundedContext getBoundedContext() {
        return boundedContext;
    }

    private static class OrderIdToStringConverter implements Function<OrderId, String> {

        @Override
        public String apply(@Nullable OrderId orderId) {
            if (orderId == null) {
                return NULL_ID_OR_FIELD;
            }
            final String value = orderId.getValue();
            if (isNullOrEmpty(value) || value.trim().isEmpty()) {
                return NULL_ID_OR_FIELD;
            }
            return value;
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Application.class);
    }
}
