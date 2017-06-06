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

package io.spine.server.stand;

import com.google.common.base.Optional;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;
import io.spine.type.TypeUrl;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public class AggregateStateIdShould {

    @Test
    public void not_accept_nulls_on_construction() {
        new NullPointerTester()
                .setDefault(TypeUrl.class, TypeUrl.of(Any.class))
                .testStaticMethods(AggregateStateId.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void have_stringifier() throws ClassNotFoundException {
        // Ensure class loaded
        Class.forName(AggregateStateId.class.getCanonicalName());

        final Optional<Stringifier<AggregateStateId>> stringifierOptional =
                StringifierRegistry.getInstance()
                                   .get(AggregateStateId.class);
        assertTrue(stringifierOptional.isPresent());
    }
}
