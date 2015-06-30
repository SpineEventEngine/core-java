/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.util;

import org.junit.Test;
import org.spine3.base.UserId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Mikhail Melnik
 */
public class UserIdsTest {

    private static String testIdString = "12345";

    @Test
    public void create() {
        UserId userId = UserIds.create(testIdString);

        assertThat(userId, equalTo(UserId.newBuilder().setValue(testIdString).build()));
    }

    @Test(expected = NullPointerException.class)
    public void createFailsOnNull() {
        UserIds.create(null);
    }

}
