/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.util;


import org.spine3.base.UserId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@link UserId} utility class.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
public class UserIds {

    /**
     * Creates a new user ID instance by passed string value.
     *
     * @param value new user ID value
     * @return new instance
     */
    public static UserId create(String value) {
        checkNotNull(value);

        return UserId.newBuilder()
                .setValue(value)
                .build();
    }

    private UserIds() {
    }
}
