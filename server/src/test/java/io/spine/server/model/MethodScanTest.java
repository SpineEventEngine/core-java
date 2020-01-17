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

package io.spine.server.model;

import com.google.common.collect.ImmutableSetMultimap;
import io.spine.server.event.model.SubscriberMethod;
import io.spine.server.event.model.SubscriberSignature;
import io.spine.server.model.given.map.ARejectionSubscriber;
import io.spine.server.model.given.map.FilteredSubscription;
import io.spine.string.StringifierRegistry;
import io.spine.string.Stringifiers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.model.MethodScan.findMethodsBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("`MethodScan` should")
class MethodScanTest {

    /**
     * Registers the stringifier for {@code Integer}, which is used for parsing filter field values.
     */
    @BeforeAll
    static void prepare() {
        StringifierRegistry.instance()
                           .register(Stringifiers.forInteger(), Integer.TYPE);
    }

    @Test
    @DisplayName("provide a map with filter-less keys")
    void noFiltersInKeys() {
        ImmutableSetMultimap<DispatchKey, SubscriberMethod> map =
                findMethodsBy(FilteredSubscription.class, new SubscriberSignature());
        map.keySet()
           .forEach(key -> assertEquals(key.withoutFilter(), key));
    }

    @Test
    @DisplayName("allow multiple subscriptions to the same rejection with different causes")
    void multipleRejectionSubscriptions() {
        ImmutableSetMultimap<DispatchKey, SubscriberMethod> map =
                findMethodsBy(ARejectionSubscriber.class, new SubscriberSignature());
        assertThat(map.keys()).hasSize(2);
    }
}
