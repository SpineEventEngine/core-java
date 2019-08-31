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

import com.google.common.collect.ImmutableList;
import io.spine.core.Ack;
import io.spine.core.Status;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.grpc.MemoizingObserver;
import io.spine.net.InternetDomain;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.UnsupportedEventException;
import io.spine.server.event.funnel.given.DocumentAggregate;
import io.spine.server.event.funnel.given.DocumentRepository;
import io.spine.server.event.funnel.given.EditHistoryProjection;
import io.spine.server.event.funnel.given.EditHistoryRepository;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.time.Now;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.integration.ExternalMessageValidationError.UNSUPPORTED_EXTERNAL_MESSAGE;
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
            MemoizingObserver<Ack> observer = memoizingObserver();
            UserId johnDoe = userId();
            PaperDocumentScanned importEvent = PaperDocumentScanned
                    .newBuilder()
                    .setId(DocumentId.generate())
                    .setText("Quarter report")
                    .setOwner(johnDoe)
                    .setWhenCreated(Now.get().asLocalDateTime())
                    .build();
            context.postEvents()
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
            MemoizingObserver<Ack> observer = memoizingObserver();
            UserId johnDoe = userId();
            TextEdited importEvent = TextEdited
                    .newBuilder()
                    .setId(DocumentId.generate())
                    .build();
            context.postEvents()
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

        @Test
        @DisplayName("in a multitenant environment")
        void multitenant() {
            DocumentRepository documentRepository = new DocumentRepository();
            BoundedContext context = BoundedContextBuilder
                    .assumingTests(true)
                    .add(documentRepository)
                    .build();
            UserId johnDoe = userId();
            TenantId acmeCorp = TenantId
                    .newBuilder()
                    .setDomain(InternetDomain.newBuilder().setValue("acme.com"))
                    .build();
            TenantId cyberdyne = TenantId
                    .newBuilder()
                    .setDomain(InternetDomain.newBuilder().setValue("cyberdyne.com"))
                    .build();
            PaperDocumentScanned importEvent = PaperDocumentScanned
                    .newBuilder()
                    .setId(DocumentId.generate())
                    .setText("Monthly report")
                    .setOwner(johnDoe)
                    .setWhenCreated(Now.get().asLocalDateTime())
                    .build();
            context.postEvents()
                   .producedBy(johnDoe)
                   .toAggregate()
                   .forTenant(acmeCorp)
                   .post(importEvent);

            Optional<DocumentAggregate> acmeMonthlyReport = TenantAwareRunner
                    .with(acmeCorp)
                    .evaluate(() -> documentRepository.find(importEvent.getId()));
            assertThat(acmeMonthlyReport).isPresent();

            Optional<DocumentAggregate> cyberdyneMonthlyReport = TenantAwareRunner
                    .with(cyberdyne)
                    .evaluate(() -> documentRepository.find(importEvent.getId()));
            assertThat(cyberdyneMonthlyReport).isEmpty();
        }
    }

    @Nested
    @DisplayName("broadcast events via IntegrationBus")
    class Broadcast {

        @Test
        @DisplayName("and deliver to external reactors")
        void externalReactor() {
            MemoizingObserver<Ack> observer = memoizingObserver();
            UserId johnDoe = userId();
            OpenOfficeDocumentUploaded importEvent = OpenOfficeDocumentUploaded
                    .newBuilder()
                    .setId(DocumentId.generate())
                    .setText("The scary truth about gluten")
                    .build();
            context.postEvents()
                   .producedBy(johnDoe)
                   .broadcast()
                   .with(observer)
                   .post(importEvent);
            Status status = observer.firstResponse()
                                    .getStatus();
            assertWithMessage(status.toString())
                    .that(status.getStatusCase())
                    .isEqualTo(OK);
            Optional<DocumentAggregate> foundDoc = documentRepository.find(importEvent.getId());
            assertThat(foundDoc).isPresent();
            assertThat(foundDoc.get()
                               .state()
                               .getText())
                    .isEqualTo(importEvent.getText());
        }

        @Test
        @DisplayName("and ignore domestic reactors")
        void domesticReactor() {
            MemoizingObserver<Ack> observer = memoizingObserver();
            UserId johnDoe = userId();
            DocumentImported importEvent = DocumentImported
                    .newBuilder()
                    .setId(DocumentId.generate())
                    .setText("Annual report")
                    .build();
            context.postEvents()
                   .producedBy(johnDoe)
                   .broadcast()
                   .with(observer)
                   .post(importEvent);
            Status status = observer.firstResponse()
                                    .getStatus();
            assertWithMessage(status.toString())
                    .that(status.getStatusCase())
                    .isEqualTo(ERROR);
            assertThat(status.getError().getType())
                    .isEqualTo(UnsupportedEventException.class.getName());
            Optional<DocumentAggregate> foundDoc = documentRepository.find(importEvent.getId());
            assertThat(foundDoc).isEmpty();
        }

        @Test
        @DisplayName("and deliver to external subscribers")
        void externalSubscriber() {
            UserId johnDoe = userId();
            TestActorRequestFactory requests =
                    new TestActorRequestFactory(johnDoe);
            DocumentId documentId = DocumentId.generate();
            CreateDocument crete = CreateDocument
                    .newBuilder()
                    .setId(documentId)
                    .vBuild();
            EditText edit = EditText
                    .newBuilder()
                    .setId(documentId)
                    .setPosition(0)
                    .setNewText("Fresh new document")
                    .vBuild();
            context.commandBus()
                   .post(ImmutableList.of(requests.createCommand(crete),
                                          requests.createCommand(edit)),
                         noOpObserver());
            EditHistoryProjection historyAfterEdit = editHistoryRepository
                    .find(documentId)
                    .orElseGet(Assertions::fail);
            assertThat(historyAfterEdit.state().getEditList()).isNotEmpty();
            context.postEvents()
                   .producedIn("3d party directory service")
                   .broadcast()
                   .post(UserDeleted
                                 .newBuilder()
                                 .setUser(johnDoe)
                                 .vBuild());
            EditHistoryProjection historyAfterDeleted = editHistoryRepository
                    .find(documentId)
                    .orElseGet(Assertions::fail);
            assertThat(historyAfterDeleted.state().getEditList()).isEmpty();
        }

        @Test
        @DisplayName("and ignore domestic subscribers")
        void domesticSubscriber() {
            DocumentId documentId = DocumentId.generate();
            TextEdited event = TextEdited
                    .newBuilder()
                    .setId(documentId)
                    .vBuild();
            MemoizingObserver<Ack> observer = memoizingObserver();
            context.postEvents()
                   .producedIn("Abusing client")
                   .broadcast()
                   .with(observer)
                   .post(event);
            Status status = observer.firstResponse()
                                    .getStatus();
            assertThat(status.getStatusCase()).isEqualTo(ERROR);
            assertThat(status.getError().getCode())
                    .isEqualTo(UNSUPPORTED_EXTERNAL_MESSAGE.getNumber());
        }

        @Test
        @DisplayName("in a multitenant environment")
        void multitenant() {
            DocumentRepository documentRepository = new DocumentRepository();
            BoundedContext context = BoundedContextBuilder
                    .assumingTests(true)
                    .add(documentRepository)
                    .build();
            UserId johnDoe = userId();
            TenantId acmeCorp = TenantId
                    .newBuilder()
                    .setDomain(InternetDomain.newBuilder().setValue("acme.com"))
                    .build();
            TenantId cyberdyne = TenantId
                    .newBuilder()
                    .setDomain(InternetDomain.newBuilder().setValue("cyberdyne.com"))
                    .build();
            OpenOfficeDocumentUploaded importEvent = OpenOfficeDocumentUploaded
                    .newBuilder()
                    .setId(DocumentId.generate())
                    .setText("Daily report")
                    .build();
            context.postEvents()
                   .producedBy(johnDoe)
                   .broadcast()
                   .forTenant(acmeCorp)
                   .post(importEvent);

            Optional<DocumentAggregate> acmeDailyReport = TenantAwareRunner
                    .with(acmeCorp)
                    .evaluate(() -> documentRepository.find(importEvent.getId()));
            assertThat(acmeDailyReport).isPresent();

            Optional<DocumentAggregate> cyberdyneDailyReport = TenantAwareRunner
                    .with(cyberdyne)
                    .evaluate(() -> documentRepository.find(importEvent.getId()));
            assertThat(cyberdyneDailyReport).isEmpty();
        }
    }

    private static UserId userId() {
        return UserId
                .newBuilder()
                .setValue(newUuid())
                .build();
    }
}
