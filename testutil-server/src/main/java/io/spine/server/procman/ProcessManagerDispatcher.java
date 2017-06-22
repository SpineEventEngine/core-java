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
package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.base.Event;
import io.spine.base.EventContext;
import io.spine.envelope.CommandEnvelope;
import io.spine.protobuf.AnyPacker;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A test utility for dispatching commands and events to a {@code ProcessManager} in test purposes.
 *
 * @author Alex Tymchenko
 */
@VisibleForTesting
public class ProcessManagerDispatcher {

    private ProcessManagerDispatcher() {
        // Prevent this utility class from instantiation.
    }

    /**
     * Dispatches the {@linkplain CommandEnvelope Command envelope}
     * to the given {@code ProcessManager}.
     *
     * @return the list of {@linkplain Event events}, being the command output.
     */
    public static List<Event> dispatch(ProcessManager<?, ?, ?> processManager,
                                       CommandEnvelope envelope) {
        checkNotNull(processManager);
        checkNotNull(envelope);

        final ProcManTransaction<?, ?, ?> tx = ProcManTransaction.start(processManager);
        final List<Event> eventMessages = processManager.dispatchCommand(envelope);
        tx.commit();

        return eventMessages;
    }

    /**
     * Dispatches the {@code Event} to the given {@code ProcessManager}.
     */
    public static void dispatch(ProcessManager<?, ?, ?> processManager,
                                Event event) {
        checkNotNull(processManager);
        checkNotNull(event);

        final Message unpackedMessage = AnyPacker.unpack(event.getMessage());
        dispatch(processManager, unpackedMessage, event.getContext());
    }

    /**
     * Dispatches the passed {@code Event} message along with its context
     * to the given {@code ProcessManager}.
     */
    public static void dispatch(ProcessManager<?, ?, ?> processManager,
                                Message eventMessage,
                                EventContext eventContext) {
        checkNotNull(processManager);
        checkNotNull(eventMessage);
        checkNotNull(eventContext);

        final ProcManTransaction<?, ?, ?> tx = ProcManTransaction.start(processManager);
        processManager.dispatchEvent(eventMessage, eventContext);
        tx.commit();
    }
}
