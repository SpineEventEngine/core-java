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

package org.spine3.examples.importado;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.TextFormat;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.base.Response;
import org.spine3.client.CommandFactory;
import org.spine3.client.UserUtil;
import org.spine3.examples.importado.commands.ImportEvents;
import org.spine3.examples.importado.events.TaskCreated;
import org.spine3.examples.importado.events.TaskDone;
import org.spine3.examples.importado.events.WorkStarted;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.event.Subscribe;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.time.ZoneOffsets;
import org.spine3.users.UserId;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import static org.spine3.base.Events.createImportEvent;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.util.Logging.closed;

/**
 * This example constructs a simple application that creates events outside of the
 * {@link TaskAggregate} and passes them to the aggregate via {@link ImportEvents} command.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"}) // Is OK for this example.
public class TaskImporter implements AutoCloseable {

    private final StorageFactory storageFactory;
    private final BoundedContext boundedContext;
    private final EventPrinter printer;

    public TaskImporter(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
        this.boundedContext = BoundedContext.newBuilder()
                                            .setStorageFactory(storageFactory)
                                            .build();
        final TaskRepository repository = new TaskRepository(boundedContext);
        boundedContext.register(repository);
        this.printer = new EventPrinter();
        boundedContext.getEventBus().subscribe(printer);
    }

    public void process(TaskId taskId, Iterable<Event> e) {
        final UserId actor = UserUtil.newUserId(getClass().getSimpleName());
        final CommandFactory commandFactory = CommandFactory.newBuilder()
                                                            .setActor(actor)
                                                            .setZoneOffset(ZoneOffsets.UTC)
                                                            .build();
        final ImportEvents commandMessage = ImportEvents.newBuilder()
                                                        .setId(taskId)
                                                        .addAllEvent(e)
                                                        .build();
        final Command command = commandFactory.create(commandMessage);
        boundedContext.getCommandBus().post(command, new StreamObserver<Response>() {
            @Override
            public void onNext(Response value) {
                System.out.println("Command posted " + TextFormat.shortDebugString(value));
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
            }
        });
    }

    @Override
    public void close() throws Exception {
        printer.close();
        boundedContext.close();
        storageFactory.close();
    }

    /**
     * This class prints events to which it is subscribed via {@code System.out}.
     */
    private static class EventPrinter extends EventSubscriber implements Closeable  {

        @Subscribe
        public void on(TaskCreated event) {
            print(event);
        }

        @Subscribe
        public void on(WorkStarted event) {
            print(event);
        }

        @Subscribe
        public void on(TaskDone event) {
            print(event);
        }

        private static void print(Message event) {
            System.out.println("Got from EventBus: " + event.getClass().getSimpleName() + ": "
                                       + TextFormat.shortDebugString(event));
        }

        @Override
        public void close() throws IOException {
            System.out.println(closed(getClass().getSimpleName()));
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
        /* We use more variables to make the code more readable in this example. This example has the same value for
           all UserIds, while the real import code would create IDs from imported data. */
    private static List<Event> generateEvents(TaskId taskId) {
        final Task task = Task.newBuilder()
                              .setId(taskId)
                              .setName("Show how events can be imported into an aggregate")
                              .build();
        final String className = TaskImporter.class.getSimpleName();

        final UserId taskOwner = UserUtil.newUserId(className);
        final UserId assignee = taskOwner;
        // The ID of the entity, which produces events.
        final StringValue producerId = newStringValue(className);
        final ImmutableList.Builder<Event> builder = ImmutableList.builder();

        final TaskCreated taskCreated = TaskCreated.newBuilder()
                                             .setTask(task)
                                             .build();
        builder.add(createImportEvent(taskCreated, producerId));

        final WorkStarted workStarted = WorkStarted.newBuilder()
                                             .setId(taskId)
                                             .setAssignee(assignee)
                                             .build();
        builder.add(createImportEvent(workStarted, producerId));

        final TaskDone taskDone = TaskDone.newBuilder().setId(taskId).build();
        builder.add(createImportEvent(taskDone, producerId));

        return builder.build();
    }

    public static void main(String[] args) {
        try(TaskImporter importer = new TaskImporter(InMemoryStorageFactory.getInstance())) {
            @SuppressWarnings("MagicNumber") // It is!
            final TaskId taskId = TaskId.newBuilder()
                                        .setNumber(42)
                                        .build();
            final List<Event> events = generateEvents(taskId);
            importer.process(taskId, events);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
