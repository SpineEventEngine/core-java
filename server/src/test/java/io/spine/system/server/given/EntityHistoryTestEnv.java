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

import com.google.common.collect.ImmutableSet;
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
import io.spine.system.server.ExposePerson;
import io.spine.system.server.HidePerson;
import io.spine.system.server.Person;
import io.spine.system.server.PersonCreated;
import io.spine.system.server.PersonCreation;
import io.spine.system.server.PersonCreationCompleted;
import io.spine.system.server.PersonCreationStarted;
import io.spine.system.server.PersonCreationVBuilder;
import io.spine.system.server.PersonDetails;
import io.spine.system.server.PersonDetailsVBuilder;
import io.spine.system.server.PersonExposed;
import io.spine.system.server.PersonFirstName;
import io.spine.system.server.PersonFirstNameVBuilder;
import io.spine.system.server.PersonHidden;
import io.spine.system.server.PersonId;
import io.spine.system.server.PersonNameCreated;
import io.spine.system.server.PersonRenamed;
import io.spine.system.server.PersonVBuilder;
import io.spine.system.server.RenamePerson;
import io.spine.system.server.StartPersonCreation;
import io.spine.type.TypeUrl;

import java.util.Collection;

/**
 * @author Dmytro Dashenkov
 */
public final class EntityHistoryTestEnv {

    /**
     * Prevents the utility class instantiation.
     */
    private EntityHistoryTestEnv() {
    }

    public static class HistoryEventWatcher extends AbstractEventAccumulator {

        @Override
        public Collection<Class<? extends Message>> getEventClasses() {
            return ImmutableSet.of(EntityCreated.class,
                                   EventDispatchedToSubscriber.class,
                                   EventDispatchedToReactor.class,
                                   CommandDispatchedToHandler.class,
                                   EntityStateChanged.class,
                                   EntityArchived.class,
                                   EntityDeleted.class,
                                   EntityExtractedFromArchive.class,
                                   EntityRestored.class);
        }
    }

    public static class TestAggregate extends Aggregate<PersonId, Person, PersonVBuilder> {

        public static final TypeUrl TYPE = TypeUrl.of(Person.class);

        protected TestAggregate(PersonId id) {
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
        PersonExposed handle(ExposePerson command) {
            return PersonExposed.newBuilder()
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
        private void on(PersonExposed event) {
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

    public static class TestProjection
            extends Projection<PersonId, PersonDetails, PersonDetailsVBuilder> {

        public static final TypeUrl TYPE = TypeUrl.of(PersonDetails.class);

        protected TestProjection(PersonId id) {
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
        public void on(PersonExposed event) {
            setDeleted(false);
        }
    }

    public static class TestProcman
            extends ProcessManager<PersonId, PersonCreation, PersonCreationVBuilder> {

        public static final TypeUrl TYPE = TypeUrl.of(PersonCreation.class);

        protected TestProcman(PersonId id) {
            super(id);
        }

        @Assign
        PersonCreationStarted handle(StartPersonCreation command) {
            getBuilder().setId(command.getId());

            PersonCreationStarted event = PersonCreationStarted
                    .newBuilder()
                    .setId(command.getId())
                    .build();
            return event;
        }

        @Assign
        PersonCreationCompleted handle(CompletePersonCreation command) {
            getBuilder().setId(command.getId())
                        .setCreated(true);

            PersonCreationCompleted event = PersonCreationCompleted
                    .newBuilder()
                    .setId(command.getId())
                    .build();
            return event;
        }

        @React
        Empty reactOn(PersonNameCreated event) {
            getBuilder().setId(event.getId())
                        .setCreated(true);
            return Empty.getDefaultInstance();
        }
    }

    public static class TestAggregateRoot extends AggregateRoot<PersonId> {

        protected TestAggregateRoot(BoundedContext boundedContext, PersonId id) {
            super(boundedContext, id);
        }
    }

    public static class TestAggregatePart extends AggregatePart<PersonId,
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

    public static class TestAggregateRepository
            extends AggregateRepository<PersonId, TestAggregate> {
    }

    public static class TestProjectionRepository
            extends ProjectionRepository<PersonId, TestProjection, PersonDetails> {
    }

    public static class TestAggregatePartRepository
            extends AggregatePartRepository<PersonId, TestAggregatePart, TestAggregateRoot> {
    }

    public static class TestProcmanRepository
            extends ProcessManagerRepository<PersonId, TestProcman, PersonCreation> {
    }
}
