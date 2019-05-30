/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.aggregate.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.validate.ConstraintViolation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;

/**
 * Test environment aggregate for {@link io.spine.server.aggregate.AggregateTransactionTest}.
 */
public class TxTestAggregate
        extends Aggregate<ProjectId, Project, Project.Builder> {

    private final List<Message> receivedEvents = newArrayList();
    private final List<ConstraintViolation> violations;

    public TxTestAggregate(ProjectId id) {
        this(id, null);
    }

    public TxTestAggregate(ProjectId id, @Nullable List<ConstraintViolation> violations) {
        super(id);
        this.violations = violations;
    }

    @Override
    protected List<ConstraintViolation> checkEntityState(Project newState) {
        if (violations != null) {
            return ImmutableList.copyOf(violations);
        }
        return super.checkEntityState(newState);
    }

    @Assign
    AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
        return projectCreated(cmd.getProjectId(), cmd.getName());
    }

    @Apply
    private void event(AggProjectCreated event) {
        receivedEvents.add(event);
        Project newState = Project
                .newBuilder(state())
                .setId(event.getProjectId())
                .setName(event.getName())
                .build();
        builder().mergeFrom(newState);
    }

    @Apply
    @SuppressWarnings("MethodMayBeStatic")
    private void event(AggTaskAdded event) {
        throw new RuntimeException("that tests the tx behaviour");
    }

    public List<Message> receivedEvents() {
        return ImmutableList.copyOf(receivedEvents);
    }
}
