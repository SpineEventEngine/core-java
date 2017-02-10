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

package org.spine3.server.procman;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Response;
import org.spine3.server.command.CommandBus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Alexaneder Yevsyukov
 */
public class CommandRouterOnErrorShould extends AbstractCommandRouterShould<CommandRouter> {

    /**
     * Creates a router with mocked {@code CommandBus} which always calls
     * {@link StreamObserver#onError(Throwable) StreamObserver.onError()} when
     * {@link CommandBus#post(Command, StreamObserver) CommandBus.post()} is invoked.
     */
    @Override
    CommandRouter createRouter(CommandBus ignored, Message sourceMessage, CommandContext commandContext) {
        final CommandBus mockBus = mock(CommandBus.class);

        doAnswer(new Answer() {
            @SuppressWarnings("ReturnOfNull") // is OK for Answer
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final StreamObserver<Response> observer = invocation.getArgument(1);
                observer.onError(new RuntimeException("simulate error"));
                return null;
            }
        }).when(mockBus).post(any(Command.class), ArgumentMatchers.<StreamObserver<Response>>any());

        return new CommandRouter(mockBus, sourceMessage, commandContext);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_IllegalStateException_when_caught_error_when_posting() {
        router().routeAll();
    }
}
