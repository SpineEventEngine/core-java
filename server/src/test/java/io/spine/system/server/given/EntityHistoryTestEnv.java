/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.system.server.given;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.React;
import io.spine.core.Subscribe;
import io.spine.people.PersonName;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregatePartRepository;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.EventSubscriber;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.system.server.CommandDispatchedToHandler;
import io.spine.system.server.CompletePersonCreation;
import io.spine.system.server.CreatePerson;
import io.spine.system.server.CreatePersonName;
import io.spine.system.server.EntityArchived;
import io.spine.system.server.EntityCreated;
import io.spine.system.server.EntityDeleted;
import io.spine.system.server.EntityExtractedFromArchive;
import io.spine.system.server.EntityRestored;
import io.spine.system.server.EntityStateChanged;
import io.spine.system.server.EventDispatchedToReactor;
import io.spine.system.server.EventDispatchedToSubscriber;
import io.spine.system.server.EventPassedToApplier;
import io.spine.system.server.HidePerson;
import io.spine.system.server.Person;
import io.spine.system.server.PersonCreated;
import io.spine.system.server.PersonCreation;
import io.spine.system.server.PersonCreationVBuilder;
import io.spine.system.server.PersonFirstName;
import io.spine.system.server.PersonFirstNameVBuilder;
import io.spine.system.server.PersonHidden;
import io.spine.system.server.PersonNameCreated;
import io.spine.system.server.PersonRenamed;
import io.spine.system.server.PersonUnHidden;
import io.spine.system.server.PersonVBuilder;
import io.spine.system.server.PersonView;
import io.spine.system.server.PersonViewVBuilder;
import io.spine.system.server.RenamePerson;
import io.spine.system.server.StartPersonCreation;
import io.spine.system.server.UnHidePerson;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public final class EntityHistoryTestEnv {

    /**
     * Prevents the utility class instantiation.
     */
    private EntityHistoryTestEnv() {
    }
    
    public static class HistoryEventSubscriber extends EventSubscriber {

        private final List<Message> events = newLinkedList();

        private @Nullable Iterator<? extends Message> eventIterator;

        public void assertEventCount(int expectedCount) {
            assertEquals(expectedCount, events.size(), errorMessage());
        }

        public void clearEvents() {
            events.clear();
            eventIterator = null;
        }

        @CanIgnoreReturnValue
        public <E extends Message> E nextEvent(Class<E> eventType) {
            if (eventIterator == null) {
                eventIterator = copyOf(events).iterator();
            }
            assertTrue(eventIterator.hasNext(), errorMessage());
            Message next = eventIterator.next();
            assertThat(next, instanceOf(eventType));
            @SuppressWarnings("unchecked")
            E result = (E) next;
            return result;
        }

        @Subscribe
        public void on(EntityCreated event) {
            events.add(event);
        }

        @Subscribe
        public void on(EventDispatchedToSubscriber event) {
            events.add(event);
        }

        @Subscribe
        public void on(EventDispatchedToReactor event) {
            events.add(event);
        }

        @Subscribe
        public void on(EventPassedToApplier event) {
            events.add(event);
        }

        @Subscribe
        public void on(CommandDispatchedToHandler event) {
            events.add(event);
        }

        @Subscribe
        public void on(EntityStateChanged event) {
            events.add(event);
        }

        @Subscribe
        public void on(EntityArchived event) {
            events.add(event);
        }

        @Subscribe
        public void on(EntityDeleted event) {
            events.add(event);
        }

        @Subscribe
        public void on(EntityExtractedFromArchive event) {
            events.add(event);
        }

        @Subscribe
        public void on(EntityRestored event) {
            events.add(event);
        }

        private String errorMessage() {
            return format("Actual events are: %s", events.stream()
                                                         .map(Object::getClass)
                                                         .map(Class::getSimpleName)
                                                         .collect(joining(" -> ")));
        }
    }

    public static class TestAggregate extends Aggregate<String, Person, PersonVBuilder> {

        public static final TypeUrl TYPE = TypeUrl.of(Person.class);

        protected TestAggregate(String id) {
            super(id);
        }

        @Assign
        PersonCreated handle(CreatePerson command) {
            return PersonCreated.newBuilder()
                                .setId(command.getId())
                                .setName(command.getName())
                                .build();
        }

        @Assign
        PersonHidden handle(HidePerson command) {
            return PersonHidden.newBuilder()
                               .setId(command.getId())
                               .build();
        }

        @Assign
        PersonUnHidden handle(UnHidePerson command) {
            return PersonUnHidden.newBuilder()
                                 .setId(command.getId())
                                 .build();
        }

        @Assign
        PersonRenamed handle(RenamePerson command) {
            return PersonRenamed.newBuilder()
                                .setId(command.getId())
                                .setNewFirstName(command.getNewFirstName())
                                .build();
        }

        @Apply
        private void on(PersonCreated event) {
            getBuilder().setId(event.getId())
                        .setName(event.getName());
        }

        @Apply
        private void on(PersonHidden event) {
            setArchived(true);
        }

        @Apply
        private void on(PersonUnHidden event) {
            setArchived(false);
        }

        @Apply
        private void on(PersonRenamed event) {
            PersonName newName = getBuilder().getName()
                                             .toBuilder()
                                             .setGivenName(event.getNewFirstName())
                                             .build();
            getBuilder().setName(newName);
        }
    }

    public static class TestProjection extends Projection<String, PersonView, PersonViewVBuilder> {

        public static final TypeUrl TYPE = TypeUrl.of(PersonView.class);

        protected TestProjection(String id) {
            super(id);
        }

        @Subscribe
        public void on(PersonCreated event) {
            getBuilder().setId(event.getId())
                        .setName(event.getName());
        }

        @Subscribe
        public void on(PersonHidden event) {
            setDeleted(true);
        }

        @Subscribe
        public void on(PersonUnHidden event) {
            setDeleted(false);
        }
    }

    public static class TestProcman extends ProcessManager<String,
                                                           PersonCreation,
                                                           PersonCreationVBuilder> {

        public static final TypeUrl TYPE = TypeUrl.of(PersonCreation.class);

        protected TestProcman(String id) {
            super(id);
        }

        @Assign
        Empty handle(StartPersonCreation command) {
            getBuilder().setId(command.getId());
            return Empty.getDefaultInstance();
        }

        @Assign
        Empty handle(CompletePersonCreation command) {
            getBuilder().setId(command.getId())
                        .setCreated(true);
            return Empty.getDefaultInstance();
        }

        @React
        Empty reactOn(PersonNameCreated event) {
            getBuilder().setId(event.getId())
                        .setCreated(true);
            return Empty.getDefaultInstance();
        }
    }

    public static class TestAggregateRoot extends AggregateRoot<String> {

        protected TestAggregateRoot(BoundedContext boundedContext, String id) {
            super(boundedContext, id);
        }
    }

    public static class TestAggregatePart extends AggregatePart<String,
                                                                PersonFirstName,
                                                                PersonFirstNameVBuilder,
                                                                TestAggregateRoot> {

        public static final TypeUrl TYPE = TypeUrl.of(PersonFirstName.class);
        
        private TestAggregatePart(TestAggregateRoot root) {
            super(root);
        }

        @React
        Empty reactOn(PersonRenamed event) {
            return Empty.getDefaultInstance();
        }

        @Assign
        PersonNameCreated handle(CreatePersonName command) {
            return PersonNameCreated.newBuilder()
                                    .setId(command.getId())
                                    .setFirstName(command.getFirstName())
                                    .build();
        }

        @Apply
        private void on(PersonNameCreated event) {
            getBuilder().setId(event.getId())
                        .setFirstName(event.getFirstName());
        }
    }

    public static class TestAggregateRepository extends AggregateRepository<String, TestAggregate> {
    }

    public static class TestProjectionRepository extends ProjectionRepository<String,
                                                                              TestProjection,
                                                                              PersonView> {
    }

    public static class TestAggregatePartRepository
            extends AggregatePartRepository<String, TestAggregatePart, TestAggregateRoot> {
    }

    public static class TestProcmanRepository
            extends ProcessManagerRepository<String, TestProcman, PersonCreation>  {
    }
}
