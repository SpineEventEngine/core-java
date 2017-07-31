/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.projection;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.TenantId;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.core.given.GivenEvent;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryShould;
import io.spine.server.entity.given.Given;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.GivenEventMessage;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.NoOpTaskNamesRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjection;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjectionRepository;
import io.spine.server.storage.RecordStorage;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjProjectStarted;
import io.spine.test.projection.event.PrjTaskAdded;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static io.spine.Identifier.newUuid;
import static io.spine.test.Verify.assertContainsAll;
import static io.spine.time.Time.getCurrentTime;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class ProjectionRepositoryShould
        extends RecordBasedRepositoryShould<TestProjection, ProjectId, Project> {

    private static final Any PRODUCER_ID = Identifier.pack(GivenEventMessage.ENTITY_ID);

    private BoundedContext boundedContext;

    private ProjectionRepository<ProjectId, TestProjection, Project> repository() {
        return (ProjectionRepository<ProjectId, TestProjection, Project>) repository;
    }

    @Override
    protected RecordBasedRepository<ProjectId, TestProjection, Project> createRepository() {
        return new TestProjectionRepository();
    }

    @Override
    protected TestProjection createEntity() {
        final TestProjection projection = Given.projectionOfClass(TestProjection.class)
                                               .withId(ProjectId.newBuilder()
                                                                .setId(newUuid())
                                                                .build())
                                               .build();
        return projection;
    }

    @Override
    protected List<TestProjection> createEntities(int count) {
        final List<TestProjection> projections = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final TestProjection projection = Given.projectionOfClass(TestProjection.class)
                                                   .withId(createId(i))
                                                   .build();
            projections.add(projection);
        }

        return projections;
    }

    @Override
    protected ProjectId createId(int i) {
        return ProjectId.newBuilder()
                        .setId(format("test-projection-%s", i))
                        .build();
    }

    @Override
    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setName(getClass().getSimpleName())
                                       .setMultitenant(true)
                                       .build();
        super.setUp();

        boundedContext.register(repository);

        TestProjection.clearMessageDeliveryHistory();
    }

    /**
     * Closes the BoundedContest and shuts down the gRPC service.
     *
     * <p>The {@link #tearDown()} method of the super class will be invoked by JUnit automatically
     * after calling this method.
     */
    @After
    public void shutDown() throws Exception {
        boundedContext.close();
    }

    private static TestEventFactory newEventFactory(TenantId tenantId, Any producerId) {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(ProjectionRepositoryShould.class, tenantId);
        return TestEventFactory.newInstance(producerId, requestFactory);
    }

    private static Event createEvent(TenantId tenantId,
                                     Message eventMessage,
                                     Any producerId,
                                     Timestamp when) {
        final Version version = Versions.increment(Versions.zero());
        return newEventFactory(tenantId, producerId).createEvent(eventMessage,
                                                                 version,
                                                                 when);
    }

    /*
     * Tests
     ************/

    @Test
    public void dispatch_event_and_load_projection() {
        checkDispatchesEvent(GivenEventMessage.projectStarted());
    }

    @Test
    public void dispatch_several_events() {
        checkDispatchesEvent(GivenEventMessage.projectCreated());
        checkDispatchesEvent(GivenEventMessage.taskAdded());
        checkDispatchesEvent(GivenEventMessage.projectStarted());
    }

    private void checkDispatchesEvent(Message eventMessage) {
        final TestEventFactory eventFactory = newEventFactory(tenantId(), PRODUCER_ID);
        final Event event = eventFactory.createEvent(eventMessage);

        keepTenantIdFromEvent(boundedContext, event);

        repository().dispatch(EventEnvelope.of(event));
        assertTrue(TestProjection.processed(eventMessage));
    }

    /**
     * Simulates updating TenantIndex, which occurs during command processing
     * in multi-tenant context.
     */
    private static void keepTenantIdFromEvent(BoundedContext boundedContext, Event event) {
        final TenantId tenantId = event.getContext()
                                       .getCommandContext()
                                       .getActorContext()
                                       .getTenantId();
        if (boundedContext.isMultitenant()) {
            boundedContext.getTenantIndex()
                          .keep(tenantId);
        }
    }

    @Test
    public void log_error_if_dispatch_unknown_event() {
        final StringValue unknownEventMessage = StringValue.getDefaultInstance();

        final Event event = GivenEvent.withMessage(unknownEventMessage);

        repository().dispatch(EventEnvelope.of(event));

        TestProjectionRepository testRepo = (TestProjectionRepository)repository();

        assertTrue(testRepo.getLastErrorEnvelope() instanceof EventEnvelope);
        assertEquals(Events.getMessage(event), testRepo.getLastErrorEnvelope()
                                                       .getMessage());
        assertEquals(event, testRepo.getLastErrorEnvelope().getOuterObject());

        // It must be "illegal argument type" since projections of this repository
        // do not handle such events.
        assertTrue(testRepo.getLastException() instanceof IllegalArgumentException);
    }

    @Test
    public void return_event_classes() {
        final Set<EventClass> eventClasses = repository().getMessageClasses();
        assertContainsAll(eventClasses,
                          EventClass.of(PrjProjectCreated.class),
                          EventClass.of(PrjTaskAdded.class),
                          EventClass.of(PrjProjectStarted.class));
    }

    @Test
    public void return_entity_storage() {
        final RecordStorage<ProjectId> recordStorage = repository().recordStorage();
        assertNotNull(recordStorage);
    }

    @Test
    public void convert_null_timestamp_to_default() {
        final Timestamp timestamp = getCurrentTime();
        assertEquals(timestamp, ProjectionRepository.nullToDefault(timestamp));
        assertEquals(Timestamp.getDefaultInstance(), ProjectionRepository.nullToDefault(null));
    }

    @Test
    public void do_not_create_record_if_entity_is_not_updated() {
        final NoOpTaskNamesRepository repo = new NoOpTaskNamesRepository();
        boundedContext.register(repo);

        assertFalse(repo.loadAll()
                        .hasNext());

        final Event event = createEvent(tenantId(),
                                        GivenEventMessage.projectCreated(),
                                        PRODUCER_ID,
                                        getCurrentTime());
        repo.dispatch(EventEnvelope.of(event));

        final Iterator<?> items = repo.loadAll();
        assertFalse(items.hasNext());
    }

    /**
     * Ensures that {@link ProjectionRepository#readLastHandledEventTime()} and
     * {@link ProjectionRepository#writeLastHandledEventTime(Timestamp)} which are used by
     * Beam-based catch-up are exposed.
     */
    @Test
    public void expose_read_and_write_methods_for_last_handled_event_timestamp() {
        final Timestamp timestamp = repository().readLastHandledEventTime();
        if (timestamp != null) {
            repository().writeLastHandledEventTime(timestamp);
        }
    }

    /**
     * Ensures that {@link ProjectionRepository#createStreamQuery()}, which is used by the catch-up
     * procedures is exposed.
     */
    @Test
    public void crete_stream_query() {
        assertNotNull(repository().createStreamQuery());
    }

    /**
     * Ensures that {@link ProjectionRepository#getEventStore()} which is used by the catch-up
     * functionality is exposed to the package.
     */
    @Test
    public void expose_event_store_to_package() {
        assertNotNull(repository().getEventStore());
    }

    /**
     * Ensures that {@link ProjectionRepository#boundedContext()} which is used by the catch-up
     * functionality is exposed to the package.
     */
    @Test
    public void expose_bounded_context_to_package() {
        assertNotNull(repository().boundedContext());
    }
}
