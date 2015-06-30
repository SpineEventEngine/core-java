/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.sample.order;

import com.google.common.eventbus.Subscribe;
import org.spine3.AbstractRepository;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.sample.order.command.CreateOrder;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @author Mikhail Melnik
 */
public class OrderRootRepository extends AbstractRepository<OrderId, OrderRoot, CreateOrder> {

    @Subscribe
    @Override
    public List<EventRecord> handleCreate(CreateOrder command, CommandContext context) throws InvocationTargetException {
        return super.handleCreate(command, context);
    }

}
