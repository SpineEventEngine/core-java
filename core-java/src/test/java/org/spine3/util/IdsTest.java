/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.util;

import org.junit.Test;

/**
 * @author Mikhail Melnik
 */
public class IdsTest {

    @Test(expected = NullPointerException.class)
    public void toStringRepresentationFailsOnNull() {
        Messages.toText(null);
    }

    @Test(expected = NullPointerException.class)
    public void toJsonFailsOnNull() {
        Messages.toJson(null);
    }

}
