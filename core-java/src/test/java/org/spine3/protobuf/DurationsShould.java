/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.protobuf;


import org.spine3.util.Tests;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.spine3.protobuf.Durations.*;


/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class DurationsShould {

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Test public void
    have_private_constructor() throws Exception {
        Tests.callPrivateUtilityConstructor(Durations.class);
    }

    @Test public void
    have_ZERO_constant() {
        assertEquals(0, toNanos(ZERO));
    }

    @Test public void
    have_wrapper_for_nanos() {
        assertEquals(10, toNanos(nanos(10)));
    }

    @Test public void
    have_wrappers_for_seconds() {
        assertEquals(1, toSeconds(seconds(1)));
    }

    @Test public void
    have_wrapper_for_minutes() {
        assertEquals(1, toMinutes(minutes(1)));
    }

    @Test public void
    have_wrapper_for_hours() {
        assertEquals(1, getHours(ofHours(1)));
    }

    @Test public void
    have_conversion_to_long_minutes() {
        assertEquals(5, toMinutes(ofMinutes(5)));
    }
}
