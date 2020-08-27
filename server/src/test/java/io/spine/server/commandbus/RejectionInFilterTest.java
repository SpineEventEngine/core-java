/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.commandbus;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.Ack;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.bus.BusFilter;
import io.spine.server.command.Assign;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.commandbus.CmdBusCaffetteriaId;
import io.spine.test.commandbus.CmdBusCaffetteriaStats;
import io.spine.test.commandbus.CmdBusOrder;
import io.spine.test.commandbus.CmdBusOrderId;
import io.spine.test.commandbus.command.CaffetteriaRejections;
import io.spine.test.commandbus.command.CmdBusAllocateTable;
import io.spine.test.commandbus.command.CmdBusEntryDenied;
import io.spine.test.commandbus.command.Visitors;
import io.spine.test.commandbus.event.CmdBusTableAllocated;
import io.spine.testing.server.blackbox.BlackBoxContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static io.spine.protobuf.AnyPacker.unpack;

@SuppressWarnings("unused") // Reflective access.
@DisplayName("Command bus, when a `Rejection` is thrown from a filter, should")
class RejectionInFilterTest {

    @Test
    @DisplayName("post this rejection to `EventBus`")
    void postRejection() {
        BlackBoxContext context = BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                                     .add(OrderAggregate.class)
                                     .add(new CaffetteriaStatsRepository())
                                     .addCommandFilter(new BeachCustomerFilter())
        );
        CmdBusCaffetteriaId caffetteria = CmdBusCaffetteriaId.generate();
        int allowedCount = 2;
        Visitors visitorsAllowed = Visitors
                .newBuilder()
                .setCount(allowedCount)
                .setBringOwnFood(false)
                .build();
        int deniedCount = 4;
        Visitors visitorsDenied = Visitors
                .newBuilder()
                .setCount(deniedCount)
                .setBringOwnFood(true)
                .build();
        context.receivesCommand(
                CmdBusAllocateTable
                        .newBuilder()
                        .setId(CmdBusOrderId.generate())
                        .setCaffetteria(caffetteria)
                        .setVisitors(visitorsAllowed)
                        .build())
               .receivesCommand(
                       CmdBusAllocateTable
                               .newBuilder()
                               .setId(CmdBusOrderId.generate())
                               .setCaffetteria(caffetteria)
                               .setVisitors(visitorsDenied)
                               .build()
               );
        context.assertEntity(caffetteria, CaffetteriaStats.class)
               .hasStateThat()
               .comparingExpectedFieldsOnly()
               .isEqualTo(
                       CmdBusCaffetteriaStats
                               .newBuilder()
                               .setVisitorCount(allowedCount)
                               .setEntryDenied(deniedCount)
                               .build()
               );
    }

    private static class OrderAggregate
            extends Aggregate<CmdBusOrderId, CmdBusOrder, CmdBusOrder.Builder> {

        @Assign
        CmdBusTableAllocated handle(CmdBusAllocateTable cmd) {
            return CmdBusTableAllocated
                    .newBuilder()
                    .setId(cmd.getId())
                    .setCaffetteria(cmd.getCaffetteria())
                    .setVisitorCount(cmd.getVisitors()
                                        .getCount())
                    .build();
        }

        @Apply
        private void on(CmdBusTableAllocated event) {
            builder().setCaffetteria(event.getCaffetteria());
            // For simplicity.
            builder().setTableIndex(0);
        }
    }

    private static class CaffetteriaStats extends Projection<CmdBusCaffetteriaId,
                                                             CmdBusCaffetteriaStats,
                                                             CmdBusCaffetteriaStats.Builder> {

        @Subscribe
        void on(CmdBusTableAllocated event) {
            builder().setVisitorCount(state().getVisitorCount() + event.getVisitorCount());
        }

        @Subscribe
        void on(CaffetteriaRejections.CmdBusEntryDenied rejection) {
            builder().setEntryDenied(state().getEntryDenied() + rejection.getVisitorCount());
        }
    }

    private static class CaffetteriaStatsRepository
            extends ProjectionRepository<CmdBusCaffetteriaId,
                                         CaffetteriaStats,
                                         CmdBusCaffetteriaStats> {

        @OverridingMethodsMustInvokeSuper
        @Override
        protected void setupEventRouting(EventRouting<CmdBusCaffetteriaId> routing) {
            super.setupEventRouting(routing);
            routing.route(CmdBusTableAllocated.class,
                          (message, context) -> Collections.singleton(message.getCaffetteria()));
            routing.route(CaffetteriaRejections.CmdBusEntryDenied.class,
                          EventRoute.byFirstMessageField(idClass()));
        }
    }

    private static class BeachCustomerFilter implements BusFilter<CommandEnvelope> {

        @Override
        public Optional<Ack> doFilter(CommandEnvelope envelope) {
            CmdBusAllocateTable command =
                    unpack(envelope.command()
                                   .getMessage(), CmdBusAllocateTable.class);
            boolean withOwnFood = command.getVisitors()
                                  .getBringOwnFood();
            if (!withOwnFood) {
                return letPass();
            }
            CmdBusEntryDenied rejection = CmdBusEntryDenied
                    .newBuilder()
                    .setId(command.getCaffetteria())
                    .setVisitorCount(command.getVisitors().getCount())
                    .setReason("The caffetteria doesn't serve clients who bring their own food.")
                    .build();
            RejectionEnvelope cause = RejectionEnvelope.from(envelope, rejection);
            return reject(envelope, cause);
        }
    }
}
