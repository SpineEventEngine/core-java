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

package io.spine.server.event.funnel;

import io.spine.core.Ack;
import io.spine.core.Status;
import io.spine.core.UserId;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.funnel.given.DocumentAggregate;
import io.spine.server.event.funnel.given.DocumentRepository;
import io.spine.server.event.funnel.given.EditHistoryRepository;
import io.spine.time.Now;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.core.EventValidationError.UNSUPPORTED_EVENT;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.core.Status.StatusCase.OK;
import static io.spine.testing.server.entity.EntitySubject.assertEntity;

@DisplayName("EventFunnel should")
class EventFunnelTest {

    private BoundedContext context;
    private DocumentRepository documentRepository;
    private EditHistoryRepository editHistoryRepository;

    @BeforeEach
    void prepareContext() {
        documentRepository = new DocumentRepository();
        editHistoryRepository = new EditHistoryRepository();
        context = BoundedContextBuilder
                .assumingTests()
                .add(documentRepository)
                .add(editHistoryRepository)
                .build();
    }

    @AfterEach
    void closeContext() throws Exception {
        context.close();
    }

    @Nested
    @DisplayName("import events into aggregates")
    class ToAggregate {

        @Test
        @DisplayName("if the applier allows import")
        void markedForImport() {
            MemoizingObserver<Ack> observer = StreamObservers.memoizingObserver();
            UserId johnDoe = UserId
                    .newBuilder()
                    .setValue(newUuid())
                    .build();
            PaperDocumentScanned importEvent = PaperDocumentScanned
                    .newBuilder()
                    .setId(DocumentId.generate())
                    .setText("Quarter report")
                    .setOwner(johnDoe)
                    .setWhenCreated(Now.get().asLocalDateTime())
                    .build();
            context.postEvent()
                   .producedBy(johnDoe)
                   .toAggregate()
                   .with(observer)
                   .post(importEvent);
            Status status = observer.firstResponse()
                                    .getStatus();
            assertWithMessage(status.toString())
                    .that(status.getStatusCase())
                    .isEqualTo(OK);
            Optional<DocumentAggregate> foundDoc = documentRepository.find(importEvent.getId());
            assertThat(foundDoc).isPresent();
            assertEntity(foundDoc.get()).hasStateThat()
                                        .comparingExpectedFieldsOnly()
                                        .isEqualTo(Document.newBuilder()
                                                           .setText(importEvent.getText())
                                                           .buildPartial());
        }

        @Test
        @DisplayName("but NOT if the applier does not allow import")
        void notMarked() {
            MemoizingObserver<Ack> observer = StreamObservers.memoizingObserver();
            UserId johnDoe = UserId
                    .newBuilder()
                    .setValue(newUuid())
                    .build();
            TextEdited importEvent = TextEdited
                    .newBuilder()
                    .setId(DocumentId.generate())
                    .build();
            context.postEvent()
                   .producedBy(johnDoe)
                   .toAggregate()
                   .with(observer)
                   .post(importEvent);
            Ack ack = observer.firstResponse();
            assertThat(ack.getStatus()
                          .getStatusCase())
                    .isEqualTo(ERROR);
            assertThat(ack.getStatus()
                          .getError()
                          .getCode())
                    .isEqualTo(UNSUPPORTED_EVENT.getNumber());
        }
    }
}
