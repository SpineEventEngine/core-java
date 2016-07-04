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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.spine3.examples.enrichment.events.UserAccountCreated;
import org.spine3.people.PersonName;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.event.Subscribe;
import org.spine3.users.UserId;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * This class listens to {@link UserAccountCreated} and associates person names with their {@code UserId}s
 * for further resolving.
 */
/* package */ class UserNameLookup extends EventSubscriber implements Function<UserId, PersonName> {

    private final Map<UserId, PersonName> names = Maps.newHashMap();

    @Subscribe
    public void on(UserAccountCreated event) {
        names.put(event.getUserId(), event.getName());
    }

    /**
     * Finds a person name by the passed {@code UserId}.
     */
    @Nullable
    @Override
    public PersonName apply(@Nullable UserId userId) {
        if (userId == null) {
            return null;
        }
        final PersonName result = names.get(userId);
        return result;
    }
}
