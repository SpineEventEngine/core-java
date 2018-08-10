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
import io.spine.server.command.Command;
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
import io.spine.system.server.CompanyEstablishingStarted;
import io.spine.system.server.CompanyEstablishingVBuilder;
import io.spine.system.server.CompanyId;
import io.spine.system.server.CompanyNameAlreadyTaken;
import io.spine.system.server.CompanyNameRethought;
import io.spine.system.server.CompanyVBuilder;
import io.spine.system.server.EstablishCompany;
import io.spine.system.server.FinalizeCompanyName;
import io.spine.system.server.ProposeCompanyName;
import io.spine.system.server.StartCompanyEstablishing;
import io.spine.system.server.TargetAssignedToCommand;
import io.spine.type.TypeUrl;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;

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

    public static class CommandLifecycleWatcher extends AbstractEventAccumulator {

        @Override
        protected Collection<Class<? extends Message>> getEventClasses() {
            return ImmutableSet.of(CommandReceived.class,
                                   CommandAcknowledged.class,
                                   CommandDispatched.class,
                                   TargetAssignedToCommand.class,
                                   CommandHandled.class,
                                   CommandErrored.class,
                                   CommandRejected.class);
        }
    }

    public static class TestAggregate
            extends Aggregate<CompanyId, Company, CompanyVBuilder> {

        public static final TypeUrl TYPE = TypeUrl.of(Company.class);
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

        public static final TypeUrl TYPE = TypeUrl.of(CompanyEstablishing.class);
        public static final String FAULTY_NAME = "This name is exceptionally faulty";

        @Assign
        CompanyEstablishingStarted handle(StartCompanyEstablishing command) {
            getBuilder().setId(command.getId());

            return CompanyEstablishingStarted.newBuilder()
                                             .setId(command.getId())
                                             .build();
        }

        @Assign
        CompanyNameRethought handle(ProposeCompanyName command) {
            String name = command.getName();
            checkArgument(!name.equals(FAULTY_NAME));
            getBuilder().setProposedName(name);

            return CompanyNameRethought.newBuilder()
                                       .setId(command.getId())
                                       .setName(command.getName())
                                       .build();
        }

        @Command
        EstablishCompany transform(FinalizeCompanyName command, CommandContext context) {
            String name = getBuilder().getProposedName();
            EstablishCompany establishCommand = EstablishCompany
                    .newBuilder()
                    .setId(getBuilder().getId())
                    .setFinalName(name)
                    .build();
            return establishCommand;
        }
    }

    public static class TestAggregateRepository
            extends AggregateRepository<CompanyId, TestAggregate> {
    }

    public static class TestProcmanRepository
            extends ProcessManagerRepository<CompanyId, TestProcman, CompanyEstablishing> {
    }
}
