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

package io.spine.option;

import com.google.common.testing.NullPointerTester;
import io.spine.option.EntityOption.Visibility;
import io.spine.test.Tests;
import io.spine.test.options.FullAccessAggregate;
import io.spine.test.options.SubscribableAggregate;
import org.junit.Test;

import static io.spine.option.EntityOptions.getVisibility;
import static org.junit.Assert.assertEquals;

/**
 * See `spine/test/option/entity_options_should.proto` for definitions of types used in the tests.
 *
 * @author Alexander Yevsyukov
 */
public class EntityOptionsShould {

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(EntityOptions.class);
    }

    @Test
    public void assume_full_visibility_when_not_defined() {
        assertEquals(Visibility.FULL, getVisibility(FullAccessAggregate.class));
    }

    @Test
    public void get_defined_visibility_value() {
        assertEquals(Visibility.SUBSCRIBE, getVisibility(SubscribableAggregate.class));
    }

    @Test
    public void pass_null_tolerance_test() {
        new NullPointerTester()
                .setDefault(Visibility.class, Visibility.NONE)
                .testAllPublicStaticMethods(EntityOptions.class);
    }
}
