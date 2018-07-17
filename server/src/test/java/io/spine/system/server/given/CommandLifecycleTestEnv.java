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
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.procman.CommandRouted;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.system.server.CommandAcknowledged;
import io.spine.system.server.CommandDispatched;
import io.spine.system.server.CommandErrored;
import io.spine.system.server.CommandHandled;
import io.spine.system.server.CommandReceived;
import io.spine.system.server.CommandRejected;
import io.spine.system.server.Company;
import io.spine.system.server.CompanyEstablished;
import io.spine.system.server.CompanyEstablishing;
import io.spine.system.server.CompanyEstablishingVBuilder;
import io.spine.system.server.CompanyId;
import io.spine.system.server.CompanyNameAlreadyTaken;
import io.spine.system.server.CompanyNameNotProposed;
import io.spine.system.server.CompanyVBuilder;
import io.spine.system.server.EstablishCompany;
import io.spine.system.server.ProposeCompanyName;
import io.spine.system.server.SelectCompanyName;
import io.spine.system.server.StartCompanyEstablishing;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * The environment for the {@link io.spine.system.server.CommandLifecycleAggregate CommandLifecycle}
 * tests.
 *
 * @author Dmytro Dashenkov
 */
public final class CommandLifecycleTestEnv {

    /**
     * Prevents the utility class instantiation.
     */
    private CommandLifecycleTestEnv() {
    }

    public static class CommandLifecycleWatcher extends AbstractEventWatcher {

        @Override
        protected Collection<Class<? extends Message>> getEventClasses() {
            return ImmutableSet.of(CommandReceived.class,
                                   CommandAcknowledged.class,
                                   CommandDispatched.class,
                                   CommandHandled.class,
                                   CommandErrored.class,
                                   CommandRejected.class);
        }
    }

    public static class TestAggregate
            extends Aggregate<CompanyId, Company, CompanyVBuilder> {

        public static final String TAKEN_NAME = "NameIsTaken!";

        private TestAggregate(CompanyId id) {
            super(id);
        }

        @Assign
        CompanyEstablished handle(EstablishCompany command) throws CompanyNameAlreadyTaken {
            if (TAKEN_NAME.equals(command.getFinalName())) {
                throw new CompanyNameAlreadyTaken(getId(), TAKEN_NAME);
            }
            return CompanyEstablished.newBuilder()
                                     .setId(command.getId())
                                     .setName(command.getFinalName())
                                     .build();
        }

        @Apply
        private void on(CompanyEstablished event) {
            getBuilder().setId(event.getId())
                        .setName(event.getName());
        }
    }

    public static class TestProcman
            extends ProcessManager<CompanyId, CompanyEstablishing, CompanyEstablishingVBuilder> {

        private TestProcman(CompanyId id) {
            super(id);
        }

        @Assign
        List<Message> handle(StartCompanyEstablishing command) {
            getBuilder().setId(command.getId());

            return emptyList();
        }

        @Assign
        List<Message> handle(ProposeCompanyName command) {
            getBuilder().addProposedName(command.getName());

            return emptyList();
        }

        @Assign
        CommandRouted handle(SelectCompanyName command, CommandContext context)
                throws CompanyNameNotProposed {
            int index = command.getIndex();
            List<String> proposedNames = getBuilder().getProposedName();
            if (index < 0 || index > proposedNames.size()) {
                throw new CompanyNameNotProposed(getId(), index);
            }
            String finalName = proposedNames.get(index);
            EstablishCompany establishCommand = EstablishCompany
                    .newBuilder()
                    .setId(getBuilder().getId())
                    .setFinalName(finalName)
                    .build();
            return newRouterFor(command, context)
                    .add(establishCommand)
                    .routeAll();
        }
    }

    public static class TestAggregateRepository
            extends AggregateRepository<CompanyId, TestAggregate> {
    }

    public static class TestProcmanRepository
            extends ProcessManagerRepository<CompanyId, TestProcman, CompanyEstablishing> {
    }
}
