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

package io.spine.server.entity;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.Truth8;
import io.spine.base.entity.EntityState;
import io.spine.option.EntityOption;
import io.spine.test.entity.AccountDetails;
import io.spine.test.entity.LastSeen;
import io.spine.test.entity.Password;
import io.spine.test.entity.UserActivity;
import io.spine.test.entity.UserFeed;
import io.spine.test.entity.UserSignIn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.option.EntityOption.Visibility.FULL;
import static io.spine.option.EntityOption.Visibility.NONE;
import static io.spine.option.EntityOption.Visibility.QUERY;
import static io.spine.option.EntityOption.Visibility.SUBSCRIBE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EntityVisibility should")
class EntityVisibilityTest {

    @Test
    @DisplayName("not accept `null`s on construction")
    void notAcceptNullsOnConstruction() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EntityVisibility.class);
    }

    @Test
    @DisplayName("not accept null `Visibility` values")
    void notAcceptNulls() {
        Optional<EntityVisibility> value = EntityVisibility.of(Password.class);
        Truth8.assertThat(value).isPresent();
        EntityVisibility instance = value.get();
        new NullPointerTester()
                .testAllPublicInstanceMethods(instance);
    }

    @Test
    @DisplayName("report NONE level for `Aggregate`s by default")
    void aggregateDefaults() {
        EntityVisibility actual = assertVisibility(Password.class, NONE);
        assertFalse(actual.isNotNone());
    }

    @Test
    @DisplayName("report NONE level for `Process Manager`s by default")
    void pmDefaults() {
        EntityVisibility actual = assertVisibility(UserSignIn.class, NONE);
        assertFalse(actual.isNotNone());
    }

    @Test
    @DisplayName("report FULL level for `Projection`s by default")
    void projectionDefaults() {
        EntityVisibility actual = assertVisibility(UserFeed.class, FULL);
        assertTrue(actual.isNotNone());
    }

    //TODO:2020-06-16:alex.tymchenko: seems as non-compilable piece.
//    @Test
//    @DisplayName("report NONE level for `Event.class`")
//    void eventDefault() {
//        EntityVisibility visibility = visibilityOf(Event.class);
//        assertTrue(visibility.is(NONE));
//        assertFalse(visibility.isNotNone());
//    }

    @Test
    @DisplayName("report QUERY level")
    void findQuery() {
        EntityVisibility visibility = visibilityOf(AccountDetails.class);
        assertTrue(visibility.is(QUERY));
        assertTrue(visibility.canQuery());
        assertTrue(visibility.isAsLeast(QUERY));
        assertTrue(visibility.isAsLeast(NONE));
        assertFalse(visibility.isAsLeast(FULL));
        assertFalse(visibility.canSubscribe());
    }

    @Test
    @DisplayName("report SUBSCRIBE level")
    void findSubscribe() {
        EntityVisibility visibility = visibilityOf(UserActivity.class);
        assertTrue(visibility.is(SUBSCRIBE));
        assertTrue(visibility.canSubscribe());
        assertTrue(visibility.isAsLeast(SUBSCRIBE));
        assertTrue(visibility.isAsLeast(NONE));
        assertFalse(visibility.isAsLeast(FULL));
        assertFalse(visibility.canQuery());
    }

    @Test
    @DisplayName("report FULL level")
    void findFull() {
        EntityVisibility visibility = visibilityOf(LastSeen.class);
        assertTrue(visibility.is(FULL));
        assertTrue(visibility.canQuery());
        assertTrue(visibility.canSubscribe());
        assertTrue(visibility.isAsLeast(FULL));
        assertTrue(visibility.isAsLeast(QUERY));
        assertTrue(visibility.isAsLeast(SUBSCRIBE));
        assertTrue(visibility.isAsLeast(NONE));
    }

    private static EntityVisibility
    assertVisibility(Class<? extends EntityState<?>> stateClass, EntityOption.Visibility expected) {
        EntityVisibility actual = visibilityOf(stateClass);
        assertTrue(actual.is(expected));
        return actual;
    }

    private static EntityVisibility visibilityOf(Class<? extends EntityState<?>> stateClass) {
        Optional<EntityVisibility> visibility = EntityVisibility.of(stateClass);
        Truth8.assertThat(visibility)
              .isPresent();
        return visibility.get();
    }
}
