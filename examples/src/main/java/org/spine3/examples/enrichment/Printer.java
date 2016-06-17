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

import com.google.common.base.Optional;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.examples.enrichment.events.UserAccountCreated;
import org.spine3.examples.enrichment.events.UserAccountSuspended;
import org.spine3.people.PersonName;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.event.Subscribe;

/**
 * This class prints events into {@link System#out}.
 */
public class Printer extends EventSubscriber {

    @Subscribe
    public void on(UserAccountCreated event) {
        final PersonName personName = event.getName();
        final String text = String.format("User account created: %s. Name: %s %s",
                                          event.getUserId()
                                               .getValue(),
                                          personName.getGivenName(),
                                          personName.getFamilyName());
        print(text);
    }

    @Subscribe
    public void on(UserAccountSuspended event, EventContext context) {
        Optional<UserAccountSuspended.Enrichment> enrichment = Events
                .getEnrichment(UserAccountSuspended.Enrichment.class, context);
        print("Account suspended: " + event.getUserId().getValue());
        if (enrichment.isPresent()) {
            final PersonName personName = enrichment.get()
                                                    .getName();
            final String text = String.format("Name: %s %s",
                                              personName.getGivenName(),
                                              personName.getFamilyName());
            print(text);
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr") // OK for this example.
    private static void print(String text) {
        System.out.println(text);
    }
}
