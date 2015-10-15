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
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.util.Users;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.spine3.util.Identifiers.IdConverterRegistry;
import static org.spine3.util.Identifiers.NULL_ID_OR_FIELD;

/**
 * Framework sample. To change storage implementation, see instructions under {@code storageFactory} field.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
public class Sample {

    public static final String SUCCESS_MESSAGE = "All the requests were handled.";

    @SuppressWarnings({"FieldMayBeFinal", "StaticNonFinalField"})
    private static StorageFactory storageFactory = InMemoryStorageFactory.getInstance();

    /**
     * To run the sample on a FileSystemStorageFactory, replace the above initialization with the following:
     *
     * StorageFactory storageFactory = FileSystemStorageFactory.newInstance(MySampleClass.class);
     *
     *
     * To run the sample on a LocalDatastoreStorageFactory, replace the above initialization with the following:
     *
     * StorageFactory storageFactory = LocalDatastoreStorageFactory.newInstance();
     */

    public static void main(String[] args) {

        setUpEnvironment(storageFactory);

        // Generate test requests
        List<CommandRequest> requests = generateRequests();

        // Process requests
        for (CommandRequest request : requests) {
            Engine.getInstance().process(request);
        }

        log().info(SUCCESS_MESSAGE);

        tearDownEnvironment(storageFactory);
    }

    public static void setUpEnvironment(StorageFactory storageFactory) {

        // Set up the storage
        storageFactory.setUp();

        // Start the engine
        Engine.start(storageFactory);

        // Register repository with the engine. This will register it in the CommandDispatcher too.
        Engine.getInstance().register(new OrderRepository());

        // Register event handlers
        EventBus.getInstance().register(new EventLogger());

        // Register id converters
        IdConverterRegistry.instance().register(OrderId.class, new OrderIdToStringConverter());
    }

    public static void tearDownEnvironment(StorageFactory storageFactory) {
        storageFactory.tearDown();
        EventBus.getInstance().unregister(log());
        Engine.stop();
    }

    //TODO:2015-09-23:alexander.yevsyukov: Rename and extend sample data to better reflect the problem domain.
    // See Amazon screens for correct naming of domain things.

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

    public static void setStorageFactory(StorageFactory storageFactory) {
        Sample.storageFactory = storageFactory;
    }

    private Sample() {
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Sample.class);

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
}
