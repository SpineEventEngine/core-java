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

package io.spine.server.event;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.EventValidationError;
import io.spine.core.MessageInvalid;
import io.spine.test.event.ProjectCreated;
import io.spine.testdata.Sample;
import io.spine.testing.server.command.TestEventFactory;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.MessageValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("EventValidator should")
class EventValidatorTest {

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(EventValidatorTest.class);

    @Test
    @DisplayName("validate event messages")
    void validateEventMessages() {
        final MessageValidator messageValidator = mock(MessageValidator.class);
        when(messageValidator.validate(any(Message.class)))
                .thenReturn(newArrayList(ConstraintViolation.getDefaultInstance(),
                                         ConstraintViolation.getDefaultInstance()));
        final Event event = eventFactory.createEvent(Sample.messageOfType(ProjectCreated.class));

        final EventValidator eventValidator = new EventValidator(messageValidator);

        final Optional<MessageInvalid> error = eventValidator.validate(EventEnvelope.of(event));
        assertTrue(error.isPresent());
        final Error actualError = error.get().asError();
        assertEquals(EventValidationError.getDescriptor().getFullName(), actualError.getType());
    }
}
