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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregatePart;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.AggregateRoot;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.EventSubscriber;
import io.spine.server.procman.ProcessManager;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.system.server.CommandDispatchedToHandler;
import io.spine.system.server.CreatePerson;
import io.spine.system.server.CreatePersonName;
import io.spine.system.server.EntityArchived;
import io.spine.system.server.EntityCreated;
import io.spine.system.server.EntityDeleted;
import io.spine.system.server.EntityExtractedFromArchive;
import io.spine.system.server.EntityRestored;
import io.spine.system.server.EntityStateChanged;
import io.spine.system.server.EventDispatchedToApplier;
import io.spine.system.server.EventDispatchedToReactor;
import io.spine.system.server.EventDispatchedToSubscriber;
import io.spine.system.server.Person;
import io.spine.system.server.PersonCreated;
import io.spine.system.server.PersonCreation;
import io.spine.system.server.PersonCreationVBuilder;
import io.spine.system.server.PersonFirstName;
import io.spine.system.server.PersonFirstNameVBuilder;
import io.spine.system.server.PersonNameCreated;
import io.spine.system.server.PersonVBuilder;
import io.spine.system.server.PersonView;
import io.spine.system.server.PersonViewVBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;

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

        public List<? extends Message> events() {
            return ImmutableList.copyOf(events);
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
        public void on(EventDispatchedToApplier event) {
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
    }

    public static class TestAggregate extends Aggregate<String, Person, PersonVBuilder> {

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

        @Apply
        private void on(PersonCreated event) {
            getBuilder().setId(event.getId())
                        .setName(event.getName());
        }
    }

    public static class TestProjection extends Projection<String, PersonView, PersonViewVBuilder> {

        protected TestProjection(String id) {
            super(id);
        }

        @Subscribe
        public void on(PersonCreated event) {
            getBuilder().setId(event.getId())
                        .setName(event.getName());
        }
    }

    public static class TestProcman extends ProcessManager<String,
                                                           PersonCreation,
                                                           PersonCreationVBuilder> {

        protected TestProcman(String id) {
            super(id);
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
        protected TestAggregatePart(TestAggregateRoot root) {
            super(root);
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

    public static class TestAggregatePartRepository extends AggregateRepository<String,
                                                                                TestAggregatePart> {
    }
}
