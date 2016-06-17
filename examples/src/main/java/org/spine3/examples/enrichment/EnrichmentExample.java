/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.examples.enrichment;

import com.google.protobuf.StringValue;
import org.spine3.client.UserUtil;
import org.spine3.examples.enrichment.events.UserAccountCreated;
import org.spine3.examples.enrichment.events.UserAccountSuspended;
import org.spine3.people.PersonName;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.enrich.EventEnricher;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.users.UserId;

import static org.spine3.base.Events.createImportEvent;
import static org.spine3.protobuf.Values.newStringValue;

/**
 * This example demonstrates how to create an {@code EventBus} with {@link EventEnricher} instance.
 */
@SuppressWarnings("CallToPrintStackTrace") // Is OK for this example.
public class EnrichmentExample implements AutoCloseable {

    private final StorageFactory storageFactory;
    private final BoundedContext boundedContext;

    public EnrichmentExample(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
        final UserNameLookup userNameLookup = new UserNameLookup();
        final EventEnricher enricher = EventEnricher
                .newBuilder()
                .addEventEnrichment(UserAccountSuspended.class, UserAccountSuspended.Enrichment.class)
                .addFieldEnrichment(UserId.class, PersonName.class, userNameLookup)
                .build();

        this.boundedContext = BoundedContext.newBuilder()
                                            .setStorageFactory(storageFactory)
                                            .setEventEnricher(enricher)
                                            .build();
        final EventBus eventBus = boundedContext.getEventBus();
        eventBus.subscribe(userNameLookup);
        eventBus.subscribe(new Printer());
    }

    @Override
    public void close() throws Exception {
        boundedContext.close();
        storageFactory.close();
    }

    public static void main(String[] args) {
        try(EnrichmentExample example = new EnrichmentExample(InMemoryStorageFactory.getInstance())) {
            // The ID of the entity, which produces events.
            final StringValue producerId = newStringValue(EnrichmentExample.class.getSimpleName());
            final UserId userId = UserUtil.newUserId("jdoe");
            final PersonName name = PersonName
                    .newBuilder()
                    .setGivenName("John")
                    .setFamilyName("Doe")
                    .build();

            // Post an event on user account creation.
            final UserAccountCreated accountCreated = UserAccountCreated
                    .newBuilder()
                    .setUserId(userId)
                    .setName(name)
                    .build();

            final EventBus eventBus = example.boundedContext.getEventBus();
            eventBus.post(createImportEvent(accountCreated, producerId));

            final UserAccountSuspended accountSuspended = UserAccountSuspended
                    .newBuilder()
                    .setUserId(userId)
                    .build();
            eventBus.post(createImportEvent(accountSuspended, producerId));

            //noinspection UseOfSystemOutOrSystemErr
            System.out.println("The End");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
