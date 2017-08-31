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

package io.spine.model.verify.given;

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.event.EventBus;
import io.spine.server.procman.ProcessManager;
import io.spine.validate.AnyVBuilder;

import java.util.Collections;

/**
 * @author Dmytro Dashenkov
 */
public class Given {

    private Given() {
        // Prevent utility class instantiation.
    }

    public static class TestCommandHandler extends CommandHandler {

        protected TestCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("unused") // Used for the Model construction
        @Assign
        Iterable<Message> handle(Any command) {
            // NoOp for test
            return Collections.emptySet();
        }
    }

    public static class TestAggregate extends Aggregate<String, Any, AnyVBuilder> {

        protected TestAggregate(String id) {
            super(id);
        }

        @SuppressWarnings("unused") // Used for the Model construction
        @Assign
        Iterable<Message> handle(Int32Value command) {
            // NoOp for test
            return Collections.emptySet();
        }
    }

    public static class TestProcMan extends ProcessManager<String, Any, AnyVBuilder> {

        protected TestProcMan(String id) {
            super(id);
        }

        @SuppressWarnings("unused") // Used for the Model construction
        @Assign
        Iterable<Message> handle(Int64Value command) {
            // NoOp for test
            return Collections.emptySet();
        }
    }

    public static class DuplicateCommandHandler extends CommandHandler {

        protected DuplicateCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("unused") // Used for the Model construction
        @Assign
        Iterable<Message> handle(Any command) {
            // NoOp for test
            return Collections.emptySet();

        }
    }
}
