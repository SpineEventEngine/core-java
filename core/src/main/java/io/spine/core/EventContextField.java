package io.spine.core;

import io.spine.base.Field;
import io.spine.base.SubscribableField;

/**
 * A subscribable field of an event context.
 *
 * <p>When such field is specified to a subscription filter on creation, the {@code "context."}
 * prefix will be automatically appended to the field path, allowing to distinguish between event
 * context and event message filters when sending the request to a server side.
 */
public class EventContextField extends SubscribableField {

    public EventContextField(Field field) {
        super(field);
    }
}
