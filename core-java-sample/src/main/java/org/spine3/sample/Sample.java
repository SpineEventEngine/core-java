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

package org.spine3.sample;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandRequest;
import org.spine3.base.UserId;
import org.spine3.eventbus.EventBus;
import org.spine3.sample.order.OrderId;
import org.spine3.sample.order.OrderRepository;
import org.spine3.server.Engine;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.datastore.LocalDatastoreStorageFactory;
import org.spine3.server.storage.filesystem.FileSystemStorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.util.Users;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.spine3.util.Identifiers.IdConverterRegistry;
import static org.spine3.util.Identifiers.NULL_ID_OR_FIELD;

/**
 * Framework usage sample.
 * <p>
 * To change storage implementation, just pass another {@link Sample.StorageType} parameter
 * to {@link Sample#getStorageFactory(Sample.StorageType)} in {@link Sample#main(String[])}.
 * <p>
 * <b>NOTE:</b> see the <a href="https://github.com/SpineEventEngine/core-java/wiki/Setup-the-GAE-Local-Datastore-Environment">
 *     instructions</a> on how to run on GAE local Datastore.
 *
 * @see Sample#getStorageFactory(Sample.StorageType)
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
public class Sample {

    private final StorageFactory storageFactory;
    private final EventLogger eventLogger = new EventLogger();

    /**
     * Creates a new sample with the specified storage factory.
     * @param storageFactory factory used to create and set up storages.
     */
    public Sample(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
    }

    /**
     * The entry point of the sample.
     */
    public static void main(String[] args) {

        final StorageFactory factory = getStorageFactory(StorageType.IN_MEMORY);

        Sample sample = new Sample(factory);

        sample.execute();
    }

    /**
     * Executes the sample: generates some command requests and then the {@link Engine} processes them.
     */
    protected void execute() {

        setUp();

        // Generate test requests
        List<CommandRequest> requests = generateRequests();

        // Process requests
        for (CommandRequest request : requests) {
            Engine.getInstance().process(request);
        }

        log().info("All the requests were handled.");

        tearDown();
    }

    /**
     * Sets up the storage, starts the engine, registers repositories, handlers etc.
     */
    public void setUp() {

        // Set up the storage
        storageFactory.setUp();

        // Start the engine
        Engine.start(storageFactory);

        // Register repository with the engine. This will register it in the CommandDispatcher too.
        Engine.getInstance().register(new OrderRepository());

        // Register event handlers
        EventBus.getInstance().register(eventLogger);

        // Register id converters
        IdConverterRegistry.getInstance().register(OrderId.class, new OrderIdToStringConverter());
    }

    /**
     * Tear down storages, unregister event handlers and stop the engine.
     */
    public void tearDown() {

        storageFactory.tearDown();

        // Unregister event handlers
        EventBus.getInstance().unregister(eventLogger);

        // Stop the engine
        Engine.stop();
    }

    //TODO:2015-09-23:alexander.yevsyukov: Rename and extend sample data to better reflect the problem domain.
    // See Amazon screens for correct naming of domain things.

    /**
     * Creates several dozens of requests.
     */
    public static List<CommandRequest> generateRequests() {

        List<CommandRequest> result = Lists.newArrayList();

        for (int i = 0; i < 10; i++) {

            OrderId orderId = OrderId.newBuilder().setValue(String.valueOf(i)).build();
            UserId userId = Users.createId("user_" + i);

            CommandRequest createOrder = Requests.createOrder(userId, orderId);
            CommandRequest addOrderLine = Requests.addOrderLine(userId, orderId);
            CommandRequest payForOrder = Requests.payForOrder(userId, orderId);

            result.add(createOrder);
            result.add(addOrderLine);
            result.add(payForOrder);
        }

        return result;
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

    /**
     * Retrieves a {@link StorageFactory} implementation depending on the passed parameter.
     *
     * @param storageType the type of storage to retrieve.
     * @see Sample.StorageType
     */
    public static StorageFactory getStorageFactory(StorageType storageType) {

        switch (storageType) {
            case IN_MEMORY:
                return InMemoryStorageFactory.getInstance();
            case FILE_SYSTEM:
                return FileSystemStorageFactory.newInstance(Sample.class);
            case LOCAL_DATASTORE:
                return LocalDatastoreStorageFactory.getInstance();
            default:
                throw new IllegalArgumentException("Unknown storage type: " + storageType);
        }
    }

    /**
     * Storage implementation types.
     */
    public static enum StorageType {

        /**
         * In-memory based storage.
         */
        IN_MEMORY,

        /**
         * Filesystem based storage.
         */
        FILE_SYSTEM,

        /**
         * GAE local Datastore based storage.
         */
        LOCAL_DATASTORE
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Sample.class);

    }
}
